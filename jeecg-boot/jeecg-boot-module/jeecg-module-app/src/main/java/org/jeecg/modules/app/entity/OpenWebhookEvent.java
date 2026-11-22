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
 * 开放平台 Webhook 投递事件（B4）
 * 账本 append 钩子命中订阅者时入队（与业务同事务），投递器异步回调并记录投递结果
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@TableName("tbl_open_webhook_event")
@Schema(description = "开放平台 Webhook 投递事件")
public class OpenWebhookEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_DELIVERED = "DELIVERED";
    public static final String STATUS_DEAD = "DEAD";

    /** 雪花ID以字符串出参（防 JS 精度丢失） */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "投递ID（字符串）")
    private String id;

    @Schema(description = "订阅方 AppKey")
    private String appKey;

    @Schema(description = "事件：item.changed / item.flowed")
    private String event;

    @Schema(description = "账本操作：mint/update/recycle/gift_out/...")
    private String opType;

    @Schema(description = "物品档案ID")
    private String oriId;

    @Schema(description = "投递报文（出域最小集，无 uid 与属性值）")
    private String payload;

    @Schema(description = "状态：PENDING/DELIVERED/DEAD")
    private String status;

    @Schema(description = "已重试次数")
    private Integer retryCount;

    @Schema(description = "下次投递时间")
    private Date nextRetryAt;

    @Schema(description = "最近响应码")
    private Integer lastCode;

    @Schema(description = "最近失败摘要")
    private String lastError;

    @Schema(description = "最终投递成功时间")
    private Date deliveredAt;

    @Schema(description = "事件时间")
    private Date createTime;

    @Schema(description = "更新时间")
    private Date updateTime;
}
