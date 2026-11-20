package org.jeecg.modules.app.mip;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * MIP v1 出域红线过滤器（MIP 协议第 4 章 / 11.1 时机三 / 11.2）
 * <p>
 * 出域（开放 API / MCP / 导出 / 二维码）不做格式校验而做过滤：
 * - restricted 一律剥离，不接受任何 AppKey 白名单豁免；
 * - private 未授权时剥离；
 * - 过滤后重算剥离视图的 content_hash 并标记 integrity.filtered = true。
 * <p>
 * 红线是过滤不是拒绝：静默剥离，不报错。所有出域出口必须经此过滤器。
 */
public final class MipRedlineFilter {

    private MipRedlineFilter() {
    }

    /**
     * 过滤 MIP 实例（原地修改 attributes 与 integrity）
     *
     * @param instance        MIP 实例（含 identity/attributes/custody/integrity）
     * @param privateGranted  是否授权读取 private（readwrite 或显式勾选）
     * @return 过滤后的实例（同一引用）
     */
    public static JSONObject filter(JSONObject instance, boolean privateGranted) {
        if (instance == null) {
            return null;
        }
        JSONArray attributes = instance.getJSONArray("attributes");
        JSONArray out = new JSONArray();
        if (attributes != null) {
            for (int i = 0; i < attributes.size(); i++) {
                JSONObject a = attributes.getJSONObject(i);
                if (a == null) {
                    continue;
                }
                String s = a.getString("sensitivity");
                if (MipComponentRegistry.SENS_RESTRICTED.equals(s)) {
                    continue;
                }
                if (MipComponentRegistry.SENS_PRIVATE.equals(s) && !privateGranted) {
                    continue;
                }
                out.add(a);
            }
        }
        instance.put("attributes", out);
        JSONObject integrity = instance.getJSONObject("integrity");
        if (integrity == null) {
            integrity = new JSONObject();
            instance.put("integrity", integrity);
        }
        // 过滤视图完整性：重算剥离后的 content_hash 并标记
        integrity.put("content_hash", MipHashUtil.contentHash(instance));
        integrity.put("filtered", true);
        return instance;
    }

    /**
     * 过滤原始 fields JSON（列表/轻量出域场景，不装配完整实例）：
     * restricted 一律剥离；private 未授权时剥离。幂等，返回新 JSON 字符串。
     */
    public static String filterFieldsJson(String fieldsJson, boolean privateGranted) {
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
        JSONArray out = new JSONArray();
        for (int i = 0; i < fields.size(); i++) {
            JSONObject f = fields.getJSONObject(i);
            if (f == null) {
                continue;
            }
            String s = f.getString("sensitivity");
            if (MipComponentRegistry.SENS_RESTRICTED.equals(s)) {
                continue;
            }
            if (MipComponentRegistry.SENS_PRIVATE.equals(s) && !privateGranted) {
                continue;
            }
            out.add(f);
        }
        return out.toJSONString();
    }
}
