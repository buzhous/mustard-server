package org.jeecg.modules.app.service;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.app.bean.enums.SyncStatusEnum;
import org.jeecg.modules.app.entity.ItemInfo;
import org.jeecg.modules.app.entity.ItemSearchIndex;
import org.jeecg.modules.app.entity.ItemSync;
import org.jeecg.modules.app.entity.KeywordAlias;
import org.jeecg.modules.app.mapper.ItemInfoMapper;
import org.jeecg.modules.app.mapper.ItemSearchIndexMapper;
import org.jeecg.modules.app.mapper.ItemSyncMapper;
import org.jeecg.modules.app.mapper.KeywordAliasReadMapper;
import org.jeecg.modules.app.mip.MipAttributesCodec;
import org.jeecg.modules.app.mip.MipWriteSupport;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 物品检索索引服务：ngram 全文检索快照的构建与维护（可重建派生数据，非权威）。
 * <p>
 * - rebuild(itemId) 幂等：物品行缺失或同步记录无有效行（无行/DESTROYED）→ 删索引行；否则 UPSERT 快照；
 * - 调用方在业务事务内调用；本服务内部异常只记录不抛出，索引失败不阻断业务主链路（可由 admin 全量重建修复）；
 * - restricted 组件不进索引：以组件 sensitivity=restricted 为准（MipWriteSupport.enrich 写入），
 *   组件缺 sensitivity 键时按 element 17/18/21/22/25/26/33 兜底清单处理；
 * - 别名词表读取走 Redis 短 TTL 缓存（ail:alias:cache，60s，模式同 tuning_config 短 TTL）。
 */
@Slf4j
@Service
public class ItemSearchIndexService {

    /** 别名词表缓存键（P4 维护页保存时删键） */
    public static final String ALIAS_CACHE_KEY = "ail:alias:cache";
    private static final long ALIAS_CACHE_TTL_SECONDS = 60;

    /** restricted 兜底清单：组件缺失 sensitivity 键时按绝对受限处理（MipComponentRegistry.LOCKED_RESTRICTED 同步） */
    private static final Set<Integer> RESTRICTED_FALLBACK = Set.of(17, 18, 21, 22, 25, 26, 33);

    /** 不进 attrText 的组件：名称(1)/图标(2)/标签组(10) 单列承载，图标 URL 非检索文本 */
    private static final Set<Integer> ATTR_TEXT_SKIP = Set.of(1, 2, 10);

    private final ItemSearchIndexMapper searchIndexMapper;
    private final ItemInfoMapper itemInfoMapper;
    private final ItemSyncMapper itemSyncMapper;
    private final KeywordAliasReadMapper keywordAliasReadMapper;
    private final RedisUtil redisUtil;

    public ItemSearchIndexService(ItemSearchIndexMapper searchIndexMapper,
                                  ItemInfoMapper itemInfoMapper,
                                  ItemSyncMapper itemSyncMapper,
                                  KeywordAliasReadMapper keywordAliasReadMapper,
                                  RedisUtil redisUtil) {
        this.searchIndexMapper = searchIndexMapper;
        this.itemInfoMapper = itemInfoMapper;
        this.itemSyncMapper = itemSyncMapper;
        this.keywordAliasReadMapper = keywordAliasReadMapper;
        this.redisUtil = redisUtil;
    }

    /**
     * 重建单物品索引（幂等，同事务调用；内部吞异常保护业务主链路）
     */
    public void rebuild(Long itemId) {
        if (itemId == null) {
            return;
        }
        try {
            doRebuild(itemId);
        } catch (Exception e) {
            log.warn("物品检索索引重建失败(可由 admin 全量重建修复), itemId={}", itemId, e);
        }
    }

