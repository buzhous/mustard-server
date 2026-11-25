package org.jeecg.modules.ai.tooling;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.ai.bean.dto.AiItemQuery;
import org.jeecg.modules.ai.bean.dto.AiItemRow;
import org.jeecg.modules.ai.catalog.AiComponentCatalog;
import org.jeecg.modules.ai.port.ItemReadPort;
import org.jeecg.modules.ai.port.ItemWritePort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * R1/R3 受控物品工具：草稿 → 写内核 attributes 组装 + 单条记录承载件数创建 + 读包装。
 * <p>
 * normal confirmCreate 与 express 自动创建共用同一创建链路（凭据解析/幂等键在端口实现内）；
 * 白名单校验（restricted/未知 element 丢弃）在组装层强制执行，MIP 出域过滤为第三重防线。
 */
@Slf4j
@Component
public class ItemTools {

    /** 标签上限（MIP 校验 E-TAGS：≤3） */
    private static final int MAX_TAGS = 3;
    /** 属性组上限（element 11 属性组） */
    private static final int MAX_ATTRIBUTES = 10;
    /** attributes 总条目上限（防模型输出膨胀） */
    private static final int MAX_ATTR_ITEMS = 40;
    /** 数量上限 */
    private static final int MAX_QTY = 99;
    /** iconpark 官方图标名形态（小写字母/数字开头，含连字符/下划线，2–48 位），如 camera / electron_dev */
    private static final Pattern ICON_KEY_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9_-]{1,47}$");
    /** AI 专属凭据标记（开放接口 source） */
    private static final String AI_SOURCE = "ai-assistant";

    private final ItemWritePort itemWritePort;
    private final ItemReadPort itemReadPort;

    public ItemTools(ItemWritePort itemWritePort, ItemReadPort itemReadPort) {
        this.itemWritePort = itemWritePort;
        this.itemReadPort = itemReadPort;
    }

    // ================================ 写路径 ================================

    /**
     * 草稿 → 开放接口 attributes 组装：
     * 顶层槽位 1名称/2图标/3类别/8描述/10标签组/11属性组 + fields[] 任意组件直传。
     * restricted / 未知 element / 超限条目一律丢弃（编排器已过滤，此处兜底）。
     */
    public String buildAttributes(JSONObject draft) {
        JSONArray attributes = new JSONArray();
        attributes.add(attr(1, str(draft, "name")));
        JSONObject icon = draft.getJSONObject("icon");
        String iconValue = icon == null ? null : StrUtil.trimToNull(icon.getString("value"));
        // URL → 封面图；合法 iconpark 图标名（mock 模拟建档提名）→ 原样落库，App 端按图标名渲染、
        // 识别失败自动回退本地占位；其余非法值不填充占位域名
        if (iconValue != null && (iconValue.startsWith("http://") || iconValue.startsWith("https://"))) {
            attributes.add(attr(2, iconValue));
        } else if (iconValue != null && ICON_KEY_PATTERN.matcher(iconValue).matches()) {
            attributes.add(attr(2, iconValue));
        }
        attributes.add(attr(3, str(draft, "category")));
        String desc = str(draft, "description");
        if (StrUtil.isNotBlank(desc)) {
            attributes.add(attr(8, desc));
        }
        JSONArray tags = draft.getJSONArray("tags");
        if (tags != null && !tags.isEmpty()) {
            JSONArray tagValues = new JSONArray();
            for (int i = 0; i < Math.min(tags.size(), MAX_TAGS); i++) {
                JSONObject t = tags.getJSONObject(i);
                if (t != null && StrUtil.isNotBlank(t.getString("value"))) {
                    tagValues.add(t.getString("value").trim());
                }
            }
            if (!tagValues.isEmpty()) {
                attributes.add(attr(10, tagValues));
            }
        }
        // 属性组（element 11）：系统属性模型为「属性名+颜色」（ElementAttributeVO 无 value 字段），
        // 故把值并入名称落库（如「攻击力：120」），详情属性卡直接展示完整信息
        JSONArray attrs = draft.getJSONArray("attributes");
        if (attrs != null && !attrs.isEmpty()) {
            JSONArray pairs = new JSONArray();
            for (int i = 0; i < Math.min(attrs.size(), MAX_ATTRIBUTES); i++) {
                JSONObject p = attrs.getJSONObject(i);
                if (p == null || StrUtil.isBlank(p.getString("name"))) {
                    continue;
                }
                String name = p.getString("name").trim();
                String value = p.get("value") == null ? "" : String.valueOf(p.get("value")).trim();
                JSONObject pair = new JSONObject(true);
                pair.put("name", value.isEmpty() ? name : name + "：" + value);
                pairs.add(pair);
            }
            if (!pairs.isEmpty()) {
                attributes.add(attr(11, pairs));
            }
        }
        // fields[]（v2 通用组件）：白名单校验后 element+value 直传写内核
        JSONArray fields = draft.getJSONArray("fields");
        if (fields != null && !fields.isEmpty()) {
            for (int i = 0; i < fields.size() && attributes.size() < MAX_ATTR_ITEMS; i++) {
                JSONObject f = fields.getJSONObject(i);
                if (f == null) {
                    continue;
                }
                Integer element = f.getInteger("element");
                if (element == null || !AiComponentCatalog.isKnown(element)
                        || AiComponentCatalog.isRestricted(element)
                        || AiComponentCatalog.TOP_LEVEL_ELEMENTS.contains(element)) {
                    continue;
                }
                Object value = f.get("value");
                if (value == null || (value instanceof String s && StrUtil.isBlank(s))) {
                    continue;
                }
                attributes.add(attr(element, value));
            }
        }
        return attributes.toJSONString();
    }

    /**
     * 创建 1 条物品记录，件数为 quantity（数量组件 element 7 顶层直传，服务端 addItem 提取为
     * ItemInfo.quantity 与库存数量；范围 1–99）。同规格多件不再拆分为多条记录。
     *
     * @return CreateOutcome（oriIds 成功清单 + firstError 首个失败原因 + quantity 件数）
     */
    public CreateOutcome createItems(String uid, JSONObject draft, int quantity) {
        int n = Math.max(1, Math.min(quantity <= 0 ? 1 : quantity, MAX_QTY));
        JSONArray attributes = JSON.parseArray(buildAttributes(draft));
        // 数量组件 element 7（fields 白名单禁入，仅顶层直传）：单条记录承载件数
        JSONObject qty = new JSONObject(true);
        qty.put("element", 7);
        qty.put("value", n);
        attributes.add(qty);
        int confidence = overallOf(draft);
        List<String> oriIds = new ArrayList<>();
        String firstError = null;
        JSONObject body = new JSONObject(true);
        body.put("attributes", attributes);
        body.put("confidence", confidence);
        body.put("source", AI_SOURCE);
        try {
            var result = itemWritePort.addItem(uid, body);
            if (result.isSuccess() && result.getResult() != null) {
                oriIds.add(oriIdOf(result.getResult()));
            } else {
                firstError = result.getMessage();
            }
        } catch (Exception e) {
            log.error("[ai-tools] add_item 失败 uid={} qty={}", uid, n, e);
            firstError = e.getMessage();
        }
        return new CreateOutcome(oriIds, firstError, 1, n);
    }

    // ================================ 读路径（thin 包装） ================================

    public Page<AiItemRow> searchItems(AiItemQuery query) {
        return itemReadPort.listByCondition(query);
    }

    public JSONObject getItemDetail(String uid, String itemId) {
        return itemReadPort.itemDetail(uid, itemId);
    }

    public AiItemRow getItemByOriId(String uid, String oriId) {
        return itemReadPort.queryByOriId(uid, oriId);
    }

    public JSONObject getStats(String uid) {
        return itemReadPort.stats(uid);
    }

    public JSONObject getLineage(String uid, String itemId) {
        return itemReadPort.lineage(uid, itemId);
    }

    // ================================ 私有 ================================

    /** overall：name/category 置信度与 fields[] 置信度的最小值 */
    private int overallOf(JSONObject draft) {
        JSONObject overall = draft.getJSONObject("overall");
        if (overall != null) {
            Integer c = overall.getInteger("confidence");
            if (c != null) {
                return c;
            }
        }
        int min = 100;
        for (String key : List.of("name", "category")) {
            JSONObject slot = draft.getJSONObject(key);
            min = Math.min(min, slot == null ? 0 : (slot.getInteger("confidence") == null ? 0 : slot.getInteger("confidence")));
        }
        JSONArray fields = draft.getJSONArray("fields");
        if (fields != null) {
            for (int i = 0; i < fields.size(); i++) {
                JSONObject f = fields.getJSONObject(i);
                if (f != null && f.getInteger("confidence") != null) {
                    min = Math.min(min, f.getInteger("confidence"));
                }
            }
        }
        return min;
    }

    private JSONObject attr(int element, Object value) {
        JSONObject a = new JSONObject(true);
        a.put("element", element);
        a.put("value", value);
        return a;
    }

    private String str(JSONObject draft, String key) {
        JSONObject slot = draft.getJSONObject(key);
        if (slot == null) {
            return null;
        }
        // 字符串槽位（name/category/description）：LLM 误输出数字（如 description: 0）
        // 时不落库；fastjson getString 会把数字转成 "0" 字符串，必须先做类型判断
        Object value = slot.get("value");
        if (!(value instanceof String s)) {
            return null;
        }
        return StrUtil.trimToNull(s);
    }

    /**
     * 真实图片建档：icon 槽位直填本轮首图（URL），App 预览卡片按 URL 渲染照片封面；
     * 覆盖 LLM 的图标名提名（有真实照片时照片优先于示意图标）。
     * mock 模拟建档不调用（保持 iconpark 图标名设计）
     */
    public void injectCoverImage(JSONObject draft, List<String> images) {
        if (images == null || images.isEmpty()) {
            return;
        }
        String cover = StrUtil.trimToNull(images.get(0));
        if (cover == null) {
            return;
        }
        JSONObject icon = draft.getJSONObject("icon");
        if (icon == null) {
            icon = new JSONObject(true);
            draft.put("icon", icon);
        }
        icon.put("value", cover);
    }

    private String oriIdOf(JSONObject instance) {
        JSONObject identity = instance.getJSONObject("identity");
        if (identity != null && StrUtil.isNotBlank(identity.getString("ori_id"))) {
            return identity.getString("ori_id");
        }
        return StrUtil.blankToDefault(instance.getString("ori_id"), "unknown");
    }

    /**
     * 创建结果（record）：oriIds 成功 oriId 清单；firstError 首个失败原因；
     * total 尝试创建的记录数（单条=1）；quantity 本次件数（数量 ×N）
     */
    public record CreateOutcome(List<String> oriIds, String firstError, int total, int quantity) {
    }
}
