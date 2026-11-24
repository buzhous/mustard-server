package org.jeecg.modules.app.mip;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * MIP v1 实例校验器（MIP 协议 11.1/11.2/11.3）
 * <p>
 * 校验时机：实例写入时（新增/编辑/AI 入库/导入）。只做结构、类型与安全校验，
 * 不校验业务语义（分类存在性、模板停用由既有服务层负责）。
 * E-SYNC 冗余不一致以组件值为权威自动回写，不作为阻断错误（由调用方处理回写）。
 * <p>
 * 实例 attributes 元素结构：{element, name, value, sensitivity}
 */
public final class MipValidator {

    /** Profile 侧必填三要素（模板配置强制 1/2/3） */
    private static final Set<Integer> REQUIRED = Set.of(1, 2, 3);
    /** 实例级必填（1名称/3类别）；图标(2) 实例级可缺省——AI 建档 icon 为可选槽位，App 端按空图标走本地占位展示 */
    private static final Set<Integer> INSTANCE_REQUIRED = Set.of(1, 3);

    private MipValidator() {
    }

    /**
     * 校验 MIP 实例 attributes；返回错误码列表，空列表=通过
     *
     * @param attributes 实例属性数组 [{element,name,value,sensitivity}]
     * @param identity   实例身份段 {oriId,name,category,icon,level}
     */
    public static List<String> validate(JSONArray attributes, JSONObject identity) {
        List<String> errs = new ArrayList<>();
        if (attributes == null || attributes.isEmpty()) {
            errs.add("E-REQ|缺少必填组件 1,2,3");
            return errs;
        }
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < attributes.size(); i++) {
            JSONObject a = attributes.getJSONObject(i);
            Integer elBoxed = MipAttributesCodec.elementOf(a);
            if (elBoxed == null) {
                errs.add("E-ELEM|element 非法");
                continue;
            }
            int el = elBoxed;
            Object value = a.get("value");
            if (!seen.add(el)) {
                errs.add("E-DUP|element " + el + " 重复");
                continue;
            }
            String type = MipComponentRegistry.valueType(el);
            if (type == null) {
                errs.add("E-ELEM|" + el + " 非法组件");
                continue;
            }
            validateValue(errs, el, type, value);
        }
        for (int r : INSTANCE_REQUIRED) {
            if (!seen.contains(r)) {
                errs.add("E-REQ|缺少必填组件 " + r);
            }
        }
        // 冗余一致性：identity 与组件 1/2/3/4 对齐（E-SYNC 以组件值为准自动回写）
        if (identity != null) {
            if (!Objects.equals(identity.getString("name"), attrStringValue(attributes, 1))) {
                errs.add("E-SYNC|identity.name 与组件 1 不一致");
            }
            if (!Objects.equals(identity.getString("icon"), attrStringValue(attributes, 2))) {
                errs.add("E-SYNC|identity.icon 与组件 2 不一致");
            }
        }
        return errs;
    }

    /**
     * Profile 侧校验（模板保存时调用，MIP 协议 11.1 时机一）：
     * sensitivity 只能 private→restricted 提级；LOCKED_RESTRICTED 不可降级；
     * 非 PUBLIC_CAPABLE 组件不可声明 public；形态不符组件报 E-FORM。
     *
     * @return 校验失败原因；null 表示通过
     */
    public static String validateProfile(JSONArray properties, String itemForm) {
        if (properties == null) {
            return null;
        }
        Set<Integer> elements = new HashSet<>();
        for (int i = 0; i < properties.size(); i++) {
            JSONObject p = properties.getJSONObject(i);
            Integer elBoxed = MipAttributesCodec.elementOf(p);
            if (elBoxed == null) {
                return "element 非法组件";
            }
            int el = elBoxed;
            elements.add(el);
            if (!MipComponentRegistry.isKnown(el)) {
                return "element " + el + " 非法组件";
            }
            if (!MipComponentRegistry.formAllowed(itemForm, el)) {
                return "E-FORM|element " + el + " 与形态 " + itemForm + " 不符";
            }
            String s = p.getString("sensitivity");
            if (MipComponentRegistry.LOCKED_RESTRICTED.contains(el) && !MipComponentRegistry.SENS_RESTRICTED.equals(s)) {
                return "element " + el + " 必须为 restricted，不可降级";
            }
            if (MipComponentRegistry.SENS_PUBLIC.equals(s) && !MipComponentRegistry.PUBLIC_CAPABLE.contains(el)) {
                return "element " + el + " 不可声明为 public";
            }
        }
        for (int r : REQUIRED) {
            if (!elements.contains(r)) {
                return "字段配置必须包含：名称(1)、图标(2)、类别(3)";
            }
        }
        return null;
    }

    // ================================ 私有方法 ================================

    private static void validateValue(List<String> errs, int el, String type, Object value) {
        String v = value == null ? "" : String.valueOf(value).trim();
        switch (type) {
            case "string", "html" -> {
                if (v.isEmpty() || v.length() > 2000) {
                    errs.add("E-LEN|" + el);
                }
            }
            case "url" -> {
                if (!(v.startsWith("https://") || v.startsWith("http://"))) {
                    errs.add("E-URL|" + el);
                }
            }
            case "arrurl" -> {
                JSONArray arr = toJSONArray(value);
                if (arr == null || arr.isEmpty()) {
                    errs.add("E-EMPTY|" + el);
                } else {
                    for (Object u : arr) {
                        String s = String.valueOf(u);
                        if (!(s.startsWith("https://") || s.startsWith("http://"))) {
                            errs.add("E-URL|" + el);
                        }
                    }
                }
            }
            case "arrstr" -> {
                JSONArray arr = toJSONArray(value);
                if (el == 10 && arr != null && arr.size() > 3) {
                    errs.add("E-TAGS|标签组超 3 个");
                }
            }
            case "int" -> {
                if (el == 4 && (toInt(value) < 0 || toInt(value) > 3)) {
                    errs.add("E-RANGE|等级越界");
                }
                if (el == 31 && (toInt(value) < 0 || toInt(value) > 10)) {
                    errs.add("E-RANGE|成色评级越界");
                }
            }
            case "number" -> {
                if (el == 6 && toDouble(value) < 0) {
                    errs.add("E-RANGE|价格非负");
                }
            }
            case "date" -> {
                if (v.isEmpty() || v.length() > 32) {
                    errs.add("E-RANGE|" + el);
                }
            }
            case "daterange" -> {
                JSONArray r = toJSONArray(value);
                if (r == null || r.size() != 2
                        || String.valueOf(r.get(0)).compareTo(String.valueOf(r.get(1))) > 0) {
                    errs.add("E-RANGE|" + el);
                }
            }
            case "obj" -> {
                if (!(value instanceof JSONObject)) {
                    errs.add("E-EMPTY|" + el);
                }
            }
            case "arrobj" -> {
                if (toJSONArray(value) == null) {
                    errs.add("E-EMPTY|" + el);
                }
            }
            default -> {
                // 其余类型不做额外约束
            }
        }
    }

    private static String attrStringValue(JSONArray attributes, int element) {
        for (int i = 0; i < attributes.size(); i++) {
            JSONObject a = attributes.getJSONObject(i);
            Integer el = MipAttributesCodec.elementOf(a);
            if (el != null && el == element) {
                return a.getString("value");
            }
        }
        return null;
    }

    private static JSONArray toJSONArray(Object value) {
        if (value instanceof JSONArray arr) {
            return arr;
        }
        if (value instanceof String s && !s.isBlank()) {
            try {
                return JSONArray.parseArray(s);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private static int toInt(Object value) {
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return -1;
        }
    }

    private static double toDouble(Object value) {
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return -1;
        }
    }
}
