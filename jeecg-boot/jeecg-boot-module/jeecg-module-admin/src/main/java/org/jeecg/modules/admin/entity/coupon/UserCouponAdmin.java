package org.jeecg.modules.admin.entity.coupon;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户持券（后台管理，映射 tbl_user_coupon，与 App 端 org.jeecg.modules.app.entity.UserCoupon 共表；
 * id/couponId 用 String 承接 BIGINT，避免雪花Id超过 JS 安全整数导致前端精度丢失）
 */
@Data
@TableName("tbl_user_coupon")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(name = "用户持券管理对象", description = "用户持券管理")
public class UserCouponAdmin implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "优惠券ID")
    private String couponId;

    @Schema(description = "优惠券名称")
    private String couponName;

    @Schema(description = "优惠券类型: 1-满减券, 2-折扣券, 3-兑换券")
    private Integer couponType;

    @Schema(description = "折扣金额或折扣率")
    private BigDecimal discountValue;

    @Schema(description = "最低消费金额")
    private BigDecimal minAmount;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "有效期开始时间")
    private Date validStartTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "有效期结束时间")
    private Date validEndTime;

    @Schema(description = "获取方式: 1-主动领取, 2-系统发放, 3-活动奖励")
    private Integer obtainType;

    @Schema(description = "状态: 0-未使用, 1-已使用, 2-已过期, 3-已作废")
    private Integer status;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "使用时间")
    private Date useTime;

    @Schema(description = "订单ID")
    private String orderId;

    @TableField(exist = false)
    @Schema(description = "用户昵称（列表展示用）")
    private String userName;

}
