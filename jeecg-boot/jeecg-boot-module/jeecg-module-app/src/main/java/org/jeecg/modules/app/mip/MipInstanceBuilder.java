package org.jeecg.modules.app.mip;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.Date;

/**
 * MIP 实例装配器（协议第 2 章）
 * <p>
 * 由物品档案（fields + identity 列）装配标准 MIP 实例视图：
 * header / identity / attributes / custody / lineage_digest / integrity。
 * 出域前必须再经 {@link MipRedlineFilter} 过滤。
 */
public final class MipInstanceBuilder {

    private MipInstanceBuilder() {
    }

    /**
     * 装配 MIP 实例
     *
     * @param oriId          物品全局唯一标识
     * @param schemaId       模板标识（topic_{topicId}），可为 null
     * @param profileVersion Profile 版本
     * @param fieldsJson     物品 fields（应已含 sensitivity）
     * @param profileJson    模板 Profile 文档 JSON，可为 null
     * @param name           物品名称（identity.name）
     * @param category       分类 ID
     * @param icon           图标 URL
     * @param level          等级 0-3
     * @param ownerUidHash   持有者 uid 哈希（LineageService.hashUid）
     * @param stationId      仓位
     * @param acquiredAt     获得时间
     * @param lineageDigest  账本摘要 {event_count, head_hash}
     */
    public static JSONObject build(String oriId, String schemaId, String profileVersion,
                                   String fieldsJson, String profileJson,
                                   String name, String category, String icon, Integer level,
                                   String ownerUidHash, String stationId, Date acquiredAt,
                                   JSONObject lineageDigest) {
        JSONObject instance = new JSONObject(true);

        JSONObject header = new JSONObject(true);
        header.put("mip_version", MipProfileBuilder.MIP_VERSION);
        header.put("schema_id", schemaId);
        header.put("profile_version", profileVersion);
        header.put("generated_at", new Date().toInstant().toString());
        instance.put("header", header);

        JSONObject identity = new JSONObject(true);
        identity.put("ori_id", oriId);
        identity.put("name", name);
        identity.put("category", category);
        identity.put("icon", icon);
        identity.put("level", level);
        instance.put("identity", identity);

        instance.put("attributes", MipAttributesCodec.buildAttributes(fieldsJson, safeParse(profileJson)));

        JSONObject custody = new JSONObject(true);
        custody.put("owner_uid", ownerUidHash);
        custody.put("station_id", stationId);
        custody.put("acquired_at", acquiredAt == null ? null : acquiredAt.toInstant().toString());
        instance.put("custody", custody);

        if (lineageDigest != null) {
            instance.put("lineage_digest", lineageDigest);
        }

        JSONObject integrity = new JSONObject(true);
        integrity.put("content_hash", MipHashUtil.contentHash(instance));
        instance.put("integrity", integrity);
        return instance;
    }

    private static JSONObject safeParse(String profileJson) {
        if (profileJson == null || profileJson.isBlank()) {
            return null;
        }
        try {
            return JSONObject.parseObject(profileJson);
        } catch (Exception e) {
            return null;
        }
    }
}
