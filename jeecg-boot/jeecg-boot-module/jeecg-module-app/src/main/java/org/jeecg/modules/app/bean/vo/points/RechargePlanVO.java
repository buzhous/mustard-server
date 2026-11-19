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
@Schema(description = "充值套餐")
public class RechargePlanVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "套餐Id")
    private String planId;

    @Schema(description = "积分数量")
    private Integer points;

    @Schema(description = "支付金额(分)")
    private Integer amountCents;

    @Schema(description = "角标文案（如：超值），可为空")
    private String tag;

}
