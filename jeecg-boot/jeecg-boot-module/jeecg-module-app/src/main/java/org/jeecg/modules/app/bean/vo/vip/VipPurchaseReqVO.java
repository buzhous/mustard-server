package org.jeecg.modules.app.bean.vo.vip;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * VIP 购买请求
 */
@Data
@Schema(description = "VIP购买请求")
public class VipPurchaseReqVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "支付渠道:WECHAT|ALIPAY")
    private String payChannel;

    @Schema(description = "是否模拟支付,默认true(真实支付渠道接入前固定走模拟)")
    private Boolean mock;

}
