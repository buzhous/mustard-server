package org.jeecg.modules.app.mip;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * MIP v1 写路径增强（协议 10.3 缺陷③ / 11.1 时机二）
 * <p>
 * 写入时服务端单点补级：fields 落库前追加 sensitivity（幂等，App/AI 传什么都被重算覆盖），
 * 并按组件值为权威回写 identity 列（E-SYNC 冗余一致性，协议 2.1）。
 */
public final class MipWriteSupport {

    private MipWriteSupport() {
    }

    /**
     * fields 落库前补级：按 fields 现场推导 Profile 后追加 sensitivity。
     * 模板 mip_profile 由 T4 钩子维护，此处现场推导可独立于模板数据工作且结果一致。
     */
    public static String enrich(String fieldsJson) {
        if (fieldsJson == null || fieldsJson.isBlank()) {
            return fieldsJson;
        }
        JSONObject profile = MipProfileBuilder.build(fieldsJson, null, null);
        return MipAttributesCodec.upgradeFields(fieldsJson, profile);
    }

    /**
     * E-SYNC 冗余计算：以组件值为权威，从组件 1/2/3/4 提取 identity 列值（E-SYNC，协议 2.1）。
     * 组件缺失的键为 null，调用方保留原列值（不抹数据）。
     */
    public static IdentitySync computeIdentitySync(String fieldsJson) {
        String name = null;
        String icon = null;
        String category = null;
        Integer level = null;
        if (fieldsJson != null && !fieldsJson.isBlank()) {
            JSONArray fields;
            try {
                fields = JSONArray.parseArray(fieldsJson);
            } catch (Exception e) {
                fields = null;
            }
            if (fields != null) {
                for (int i = 0; i < fields.size(); i++) {
                    JSONObject f = fields.getJSONObject(i);
                    Integer element = MipAttributesCodec.elementOf(f);
                    if (element == null) {
                        continue;
                    }
                    Object value = MipAttributesCodec.contentValue(element, f.getJSONObject("content"));
                    if (value == null) {
                        continue;
                    }
                    String strValue = String.valueOf(value);
                    switch (element) {
                        case 1 -> name = strValue;
                        case 2 -> icon = strValue;
                        case 3 -> category = strValue;
                        case 4 -> {
                            try {
                                level = Integer.parseInt(strValue);
                            } catch (NumberFormatException ignored) {
                                // 非法等级保留原列值
                            }
                        }
                        default -> {
                            // 其余组件不冗余
                        }
                    }
                }
            }
        }
        return new IdentitySync(name, icon, category, level);
    }

    /** identity 列同步结果（null=保留原列值） */
    public record IdentitySync(String name, String icon, String category, Integer level) {
    }
}
