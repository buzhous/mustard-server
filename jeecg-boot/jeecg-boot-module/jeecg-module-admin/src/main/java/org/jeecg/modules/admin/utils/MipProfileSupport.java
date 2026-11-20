package org.jeecg.modules.admin.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * MIP v1 Profile 生成与校验（admin 模块侧，协议 3.3 / 11.1 时机一）
 * <p>
 * 模块隔离约束：jeecg-module-admin 与 jeecg-module-app 无相互依赖，本类与 app 模块的
 * MipProfileBuilder / MipComponentRegistry 同语义实现，<strong>修改必须双侧同步</strong>。
 * <p>
 * 职责：模板保存/更新时 ① 校验 fields 合法性（含 21-34 扩展组件）；
 * ② 自动生成 mip_profile 文档并落 tbl_topic.mip_profile。
 * sensitivity 规则：注册表默认 + 证件类 element 12 提级；LOCKED_RESTRICTED 不得降级。
 */
public final class MipProfileSupport {

    public static final String MIP_VERSION = "1.0";
    public static final String DEFAULT_PROFILE_VERSION = "1";

    private static final Set<Integer> LOCKED_RESTRICTED = Set.of(17, 18, 21, 22, 25, 26, 33);
    private static final Set<Integer> PUBLIC_CAPABLE = Set.of(1, 2, 3, 4, 8, 10, 23, 27, 29);
    private static final Set<Integer> VIRTUAL_ONLY = Set.of(23, 24, 25, 26, 27, 28);
    private static final Set<Integer> PHYSICAL_ONLY = Set.of(29, 30, 31, 32, 33, 34);

    /** element → [语义, value类型, 默认sensitivity]（与 MipComponentRegistry 同源） */
    private static final Object[][] COMPONENTS = {
            {1, "名称", "string", "public"}, {2, "图标", "url", "public"},
            {3, "类别", "int", "public"}, {4, "等级", "int", "public"},
            {5, "购入时间", "date", "private"}, {6, "价格", "number", "private"},
            {7, "数量", "int", "private"}, {8, "描述", "string", "public"},
            {9, "特殊描述", "string", "private"}, {10, "标签组", "arrstr", "public"},
            {11, "属性组", "arrobj", "private"}, {12, "图片", "arrurl", "private"},
            {13, "视频", "arrurl", "private"}, {14, "文图视附件", "arrurl", "private"},
            {15, "附件", "arrurl", "private"}, {16, "电子书", "url", "private"},
            {17, "密码", "string", "restricted"}, {18, "定位", "obj", "restricted"},
            {19, "富文本", "html", "private"}, {20, "3D模型", "url", "private"},
            {21, "证件号", "string", "restricted"}, {22, "有效期范围", "daterange", "restricted"},
            {23, "发行方/平台", "string", "public"}, {24, "权益有效期", "daterange", "private"},
            {25, "账号标识", "string", "restricted"}, {26, "兑换码/激活码", "string", "restricted"},
            {27, "链上凭证", "obj", "public"}, {28, "云端存放位置", "url", "private"},
            {29, "品牌/型号", "obj", "public"}, {30, "序列号/SN", "string", "private"},
            {31, "成色评级", "int", "private"}, {32, "保修信息", "obj", "private"},
            {33, "购买凭证", "arrurl", "restricted"}, {34, "维护记录", "arrobj", "private"},
    };

    private MipProfileSupport() {
    }

    /**
     * fields 组件合法性校验（profile 侧视角）：非法组件 / LOCKED 降级声明拦截。
     * 必填 1/2/3 已由 TopicFieldClassifier.validate 覆盖，此处不重复。
     *
     * @return 校验失败原因；null 表示通过
     */
    public static String validateFields(String fieldsJson) {
        if (fieldsJson == null || fieldsJson.isBlank()) {
            return null;
        }
        JSONArray fields;
        try {
            fields = JSONArray.parseArray(fieldsJson);
        } catch (Exception e) {
            return "字段配置不是合法的 JSON 数组";
        }
        if (fields == null) {
            return null;
        }
        for (int i = 0; i < fields.size(); i++) {
            JSONObject f = fields.getJSONObject(i);
            if (f == null) {
                continue;
            }
            Integer element = f.getInteger("element");
            if (element == null || !isKnown(element)) {
                return "element " + element + " 非法组件（合法范围 1-34）";
            }
        }
        return null;
    }