    /** String 形态重载（tbl_item_sync/recycle.item_id 均为字符串） */
    public void rebuild(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return;
        }
        try {
            rebuild(Long.parseLong(itemId.trim()));
        } catch (NumberFormatException e) {
            // 非数字主键（异常数据）跳过索引维护
        }
    }

    /**
     * 全文检索主键圈定：terms（空格分隔，任一命中即可）→ 物品 id 列表（≤200，相关度降序）。
     * 索引查询失败降级返回空列表（调用方回退原检索链）。
     */
    public List<String> searchIdsByTerms(String uid, String terms) {
        if (uid == null || uid.isBlank() || terms == null || terms.isBlank()) {
            return List.of();
        }
        try {
            List<String> ids = searchIndexMapper.selectIdsByTerms(uid, terms);
            return ids == null ? List.of() : ids;
        } catch (Exception e) {
            log.warn("检索索引全文查询失败(降级回退原链), uid={}, terms={}", uid, terms, e);
            return List.of();
        }
    }

    /**
     * 别名扩展：返回 [原词, 标准词...]（去重保序）；词表未命中或缓存异常时返回仅含原词的列表。
     * 检索新链 ① 用（P2-6）。
     */
    @SuppressWarnings("unchecked")
    public List<String> aliasExpand(String term) {
        List<String> out = new ArrayList<>();
        if (term == null || term.isBlank()) {
            return out;
        }
        String t = term.trim();
        out.add(t);
        try {
            Map<String, List<String>> aliasMap = loadAliasMap();
            List<String> standards = aliasMap.get(t);
            if (standards != null) {
                for (String s : standards) {
                    if (!out.contains(s)) {
                        out.add(s);
                    }
                }
            }
        } catch (Exception e) {
            // 词表读取失败降级：仅原词
            log.warn("别名词表读取失败, term={}", t, e);
        }
        return out;
    }

    // ================================ 私有方法 ================================

    private void doRebuild(Long itemId) {
        ItemInfo item = itemInfoMapper.selectById(itemId);
        if (item == null) {
            deleteIndexRow(String.valueOf(itemId));
            return;
        }
        // 同步记录无有效行（无行 / 当前归属用户行 DESTROYED）→ 物品不在库，删索引行
        ItemSync sync = itemSyncMapper.selectOne(new LambdaQueryWrapper<ItemSync>()
                .eq(ItemSync::getItemId, String.valueOf(itemId))
                .eq(ItemSync::getUserId, item.getUserId())
                .orderByDesc(ItemSync::getUpdateTime)
                .last("limit 1"));
        if (sync == null || (sync.getStatus() != null
                && SyncStatusEnum.DESTROYED.getCode() == sync.getStatus())) {
            deleteIndexRow(String.valueOf(itemId));
            return;
        }
        String itemIdStr = String.valueOf(itemId);
        // 快照构建
        MipWriteSupport.IdentitySync identity = MipWriteSupport.computeIdentitySync(item.getFields());
        String normName = normalize(identity.name() != null ? identity.name() : item.getName());
        String categoryNorm = item.getCategory() == null ? "" : item.getCategory().trim();
        List<String> textParts = new ArrayList<>();
        List<String> tagParts = collectTags(item.getFields());
        for (String tag : tagParts) {
            textParts.add(tag);
        }
        // 非 restricted 组件值拼接
        com.alibaba.fastjson.JSONArray fields = parseFields(item.getFields());
        if (fields != null) {
            for (int i = 0; i < fields.size(); i++) {
                com.alibaba.fastjson.JSONObject f = fields.getJSONObject(i);
                Integer element = f == null ? null : MipAttributesCodec.elementOf(f);
                if (element == null) {
                    continue;
                }
                if (isRestricted(f, element)) {
                    continue;
                }
                if (ATTR_TEXT_SKIP.contains(element)) {
                    continue;
                }
                String text = valueAsText(element, f.getJSONObject("content"));
                if (!text.isBlank()) {
                    textParts.add(text);
                }
            }
        }
        String attrText = String.join(" ", textParts);
        // 别名折叠：名称/标签命中词表时把标准词并入全文文本
        Set<String> blobParts = new LinkedHashSet<>();
        if (!normName.isBlank()) {
            blobParts.add(normName);
        }
        blobParts.addAll(tagParts);
        blobParts.add(attrText);
        List<String> extraAliasStandards = new ArrayList<>();
        Map<String, List<String>> aliasMap = loadAliasMap();
        for (String token : List.copyOf(tagParts)) {
            appendStandards(aliasMap, token, extraAliasStandards);
        }
        appendStandards(aliasMap, normName, extraAliasStandards);
        blobParts.addAll(extraAliasStandards);
        String searchBlob = String.join(" ", blobParts.stream().filter(s -> s != null && !s.isBlank()).toList());

        ItemSearchIndex idx = new ItemSearchIndex()
                .setItemId(itemIdStr)
                .setUid(item.getUserId())
                .setNormName(normName)
                .setCategoryNorm(categoryNorm)
                .setTagNorms(String.join(" ", tagParts))
                .setAttrText(attrText)
                .setSearchBlob(searchBlob)
                .setUpdatedTime(new Date());
        // 幂等 UPSERT（主键 item_id）
        ItemSearchIndex exist = searchIndexMapper.selectById(itemIdStr);
        if (exist == null) {
            searchIndexMapper.insert(idx);
        } else {
            searchIndexMapper.updateById(idx);
        }
    }

    private void deleteIndexRow(String itemId) {
        searchIndexMapper.deleteById(itemId);
    }

    /** restricted 判定：组件 sensitivity=restricted 为准；缺键时按兜底清单 */
    private boolean isRestricted(com.alibaba.fastjson.JSONObject f, int element) {
        String sensitivity = f.getString("sensitivity");
        if (sensitivity != null) {
            return "restricted".equals(sensitivity);
        }
        return RESTRICTED_FALLBACK.contains(element);
    }

    private com.alibaba.fastjson.JSONArray parseFields(String fieldsJson) {
        if (fieldsJson == null || fieldsJson.isBlank()) {
            return null;
        }
        try {
            return com.alibaba.fastjson.JSONArray.parseArray(fieldsJson);
        } catch (Exception e) {
            return null;
        }
    }

    /** element 10 标签组 → 归一化标签列表 */
    private List<String> collectTags(String fieldsJson) {
        List<String> tags = new ArrayList<>();
        com.alibaba.fastjson.JSONArray fields = parseFields(fieldsJson);
        if (fields == null) {
            return tags;
        }
        for (int i = 0; i < fields.size(); i++) {
            com.alibaba.fastjson.JSONObject f = fields.getJSONObject(i);
            Integer element = f == null ? null : MipAttributesCodec.elementOf(f);
            if (element == null || element != 10) {
                continue;
            }
            Object value = MipAttributesCodec.contentValue(10, f.getJSONObject("content"));
            if (value instanceof com.alibaba.fastjson.JSONArray arr) {
                for (int j = 0; j < arr.size(); j++) {
                    Object tag = arr.get(j);
                    // tags 元素支持 {name:..} 对象与纯字符串两种形态，取 name 键避免整串 JSON 进索引
                    String t = normalize(tag instanceof com.alibaba.fastjson.JSONObject o
                            ? o.getString("name") : String.valueOf(tag));
                    if (!t.isBlank()) {
                        tags.add(t);
                    }
                }
            } else if (value != null) {
                String t = normalize(String.valueOf(value));
                if (!t.isBlank()) {
                    tags.add(t);
                }
            }
        }
        return tags;
    }

    /** 组件值 → 检索文本（数组/对象递归拼接，媒体 URL 除外） */
    private String valueAsText(int element, com.alibaba.fastjson.JSONObject content) {
        Object value = MipAttributesCodec.contentValue(element, content);
        if (value == null) {
            return "";
        }
        if (value instanceof com.alibaba.fastjson.JSONArray arr) {
            List<String> parts = new ArrayList<>();
            for (int i = 0; i < arr.size(); i++) {
                String t = nodeText(arr.get(i));
                if (!t.isBlank()) {
                    parts.add(t);
                }
            }
            return String.join(" ", parts);
        }
        return nodeText(value);
    }

    private String nodeText(Object node) {
        if (node == null) {
            return "";
        }
        if (node instanceof com.alibaba.fastjson.JSONObject obj) {
            List<String> parts = new ArrayList<>();
            for (Map.Entry<String, Object> en : obj.entrySet()) {
                String t = nodeText(en.getValue());
                if (!t.isBlank()) {
                    parts.add(t);
                }
            }
            return String.join(" ", parts);
        }
        if (node instanceof com.alibaba.fastjson.JSONArray arr) {
            List<String> parts = new ArrayList<>();
            for (int i = 0; i < arr.size(); i++) {
                String t = nodeText(arr.get(i));
                if (!t.isBlank()) {
                    parts.add(t);
                }
            }
            return String.join(" ", parts);
        }
        String s = String.valueOf(node);
        // URL 类值不进检索文本
        if (s.startsWith("http://") || s.startsWith("https://")) {
            return "";
        }
        return normalize(s);
    }

    /** 归一化：trim / 全角→半角 / 拉丁小写 */
    private String normalize(String s) {
        if (s == null || s.isBlank()) {
            return "";
        }
        char[] chars = s.trim().toCharArray();
        StringBuilder sb = new StringBuilder(chars.length);
        for (char c : chars) {
            if (c == '\u3000') {
                sb.append(' ');
            } else if (c >= '\uFF01' && c <= '\uFF5E') {
                sb.append((char) (c - 0xFEE0));
            } else {
                sb.append(c);
            }
        }
        String out = sb.toString().trim();
        return out.toLowerCase();
    }

    private void appendStandards(Map<String, List<String>> aliasMap, String token, List<String> out) {
        if (token == null || token.isBlank()) {
            return;
        }
        List<String> standards = aliasMap.get(token.trim().toLowerCase());
        if (standards != null) {
            for (String s : standards) {
                String ns = normalize(s);
                if (!ns.isBlank() && !out.contains(ns)) {
                    out.add(ns);
                }
            }
        }
    }

    /**
     * 别名词表缓存：Redis 键 ail:alias:cache（TTL 60s），值=启用词行 JSON。
     * 结构 alias(lower) → [standard...]；Redis 异常时回源 DB，DB 异常返回空表。
     */
    @SuppressWarnings("unchecked")
    private Map<String, List<String>> loadAliasMap() {
        Object cached = redisUtil.get(ALIAS_CACHE_KEY);
        if (cached != null) {
            return (Map<String, List<String>>) JSON.parseObject(String.valueOf(cached), Map.class);
        }
        List<KeywordAlias> rows = keywordAliasReadMapper.selectEnabled();
        Map<String, List<String>> map = new java.util.LinkedHashMap<>();
        if (rows != null) {
            for (KeywordAlias row : rows) {
                if (ObjectUtil.isEmpty(row.getAlias()) || ObjectUtil.isEmpty(row.getStandard())) {
                    continue;
                }
                map.computeIfAbsent(normalize(row.getAlias()), k -> new ArrayList<>())
                        .add(row.getStandard());
            }
        }
        try {
            redisUtil.set(ALIAS_CACHE_KEY, JSON.toJSONString(map), ALIAS_CACHE_TTL_SECONDS);
        } catch (Exception e) {
            // 缓存写失败不影响本次读取
        }
        return map;
    }
}
