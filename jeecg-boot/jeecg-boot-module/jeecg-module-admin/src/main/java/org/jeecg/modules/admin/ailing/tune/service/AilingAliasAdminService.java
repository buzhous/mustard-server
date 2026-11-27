package org.jeecg.modules.admin.ailing.tune.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.admin.ailing.tune.mapper.AilingAliasAdminMapper;
import org.jeecg.modules.admin.ailing.tune.mapper.AilingTuneAdminMapper.PageArgsHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 芥灵调优中心 · 词表维护服务（P4）：tbl_keyword_alias CRUD，全部写操作成功后
 * 删除 Redis 词表缓存键 {@code ail:alias:cache}（App 侧 TTL 60s 内自然过期兜底）。
 */
@Slf4j
@Service
public class AilingAliasAdminService {

    /** 词表读缓存键（app 模块 ItemSearchIndexService.ALIAS_CACHE_KEY 同串） */
    public static final String ALIAS_CACHE_KEY = "ail:alias:cache";

    @Autowired
    private AilingAliasAdminMapper aliasMapper;
    @Autowired
    private RedisUtil redisUtil;

    /** 词表分页：alias/standard 模糊 + 类型/启用筛选 */
    public Map<String, Object> list(long pageNo, long pageSize, String alias, String standard,
                                    Integer aliasType, Integer enabled) {
        PageArgsHolder page = new PageArgsHolder(pageNo, pageSize);
        List<Map<String, Object>> records = aliasMapper.selectAliasPage(page, alias, standard, aliasType, enabled);
        Long total = aliasMapper.countAlias(alias, standard, aliasType, enabled);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("records", records);
        out.put("total", total == null ? 0 : total);
        return out;
    }

    /** 新增/更新：uk_alias(alias,standard) 冲突友好报错；更新时 alias 不可改 */
    public Map<String, Object> save(String id, String alias, String standard, Integer aliasType,
                                    Integer enabled, String operator) {
        if (StrUtil.isBlank(alias) || StrUtil.isBlank(standard)) {
            throw new IllegalArgumentException("别名与标准词均不能为空");
        }
        if (alias.trim().length() > 64 || standard.trim().length() > 64) {
            throw new IllegalArgumentException("别名/标准词长度上限 64");
        }
        int type = aliasType == null ? 1 : aliasType;
        if (type != 1 && type != 2) {
            throw new IllegalArgumentException("aliasType 仅支持 1(同义词)/2(上下位)");
        }
        int on = enabled == null ? 1 : enabled;
        if (on != 0 && on != 1) {
            throw new IllegalArgumentException("enabled 仅支持 0/1");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        if (StrUtil.isBlank(id)) {
            Long conflict = aliasMapper.countAliasConflict(alias.trim(), standard.trim(), null);
            if (conflict != null && conflict > 0) {
                throw new IllegalArgumentException("词表已存在该别名+标准词组合");
            }
            long newId = IdUtil.getSnowflakeNextId();
            aliasMapper.insertAlias(newId, alias.trim(), standard.trim(), type, on, operator);
            out.put("id", String.valueOf(newId));
            out.put("created", true);
        } else {
            Map<String, Object> row = aliasMapper.selectAliasById(id);
            if (row == null) {
                throw new IllegalArgumentException("词表条目不存在");
            }
            Long conflict = aliasMapper.countAliasConflict(String.valueOf(row.get("alias")), standard.trim(), id);
            if (conflict != null && conflict > 0) {
                throw new IllegalArgumentException("词表已存在该别名+标准词组合");
            }
            aliasMapper.updateAlias(id, standard.trim(), type, on);
            out.put("id", id);
            out.put("created", false);
        }
        redisUtil.del(ALIAS_CACHE_KEY);
        out.put("cacheCleared", ALIAS_CACHE_KEY);
        return out;
    }

    /** 物理删除（表无 del_flag；二次确认由前端承担） */
    public Map<String, Object> delete(String id) {
        Map<String, Object> row = aliasMapper.selectAliasById(id);
        if (row == null) {
            throw new IllegalArgumentException("词表条目不存在");
        }
        aliasMapper.deleteAlias(id);
        redisUtil.del(ALIAS_CACHE_KEY);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", id);
        out.put("deleted", true);
        out.put("cacheCleared", ALIAS_CACHE_KEY);
        return out;
    }
}
