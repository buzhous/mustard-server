package org.jeecg.modules.app.mip;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * MIP v1 fields 原地升级编解码器（MIP 协议 10.3 最终方案 #1/#3）
 * <p>
 * fields 组件对象结构：{element, name, sort, isShow, attributes, content}（保持不变），
 * 仅追加 sensitivity 键 —— App/服务端既有 fields 引用继续工作，content 仍为值载体。
 * <p>
 * sensitivity 为<strong>服务端单点计算</strong>：注册表默认 + Profile 覆盖（仅 private→restricted）
 * + 证件类 element 12 提级规则；App 与 AI 只产值不产级（协议 10.3 缺陷③）。
 * 本类所有方法幂等：重复执行结果一致，可安全用于迁移脚本与写入链路。
 */
public final class MipAttributesCodec {

    private MipAttributesCodec() {
    }

    /**
     * 容错提取 element 编号：缺失/非数字（存量脏数据如字符串）返回 null 而非抛异常，
     * 保证迁移与视图装配不因单条脏组件中断。
     */
    public static Integer elementOf(JSONObject o) {
        if (o == null) {
            return null;
        }
        Object v = o.get("element");
        if (v == null) {
            return null;
        }
        if (v instanceof Number num) {
            return num.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 单点计算组件 sensitivity：
     * 1) 注册表默认值；2) Profile 声明覆盖（仅 private→restricted 方向，LOCKED 组件强制 restricted）；
     * 3) Profile 含 element 21 时 element 12 自动提级 restricted。
     *
     * @param element     组件编号
     * @param profileProperties Profile 的 properties 数组，可为 null（无 Profile 时用注册表默认）
     */
    public static String resolveSensitivity(int element, JSONArray profileProperties) {
        String resolved = MipComponentRegistry.defaultSensitivity(element);
        if (profileProperties != null) {
            for (int i = 0; i < profileProperties.size(); i++) {
                JSONObject p = profileProperties.getJSONObject(i);
                Integer pe = elementOf(p);
                if (pe == null || pe != element) {
                    continue;
                }
                String declared = p.getString("sensitivity");
                if (MipComponentRegistry.SENS_RESTRICTED.equals(declared)
                        && !MipComponentRegistry.SENS_PUBLIC.equals(resolved)) {
                    resolved = MipComponentRegistry.SENS_RESTRICTED;
                }
                break;
            }
        }
        // 证件类 Profile：element 12（图片）与证件号同级保护
        if (element == MipComponentRegistry.ELEMENT_IMAGES
                && containsElement(profileProperties, MipComponentRegistry.ELEMENT_CERT_NO)) {
            resolved = MipComponentRegistry.SENS_RESTRICTED;
        }
        // LOCKED 组件兜底（Profile 非法声明已由 validateProfile 拦截，此处防御性强制）
        if (MipComponentRegistry.LOCKED_RESTRICTED.contains(element)) {
            resolved = MipComponentRegistry.SENS_RESTRICTED;
        }
        return resolved;
    }

    /**
     * fields 原地升级：为每个组件对象追加 sensitivity 键（保留 element/name/sort/isShow/attributes/content 全部既有键）。
     * 幂等：已有 sensitivity 直接按当前规则重算覆盖。
     *
     * @param fieldsJson 原 fields JSON 字符串
     * @param profile    Profile 文档 JSONObject（含 properties），可为 null
     * @return 升级后的 fields JSON 字符串；入参非法时原样返回
     */
    public static String upgradeFields(String fieldsJson, JSONObject profile) {
        if (fieldsJson == null || fieldsJson.isBlank()) {
            return fieldsJson;
        }
        JSONArray fields;
        try {
            fields = JSONArray.parseArray(fieldsJson);
        } catch (Exception e) {
            return fieldsJson;
        }
        if (fields == null) {
            return fieldsJson;
        }
        JSONArray properties = profile == null ? null : profile.getJSONArray("properties");
        for (int i = 0; i < fields.size(); i++) {
            JSONObject f = fields.getJSONObject(i);
            Integer el = elementOf(f);
            if (el == null || !MipComponentRegistry.isKnown(el)) {
                continue;
            }
            f.put("sensitivity", resolveSensitivity(el, properties));
        }
        return JSON.toJSONString(fields);
    }

    /**
     * 从 content 对象提取组件值（按组件类型的键优先级）。
     * 只读不改存储；提取结果用于 MIP 实例视图装配与校验。
     */
    public static Object contentValue(int element, JSONObject content) {
        if (content == null) {
            return null;
        }
        String type = MipComponentRegistry.valueType(element);
        if (type == null) {
            return firstNonNull(content, "value", "text");
        }
        return switch (type) {
            case "string" -> firstNonNull(content, "text", "name", "value", "password");
            case "html" -> firstNonNull(content, "rich", "text");
            case "url" -> firstNonNull(content, "url", "path");
            case "int" -> firstNonNull(content, element == 4 ? "level" : "value", "text");
            case "number" -> firstNonNull(content, "price", "text");
            case "date" -> firstNonNull(content, "date", "text");
            case "daterange" -> firstNonNull(content, "date", "text", "value");
            case "arrstr" -> content.get("tags");          // 标签组
            case "arrobj" -> content.get("attributes");    // 属性组/维护记录
            case "arrurl" -> mediasToUrls(content);        // 图片/视频/附件组
            case "obj" -> {
                if (element == 18) {                        // 定位 {lat,lng,address}
                    JSONObject loc = new JSONObject();
                    loc.put("lat", content.get("latitude"));
                    loc.put("lng", content.get("longitude"));
                    loc.put("address", content.get("address"));
                    yield loc;
                }
                yield firstNonNull(content, "value", "text");
            }
            default -> firstNonNull(content, "value", "text");
        };
    }

    /**
     * fields JSON → MIP 实例 attributes 数组 [{element,name,value,sensitivity}]
     * （出域视图装配用，不改动存储）
     */
    public static JSONArray buildAttributes(String fieldsJson, JSONObject profile) {
        JSONArray out = new JSONArray();
        if (fieldsJson == null || fieldsJson.isBlank()) {
            return out;
        }
        JSONArray fields;
        try {
            fields = JSONArray.parseArray(fieldsJson);
        } catch (Exception e) {
            return out;
        }
        if (fields == null) {
            return out;
        }
        JSONArray properties = profile == null ? null : profile.getJSONArray("properties");
        for (int i = 0; i < fields.size(); i++) {
            JSONObject f = fields.getJSONObject(i);
            Integer el = elementOf(f);
            if (el == null || !MipComponentRegistry.isKnown(el)) {
                continue;
            }
            JSONObject a = new JSONObject();
            a.put("element", el);
            a.put("name", f.getString("name"));
            a.put("value", contentValue(el, f.getJSONObject("content")));
            a.put("sensitivity", f.getString("sensitivity") != null
                    ? f.getString("sensitivity")
                    : resolveSensitivity(el, properties));
            out.add(a);
        }
        return out;
    }

    // ================================ 私有方法 ================================

    private static boolean containsElement(JSONArray properties, int element) {
        if (properties == null) {
            return false;
        }
        for (int i = 0; i < properties.size(); i++) {
            JSONObject p = properties.getJSONObject(i);
            Integer pe = elementOf(p);
            if (pe != null && pe == element) {
                return true;
            }
        }
        return false;
    }

    private static Object firstNonNull(JSONObject content, String... keys) {
        for (String k : keys) {
            Object v = content.get(k);
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    /** content.medias → url 数组；无 medias 时回退 path/url 标量包数组 */
    private static JSONArray mediasToUrls(JSONObject content) {
        JSONArray medias = content.getJSONArray("medias");
        if (medias != null && !medias.isEmpty()) {
            JSONArray urls = new JSONArray();
            for (int i = 0; i < medias.size(); i++) {
                JSONObject m = medias.getJSONObject(i);
                if (m == null) {
                    continue;
                }
                String url = m.getString("url") != null ? m.getString("url") : m.getString("path");
                if (url != null) {
                    urls.add(url);
                }
            }
            return urls;
        }
        String single = content.getString("path") != null ? content.getString("path") : content.getString("url");
        if (single != null) {
            JSONArray urls = new JSONArray();
            urls.add(single);
            return urls;
        }
        return null;
    }
}
