package org.jeecg.modules.admin.ailing.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 芥灵AI 物品工作台 · 流转账本只读 Mapper（tbl_item_lineage 按 item_id 直查 + spawn 父子链）
 */
public interface AilingLineageAdminMapper {

    @Select("SELECT id, ori_id, item_id, event_type, from_uid, to_uid, from_station, to_station, "
            + "operator, payload, qty, parent_ori, idempotency_key, occurred_at, prev_hash, hash "
            + "FROM tbl_item_lineage WHERE item_id = #{itemId} ORDER BY id ASC")
    List<Map<String, Object>> selectByItemId(@Param("itemId") String itemId);

    @Select("SELECT id, ori_id, item_id, event_type, from_uid, to_uid, operator, qty, parent_ori, occurred_at "
            + "FROM tbl_item_lineage WHERE parent_ori = #{oriId} ORDER BY id ASC")
    List<Map<String, Object>> selectSpawnChildren(@Param("oriId") String oriId);

    /**
     * AI 操作事件（P3-3 双源归一）：tbl_ai_chat_message 中 process/receipt 卡
     * process_entries.itemIds 含该 itemId 的消息（JSON_VALID 守卫防脏数据；不入哈希链）
     */
    @Select("SELECT m.id AS msg_id, m.session_id, m.uid, m.create_time, m.msg_type, m.process_entries "
            + "FROM tbl_ai_chat_message m "
            + "WHERE m.msg_type IN ('process','receipt') AND JSON_VALID(m.process_entries) "
            + "AND JSON_CONTAINS(JSON_EXTRACT(m.process_entries, '$.itemIds'), JSON_QUOTE(#{itemId})) "
            + "ORDER BY m.create_time ASC")
    List<Map<String, Object>> selectAiEventsByItemId(@Param("itemId") String itemId);
}
