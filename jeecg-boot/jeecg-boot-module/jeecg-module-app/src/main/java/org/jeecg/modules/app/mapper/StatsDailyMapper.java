package org.jeecg.modules.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.app.entity.StatsDaily;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * F1 运营统计 · 日汇总 Mapper
 */
@Mapper
public interface StatsDailyMapper extends BaseMapper<StatsDaily> {

    /** 聚合幂等：先删指定日再插入（事务内），避免双库 upsert 语法差异 */
    @Delete("DELETE FROM tbl_stats_daily WHERE stats_date = #{date}")
    int deleteByDate(@Param("date") Date date);

    /** 趋势：metric（可带 dimension 过滤）近 N 日序列 */
    @Select("<script>SELECT stats_date, SUM(value) AS value FROM tbl_stats_daily WHERE metric = #{metric} "
            + "<if test='dimension != null and dimension != \"\"'> AND dimension = #{dimension} </if>"
            + " AND stats_date BETWEEN #{from} AND #{to} GROUP BY stats_date ORDER BY stats_date</script>")
    List<Map<String, Object>> trend(@Param("metric") String metric, @Param("dimension") String dimension,
                                    @Param("from") Date from, @Param("to") Date to);

    /** 指定日全部指标 */
    @Select("SELECT metric, dimension, value FROM tbl_stats_daily WHERE stats_date = #{date}")
    List<Map<String, Object>> metricsOfDay(@Param("date") Date date);

    /** 近 N 日各事件总量（漏斗/看板汇总用） */
    @Select("SELECT metric, SUM(value) AS value FROM tbl_stats_daily WHERE stats_date BETWEEN #{from} AND #{to} "
            + "AND (metric LIKE 'event:%' OR metric = 'dau' OR metric = 'new_users') GROUP BY metric")
    List<Map<String, Object>> sumBetween(@Param("from") Date from, @Param("to") Date to);

    /** 注册来源分布（聚合时以 dimension=source 落库） */
    @Select("SELECT dimension, SUM(value) AS value FROM tbl_stats_daily WHERE stats_date BETWEEN #{from} AND #{to} "
            + "AND metric = 'register_source' GROUP BY dimension")
    List<Map<String, Object>> selectRegisterSourceSums(@Param("from") Date from, @Param("to") Date to);

    /** 导出全部指标行 */
    @Select("SELECT stats_date, metric, dimension, value FROM tbl_stats_daily WHERE stats_date BETWEEN #{from} AND #{to} "
            + "ORDER BY stats_date, metric")
    List<Map<String, Object>> exportRows(@Param("from") Date from, @Param("to") Date to);
}
