package org.jeecg.modules.app.bean.vo.points;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "创建充值订单结果")
public class RechargeCreateResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "支付金额(分)")
    private Integer amountCents;

    @Schema(description = "充值积分")
    private Integer points;

    @Schema(description = "支付渠道 10微信 20支付宝")
    private Integer channel;

}
