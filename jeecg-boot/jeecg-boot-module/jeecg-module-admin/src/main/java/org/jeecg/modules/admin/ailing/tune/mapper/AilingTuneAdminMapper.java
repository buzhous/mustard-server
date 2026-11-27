package org.jeecg.modules.admin.ailing.tune.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 芥灵调优中心 Mapper（P4，admin 模块自建 SQL）：兜底链观测聚合（tbl_stats_event）+ 样本池（tbl_ai_feedback）。
 * 调优配置读写走 ai 模块 TuningConfigMapper（MyBatis-Plus，admin→ai 依赖链复用）。
 */
public interface AilingTuneAdminMapper {

    /**
     * 观测聚合：取近 N 天 ai_search_trace 的 props 原文（Java 侧解析聚合）。
     * 说明：JSON_TABLE 聚合在 MySQL 侧报 Incorrect arguments 且 MyBatis-Plus JSqlParser
     * 无法解析该语法（T4 验证期 500），故改为取原文服务端聚合，样本上限 2000 与 p95 口径一致。
     */
    @Select("SELECT props FROM tbl_stats_event "
            + "WHERE event = 'ai_search_trace' AND JSON_VALID(props) AND server_ts >= #{since} "
            + "ORDER BY server_ts DESC LIMIT 2000")
    List<String> selectTraceProps(@Param("since") long since);

    /** 样本池分页（tbl_ai_feedback），create_time desc + LIMIT */
    @Select("<script>SELECT f.*, u.username FROM tbl_ai_feedback f LEFT JOIN sys_user u ON u.id = f.uid "
            + "WHERE 1 = 1"
            + "<if test='sourceType != null'> AND f.source_type = #{sourceType}</if>"
            + "<if test='sampleStatus != null'> AND f.sample_status = #{sampleStatus}</if>"
            + "<if test='uid != null and uid != \"\"'> AND f.uid = #{uid}</if>"
            + "<if test='timeStart != null'> AND f.create_time &gt;= #{timeStart}</if>"
            + "<if test='timeEnd != null'> AND f.create_time &lt;= #{timeEnd}</if>"
            + " ORDER BY f.create_time DESC LIMIT #{page.offset}, #{page.size}"
            + "</script>")
    List<Map<String, Object>> selectFeedbackPage(@Param("page") PageArgsHolder page, @Param("sourceType") Integer sourceType,
                                                 @Param("sampleStatus") Integer sampleStatus, @Param("uid") String uid,
                                                 @Param("timeStart") String timeStart, @Param("timeEnd") String timeEnd);

    @Select("<script>SELECT COUNT(*) FROM tbl_ai_feedback f WHERE 1 = 1"
            + "<if test='sourceType != null'> AND f.source_type = #{sourceType}</if>"
            + "<if test='sampleStatus != null'> AND f.sample_status = #{sampleStatus}</if>"
            + "<if test='uid != null and uid != \"\"'> AND f.uid = #{uid}</if>"
            + "<if test='timeStart != null'> AND f.create_time &gt;= #{timeStart}</if>"
            + "<if test='timeEnd != null'> AND f.create_time &lt;= #{timeEnd}</if>"
            + "</script>")
    Long countFeedback(@Param("sourceType") Integer sourceType, @Param("sampleStatus") Integer sampleStatus,
                       @Param("uid") String uid, @Param("timeStart") String timeStart, @Param("timeEnd") String timeEnd);

    @Select("SELECT f.*, u.username FROM tbl_ai_feedback f LEFT JOIN sys_user u ON u.id = f.uid WHERE f.id = #{id}")
    Map<String, Object> selectFeedbackById(@Param("id") String id);

    @Update("UPDATE tbl_ai_feedback SET sample_status = #{status} WHERE id = #{id}")
    int updateFeedbackStatus(@Param("id") String id, @Param("status") Integer status);

    /** 样本池分页参数（offset/size 与 P2 PageArgs 口径一致） */
    class PageArgsHolder {
        private final long offset;
        private final long size;

        public PageArgsHolder(long pageNo, long pageSize) {
            long size = pageSize <= 0 ? 10 : Math.min(pageSize, 100);
            long no = pageNo <= 0 ? 1 : pageNo;
            this.offset = (no - 1) * size;
            this.size = size;
        }

        public long getOffset() {
            return offset;
        }

        public long getSize() {
            return size;
        }
    }
}
