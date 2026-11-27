package org.jeecg.modules.admin.ailing.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 芥灵过程 · AI 会话可视化只读 Mapper（P3-2，admin 模块自建 SQL，不依赖 ai 模块实体）
 * 数据源：tbl_ai_chat_session / tbl_ai_chat_message（零 DDL，联合查询装配）
 */
public interface AilingSessionAdminMapper {

    /** 模式判定表达式（三档：express>normal>query），复用于列表与模式分布 */
    String MODE_EXPR =
            "CASE WHEN EXISTS(SELECT 1 FROM tbl_ai_chat_message m WHERE m.session_id = s.id "
            + "AND m.msg_type IN ('process','receipt') AND JSON_VALID(m.process_entries) "
            + "AND JSON_EXTRACT(m.process_entries, '$.express') = true) THEN 'express' "
            + "WHEN EXISTS(SELECT 1 FROM tbl_ai_chat_message m WHERE m.session_id = s.id "
            + "AND (m.msg_type IN ('process','receipt') OR m.msg_type = 'draft')) THEN 'normal' "
            + "ELSE 'query' END";

    /**
     * 会话分页（view=列表行）：附消息数/模式/最后草稿置信度，LEFT JOIN sys_user 取用户名。
     * mode 在外层过滤（内层派生表），按 update_time desc 手工 LIMIT
     */
    @Select("<script>SELECT * FROM ("
            + "SELECT s.id, s.uid, s.title, s.status, s.create_time, s.update_time, u.username, "
            + "(SELECT count(*) FROM tbl_ai_chat_message m WHERE m.session_id = s.id) AS message_count, "
            + "(SELECT m.confidence FROM tbl_ai_chat_message m WHERE m.session_id = s.id "
            + "AND m.msg_type = 'draft' AND m.confidence IS NOT NULL "
            + "ORDER BY m.create_time DESC LIMIT 1) AS last_confidence, "
            + MODE_EXPR + " AS `mode` "
            + "FROM tbl_ai_chat_session s LEFT JOIN sys_user u ON u.id = s.uid "
            + "WHERE s.del_flag = 0"
            + "<if test='sessionId != null and sessionId != \"\"'> AND s.id = #{sessionId}</if>"
            + "<if test='uid != null and uid != \"\"'> AND s.uid = #{uid}</if>"
            + "<if test='username != null and username != \"\"'> AND u.username like concat('%',#{username},'%')</if>"
            + "<if test='title != null and title != \"\"'> AND s.title like concat('%',#{title},'%')</if>"
            + "<if test='status != null and status != \"\"'> AND s.status = #{status}</if>"
            + "<if test='createTimeStart != null'> AND s.create_time &gt;= #{createTimeStart}</if>"
            + "<if test='createTimeEnd != null'> AND s.create_time &lt;= #{createTimeEnd}</if>"
            + ") t "
            + "<if test='mode != null and mode != \"\"'> WHERE t.`mode` = #{mode}</if>"
            + " ORDER BY t.update_time DESC LIMIT #{page.offset}, #{page.size}"
            + "</script>")
    List<Map<String, Object>> selectSessionPage(AilingItemAdminMapper.PageArgs page, @Param("sessionId") String sessionId,
                                                @Param("uid") String uid, @Param("username") String username,
                                                @Param("title") String title, @Param("status") String status,
                                                @Param("mode") String mode,
                                                @Param("createTimeStart") Date createTimeStart,
                                                @Param("createTimeEnd") Date createTimeEnd);

    /** 会话总数（del_flag=0） */
    @Select("SELECT count(*) FROM tbl_ai_chat_session WHERE del_flag = 0")
    Long countSessions();

    /** 发起过创建的会话数（process 卡 step=create）——建档成功率分母 */
    @Select("SELECT count(DISTINCT s.session_id) FROM tbl_ai_chat_message s "
            + "WHERE s.msg_type = 'process' AND JSON_VALID(s.process_entries) "
            + "AND JSON_EXTRACT(s.process_entries, '$.step') = 'create'")
    Long countCreateSessions();

    /** 建档成功会话数（存在 receipt 卡，仅成功时追加）——建档成功率分子 */
    @Select("SELECT count(DISTINCT session_id) FROM tbl_ai_chat_message WHERE msg_type = 'receipt'")
    Long countReceiptSessions();

    /** express 发起会话数（process 卡 express=true）——直达率分母 */
    @Select("SELECT count(DISTINCT session_id) FROM tbl_ai_chat_message "
            + "WHERE msg_type = 'process' AND JSON_VALID(process_entries) "
            + "AND JSON_EXTRACT(process_entries, '$.express') = true")
    Long countExpressSessions();

    /** express 直达成功会话数（receipt 卡 express=true）——直达率分子 */
    @Select("SELECT count(DISTINCT session_id) FROM tbl_ai_chat_message "
            + "WHERE msg_type = 'receipt' AND JSON_VALID(process_entries) "
            + "AND JSON_EXTRACT(process_entries, '$.express') = true")
    Long countExpressSuccessSessions();

    /** 平均置信度（draft 卡 confidence 非空均值） */
    @Select("SELECT AVG(confidence) FROM tbl_ai_chat_message "
            + "WHERE msg_type = 'draft' AND confidence IS NOT NULL")
    Double avgConfidence();

    /** 追问轮次直方图（按会话 clarify 条数 1/2/3+，0 桶 Java 侧补算） */
    @Select("SELECT CASE WHEN cnt >= 3 THEN '3+' ELSE CAST(cnt AS CHAR) END AS k, count(*) AS c "
            + "FROM (SELECT session_id, count(*) AS cnt FROM tbl_ai_chat_message "
            + "WHERE msg_type = 'clarify' GROUP BY session_id) t GROUP BY k ORDER BY k")
    List<Map<String, Object>> clarifyHist();

    /** 会话量趋势（30d 按日） */
    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS k, count(*) AS c "
            + "FROM tbl_ai_chat_session WHERE del_flag = 0 AND create_time >= #{start} "
            + "GROUP BY k ORDER BY k ASC")
    List<Map<String, Object>> sessionTrend(@Param("start") Date start);

    /** 模式分布（三档会话数） */
    @Select("SELECT t.`mode` AS k, count(*) AS c FROM (SELECT s.id, "
            + MODE_EXPR + " AS `mode` FROM tbl_ai_chat_session s WHERE s.del_flag = 0) t "
            + "GROUP BY t.`mode` ORDER BY c DESC")
    List<Map<String, Object>> modeDist();

    /** 会话详情（session 元信息 + username） */
    @Select("SELECT s.*, u.username FROM tbl_ai_chat_session s "
            + "LEFT JOIN sys_user u ON u.id = s.uid "
            + "WHERE s.id = #{sessionId} AND s.del_flag = 0")
    Map<String, Object> selectSessionDetail(@Param("sessionId") String sessionId);

    /** 会话消息流（create_time ASC，≤200 条，与 App listBySession 同口径） */
    @Select("SELECT * FROM tbl_ai_chat_message WHERE session_id = #{sessionId} "
            + "ORDER BY create_time ASC LIMIT 200")
    List<Map<String, Object>> selectMessages(@Param("sessionId") String sessionId);

    /** 会话回放审计查询（tbl_stats_event event=ai_session_viewed 最近 200 条） */
    @Select("SELECT uid, props, server_ts, stats_date FROM tbl_stats_event "
            + "WHERE event = 'ai_session_viewed' ORDER BY server_ts DESC LIMIT 200")
    List<Map<String, Object>> selectAuditList();
}
