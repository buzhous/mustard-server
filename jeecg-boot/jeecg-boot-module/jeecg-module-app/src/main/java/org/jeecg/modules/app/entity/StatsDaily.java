package org.jeecg.modules.app.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * F1 轻量自研采集统计 · 日汇总表（设计方案 2.1）
 * 由 StatsAggregateJob 每日 02:00 聚合产出（先删后插，幂等）；看板只读本表，永久保留。
 * metric 字典：dau / new_users / event:{name}（app_launch 为去重设备数）/ funnel_launch / funnel_register / funnel_activate。
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@TableName("tbl_stats_daily")
@Schema(description = "运营统计日汇总")
public class StatsDaily implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "统计日")
    private Date statsDate;

    @Schema(description = "指标名")
    private String metric;

    @Schema(description = "维度细分（source/platform/channel，可为空）")
    private String dimension;

    @Schema(description = "指标值")
    private Long value;
}
