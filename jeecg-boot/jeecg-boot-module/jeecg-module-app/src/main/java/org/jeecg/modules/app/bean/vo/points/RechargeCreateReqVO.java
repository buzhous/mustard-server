package org.jeecg.modules.app.bean.vo.points;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


@Data
@Schema(description = "创建充值订单请求")
public class RechargeCreateReqVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "套餐Id")
    private String planId;

    @Schema(description = "支付渠道 10微信 20支付宝")
    private Integer channel;

}
