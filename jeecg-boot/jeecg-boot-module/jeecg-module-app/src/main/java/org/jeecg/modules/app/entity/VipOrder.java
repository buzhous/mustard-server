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
import java.math.BigDecimal;
import java.util.Date;

/**
 * VIP 购买订单
 */
@Data
@TableName("tbl_vip_order")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(name = "VIP订单表", description = "VIP订单表")
public class VipOrder implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "id")
    private String id;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "用户Id")
    private String userId;

    @Schema(description = "支付金额(元)")
    private BigDecimal amount;

    @Schema(description = "购买年数")
    private Integer years;

    @Schema(description = "支付渠道:WECHAT|ALIPAY")
    private String payChannel;

    @Schema(description = "支付状态:PAID")
    private String payStatus;

    @Schema(description = "是否模拟支付")
    private Boolean mock;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "支付时间")
    private Date payTime;

}
