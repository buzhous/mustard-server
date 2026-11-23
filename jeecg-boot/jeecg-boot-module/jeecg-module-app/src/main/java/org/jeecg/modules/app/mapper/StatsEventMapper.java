package org.jeecg.modules.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.app.entity.StatsEvent;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * F1 运营统计 · 原始事件 Mapper
 * 聚合 SQL 仅用基础语法（GROUP BY / COUNT DISTINCT / COALESCE / JOIN），双库（PG / Kingbase）兼容。
 */
@Mapper
public interface StatsEventMapper extends BaseMapper<StatsEvent> {

    /** DAU：distinct uid，匿名事件回退 device_id */
    @Select("SELECT COUNT(DISTINCT COALESCE(NULLIF(uid,''), device_id)) FROM tbl_stats_event WHERE stats_date = #{date}")
    Long countDau(@Param("date") Date date);

    /** 按事件计数：app_launch 按设备去重，其余按条数 */
    @Select("SELECT event, COUNT(*) AS cnt FROM tbl_stats_event WHERE stats_date = #{date} AND event != 'app_launch' GROUP BY event")
    List<Map<String, Object>> countGroupByEvent(@Param("date") Date date);

    @Select("SELECT COUNT(DISTINCT device_id) FROM tbl_stats_event WHERE stats_date = #{date} AND event = 'app_launch'")
    Long countLaunchDevices(@Param("date") Date date);

    /** 新增注册：register 事件 distinct uid */
    @Select("SELECT COUNT(DISTINCT uid) FROM tbl_stats_event WHERE stats_date = #{date} AND event = 'register' AND uid IS NOT NULL AND uid != ''")
    Long countNewUsers(@Param("date") Date date);

    /** 注册来源分布 */
    @Select("SELECT props AS dim, COUNT(*) AS cnt FROM tbl_stats_event WHERE stats_date = #{date} AND event = 'register' GROUP BY props")
    List<Map<String, Object>> countRegisterBySource(@Param("date") Date date);

    /** 注册来源分布（聚合用：按 props 细分，job 落库 dimension=register_source:*） */
    @Select("SELECT props, COUNT(*) AS cnt FROM tbl_stats_event WHERE stats_date = #{date} AND event = 'register' GROUP BY props")
    List<Map<String, Object>> registerSourceOfDay(@Param("date") Date date);

    /**
     * 留存明细（双库通用，offset 由 Java 侧按天差计算）：
     * cohort 日注册用户在后续某日的活跃人数
     */
    @Select("SELECT a.stats_date AS cohort_date, b.stats_date AS active_date, "
            + "COUNT(DISTINCT b.uid) AS users "
            + "FROM (SELECT DISTINCT uid, stats_date FROM tbl_stats_event WHERE event = 'register' AND uid IS NOT NULL AND uid != '' "
            + "      AND stats_date BETWEEN #{from} AND #{to}) a "
            + "JOIN tbl_stats_event b ON b.uid = a.uid AND b.stats_date > a.stats_date "
            + "GROUP BY a.stats_date, b.stats_date "
            + "ORDER BY a.stats_date, b.stats_date")
    List<Map<String, Object>> retentionMatrix(@Param("from") Date from, @Param("to") Date to);

    /** 待归档截止日之前的原始数据（分批读取） */
    @Select("SELECT * FROM tbl_stats_event WHERE server_ts < #{beforeTs} ORDER BY id LIMIT #{limit} OFFSET #{offset}")
    List<StatsEvent> selectArchiveBatch(@Param("beforeTs") long beforeTs, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM tbl_stats_event WHERE server_ts < #{beforeTs}")
    long countArchive(@Param("beforeTs") long beforeTs);
}
