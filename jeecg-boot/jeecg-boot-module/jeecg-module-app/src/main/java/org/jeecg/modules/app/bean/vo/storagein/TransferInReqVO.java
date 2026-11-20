package org.jeecg.modules.app.bean.vo.storagein;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 物品转入请求
 */
@Data
@Schema(description = "物品转入")
public class TransferInReqVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "物品Id")
    private String itemId;

    @Schema(description = "物品原始Id")
    private String oriId;

    @Schema(description = "目标仓Id")
    private String targetStationId;

    @Schema(description = "目标仓类型:2官方仓,3自建仓")
    private Integer targetType;
}
