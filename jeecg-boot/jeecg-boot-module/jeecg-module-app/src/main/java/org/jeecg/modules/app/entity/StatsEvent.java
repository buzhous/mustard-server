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
 * F1 轻量自研采集统计 · 原始事件表（设计方案 2.1）
 * append-only：只追加不更新；eventId 幂等（唯一约束）；按 stats_date 归档（90 天保留）。
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@TableName("tbl_stats_event")
@Schema(description = "运营统计原始事件")
public class StatsEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "客户端幂等键 UUID")
    private String eventId;

    @Schema(description = "事件名（8 基线事件字典内）")
    private String event;

    @Schema(description = "用户 id（匿名启动事件为空）")
    private String uid;

    @Schema(description = "设备假名 id")
    private String deviceId;

    @Schema(description = "平台 ios / android")
    private String platform;

    @Schema(description = "App 版本")
    private String appVersion;

    @Schema(description = "扩展属性 JSON（键白名单）")
    private String props;

    @Schema(description = "客户端时间戳毫秒")
    private Long clientTs;

    @Schema(description = "服务端接收时间戳毫秒")
    private Long serverTs;

    @Schema(description = "统计归属自然日（客户端时区口径）")
    private Date statsDate;
}
