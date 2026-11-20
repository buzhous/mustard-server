package org.jeecg.modules.app.mip;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * MIP v1 Profile 生成器（MIP 协议 3.3）
 * <p>
 * 由模板 fields 生成 Profile 文档：{schema_id, profile_version, item_form, topic_type, properties}。
 * properties 元素：{element, name, type, required, sensitivity}——sensitivity 由
 * {@link MipAttributesCodec#resolveSensitivity} 单点计算（注册表默认 + 证件 12 提级）。
 * <p>
 * topic_type 推导与 admin 侧 TopicFieldClassifier.classify 同源（模型型&gt;多媒体型&gt;扩展型&gt;基础型）；
 * 模块隔离约束下在 app 模块内同语义实现，修改须双侧同步。
 */
public final class MipProfileBuilder {

    public static final String MIP_VERSION = "1.0";
    public static final String DEFAULT_PROFILE_VERSION = "1";

    /** 形态组件区间：V 系列 23-28，P 系列 29-34 */
    private static final int V_FROM = 23, V_TO = 28, P_FROM = 29, P_TO = 34;

    private MipProfileBuilder() {
    }

    /**
     * 生成 Profile 文档
     *
     * @param fieldsJson     模板 fields JSON
     * @param schemaId       模板标识（如 topic_10023），可为 null
     * @param profileVersion Profile 版本，null 时取默认 "1"
     */
    public static JSONObject build(String fieldsJson, String schemaId, String profileVersion) {
        JSONObject profile = new JSONObject(true);
        profile.put("mip_version", MIP_VERSION);
        profile.put("schema_id", schemaId);
        profile.put("profile_version", profileVersion == null ? DEFAULT_PROFILE_VERSION : profileVersion);

        JSONArray fields = safeParse(fieldsJson);
        Set<Integer> elements = new LinkedHashSet<>();
        for (int i = 0; i < fields.size(); i++) {
            JSONObject f = fields.getJSONObject(i);
            Integer el = MipAttributesCodec.elementOf(f);
            if (el != null) {
                elements.add(el);
            }
        }
        profile.put("item_form", deriveItemForm(elements));
        profile.put("topic_type", deriveTopicType(elements));

        // properties：先装配元素与类型，sensitivity 二次填充（12 提级需先知全量 elements）
        JSONArray properties = new JSONArray();
        for (int el : elements) {
            if (!MipComponentRegistry.isKnown(el)) {
                continue;
            }
            JSONObject p = new JSONObject(true);
            p.put("element", el);
            p.put("name", nameOf(fields, el));
            p.put("type", MipComponentRegistry.valueType(el));
            p.put("required", el == 1 || el == 2 || el == 3);
            properties.add(p);
        }
        for (int i = 0; i < properties.size(); i++) {
            JSONObject p = properties.getJSONObject(i);
            p.put("sensitivity", MipAttributesCodec.resolveSensitivity(p.getIntValue("element"), properties));
        }
        profile.put("properties", properties);
        return profile;
    }

    /**
     * 形态推导：仅含 V 系列 → virtual；仅含 P 系列 → physical；混合或纯通用 → both
     */
    public static String deriveItemForm(Set<Integer> elements) {
        boolean hasV = elements.stream().anyMatch(e -> e >= V_FROM && e <= V_TO);
        boolean hasP = elements.stream().anyMatch(e -> e >= P_FROM && e <= P_TO);
        if (hasV && !hasP) {
            return MipComponentRegistry.FORM_VIRTUAL;
        }
        if (hasP && !hasV) {
            return MipComponentRegistry.FORM_PHYSICAL;
        }
        return "both";
    }

    /**
     * 主题类型推导（与 TopicFieldClassifier.classify 同语义）：模型型(3) &gt; 多媒体型(2) &gt; 扩展型(4) &gt; 基础型(1)
     */
    public static int deriveTopicType(Set<Integer> elements) {
        if (elements.contains(20)) {
            return 3;
        }
        if (elements.contains(12) || elements.contains(13) || elements.contains(14) || elements.contains(15)) {
            return 2;
        }
        if (elements.contains(17) || elements.contains(18) || elements.contains(19)) {
            return 4;
        }
        return 1;
    }

    private static JSONArray safeParse(String fieldsJson) {
        if (fieldsJson == null || fieldsJson.isBlank()) {
            return new JSONArray();
        }
        try {
            JSONArray arr = JSONArray.parseArray(fieldsJson);
            return arr == null ? new JSONArray() : arr;
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private static String nameOf(JSONArray fields, int element) {
        for (int i = 0; i < fields.size(); i++) {
            JSONObject f = fields.getJSONObject(i);
            Integer el = MipAttributesCodec.elementOf(f);
            if (el != null && el == element) {
                return f.getString("name");
            }
        }
        return MipComponentRegistry.get(element) == null ? null : MipComponentRegistry.get(element).name();
    }
}
