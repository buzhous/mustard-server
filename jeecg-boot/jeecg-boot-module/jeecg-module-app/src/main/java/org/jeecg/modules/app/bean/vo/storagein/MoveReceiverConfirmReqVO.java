package org.jeecg.modules.app.bean.vo.storagein;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 移库接收方确认请求（确认收件信息并触发快递下单）
 */
@Data
@Schema(description = "移库接收方确认")
public class MoveReceiverConfirmReqVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "移库单Id")
    private String recordId;

    @Schema(description = "接收方落仓Id")
    private String stationId;

    @Schema(description = "接收方落仓类型:1虚拟仓,2官方仓,3自建仓")
    private Integer stationType;
}
