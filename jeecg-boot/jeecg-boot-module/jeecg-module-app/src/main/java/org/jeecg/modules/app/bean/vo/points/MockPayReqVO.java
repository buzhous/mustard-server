package org.jeecg.modules.app.bean.vo.points;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


@Data
@Schema(description = "模拟支付回调请求")
public class MockPayReqVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "订单号")
    private String orderNo;

}