    /**
     * 由 fields 生成 Profile 文档 JSON（保存钩子调用，幂等：同 fields 结果一致）
     */
    public static String buildProfileJson(String fieldsJson, String schemaId) {
        JSONObject profile = new JSONObject(true);
        profile.put("mip_version", MIP_VERSION);
        profile.put("schema_id", schemaId);
        profile.put("profile_version", DEFAULT_PROFILE_VERSION);

        JSONArray fields = parse(fieldsJson);
        Set<Integer> elements = new LinkedHashSet<>();
        for (int i = 0; i < fields.size(); i++) {
            JSONObject f = fields.getJSONObject(i);
            if (f != null && f.getInteger("element") != null && isKnown(f.getIntValue("element"))) {
                elements.add(f.getIntValue("element"));
            }
        }
        boolean hasV = elements.stream().anyMatch(VIRTUAL_ONLY::contains);
        boolean hasP = elements.stream().anyMatch(PHYSICAL_ONLY::contains);
        profile.put("item_form", hasV && hasP ? "both" : hasV ? "virtual" : hasP ? "physical" : "both");

        int topicType = 1;
        if (elements.contains(20)) {
            topicType = 3;
        } else if (elements.contains(12) || elements.contains(13) || elements.contains(14) || elements.contains(15)) {
            topicType = 2;
        } else if (elements.contains(17) || elements.contains(18) || elements.contains(19)) {
            topicType = 4;
        }
        profile.put("topic_type", topicType);

        JSONArray properties = new JSONArray();
        for (int el : elements) {
            JSONObject p = new JSONObject(true);
            p.put("element", el);
            p.put("name", nameOf(fields, el, el));
            p.put("type", typeOf(el));
            p.put("required", el == 1 || el == 2 || el == 3);
            properties.add(p);
        }
        for (int i = 0; i < properties.size(); i++) {
            JSONObject p = properties.getJSONObject(i);
            p.put("sensitivity", resolveSensitivity(p.getIntValue("element"), properties));
        }
        profile.put("properties", properties);
        return profile.toJSONString();
    }

    /**
     * sensitivity 单点计算（与 app 模块 MipAttributesCodec.resolveSensitivity 同语义）：
     * 注册表默认 + 证件类 element 12 提级 + LOCKED 强制 restricted
     */
    public static String resolveSensitivity(int element, JSONArray properties) {
        String resolved = defaultSensitivity(element);
        if (element == 12 && contains(properties, 21)) {
            resolved = "restricted";
        }
        if (LOCKED_RESTRICTED.contains(element)) {
            resolved = "restricted";
        }
        return resolved;
    }

    // ================================ 私有方法 ================================

    private static boolean isKnown(int element) {
        return element >= 1 && element <= COMPONENTS.length
                && (Integer) COMPONENTS[element - 1][0] == element;
    }

    private static String typeOf(int element) {
        return (String) COMPONENTS[element - 1][2];
    }

    private static String defaultSensitivity(int element) {
        return (String) COMPONENTS[element - 1][3];
    }

    private static boolean contains(JSONArray properties, int element) {
        if (properties == null) {
            return false;
        }
        for (int i = 0; i < properties.size(); i++) {
            JSONObject p = properties.getJSONObject(i);
            if (p != null && p.getIntValue("element") == element) {
                return true;
            }
        }
        return false;
    }

    private static String nameOf(JSONArray fields, int element, int fallback) {
        for (int i = 0; i < fields.size(); i++) {
            JSONObject f = fields.getJSONObject(i);
            if (f != null && f.getIntValue("element") == element) {
                return f.getString("name");
            }
        }
        return (String) COMPONENTS[fallback - 1][1];
    }

    private static JSONArray parse(String fieldsJson) {
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
}
