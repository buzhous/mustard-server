package org.jeecg.modules.app.bean.vo.vip;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * VIP 购买结果
 */
@Data
@Accessors(chain = true)
@Schema(description = "VIP购买结果")
public class VipPurchaseRespVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "订单Id")
    private String orderId;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "VIP到期时间")
    private Date vipExpireTime;

    @Schema(description = "支付金额(元)")
    private BigDecimal amount;

}
