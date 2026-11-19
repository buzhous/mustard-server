package org.jeecg.modules.app.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;


@Data
@TableName("tbl_recharge_order")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(name = "tbl_recharge_order对象", description = "充值订单")
public class RechargeOrder implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 待支付 */
    public static final int STATUS_PENDING = 0;
    /** 支付成功 */
    public static final int STATUS_SUCCESS = 1;
    /** 支付失败 */
    public static final int STATUS_FAILED = 2;

    /** 微信支付 */
    public static final int CHANNEL_WECHAT = 10;
    /** 支付宝 */
    public static final int CHANNEL_ALIPAY = 20;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "id")
    private String id;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "修改时间")
    private Date updateTime;

    @Schema(description = "用户Id")
    private String userId;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "支付金额(分)")
    private Integer amountCents;

    @Schema(description = "充值积分")
    private Integer points;

    @Schema(description = "支付渠道 10微信 20支付宝")
    private Integer channel;

    @Schema(description = "状态 0待支付 1成功 2失败")
    private Integer status;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "支付时间")
    private Date payTime;

}
