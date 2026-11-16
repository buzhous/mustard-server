package org.jeecg.modules.admin.entity.coupon;

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
import java.math.BigDecimal;
import java.util.Date;

/**
 * 优惠券模板（后台管理，映射 tbl_coupon，与 App 端 org.jeecg.modules.app.entity.Coupon 共表；
 * id 用 String 承接 BIGINT，避免雪花Id超过 JS 安全整数导致前端精度丢失）
 */
@Data
@TableName("tbl_coupon")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(name = "优惠券模板管理对象", description = "优惠券模板管理")
public class CouponAdmin implements Serializable {

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

    @Schema(description = "优惠券编码（业务固定编码，唯一，如RUINS_PICKUP殷墟拾取券）")
    private String code;

    @Schema(description = "优惠券名称")
    private String couponName;

    @Schema(description = "优惠券类型: 1-满减券, 2-折扣券, 3-兑换券")
    private Integer couponType;

    @Schema(description = "折扣金额或折扣率")
    private BigDecimal discountValue;

    @Schema(description = "最低消费金额")
    private BigDecimal minAmount;

    @Schema(description = "优惠券描述")
    private String description;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "有效期开始时间")
    private Date validStartTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "有效期结束时间")
    private Date validEndTime;

    @Schema(description = "发放总数量")
    private Integer totalCount;

    @Schema(description = "剩余数量")
    private Integer remainCount;

    @Schema(description = "已使用数量")
    private Integer usedCount;

    @Schema(description = "每人限领数量（0不限）")
    private Integer perLimit;

    @Schema(description = "每日限领数量（0/null 不限；拾取券每日领取流程按1兜底）")
    private Integer dailyLimit;

    @Schema(description = "状态: 0-未发布, 1-已发布, 2-已下架")
    private Integer status;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "备注")
    private String remark;

}
