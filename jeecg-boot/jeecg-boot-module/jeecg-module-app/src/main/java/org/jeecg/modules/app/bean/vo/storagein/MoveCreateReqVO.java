package org.jeecg.modules.app.bean.vo.storagein;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 移库发起请求（快递寄送）
 */
@Data
@Schema(description = "移库发起")
public class MoveCreateReqVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "物品Id")
    private String itemId;

    @Schema(description = "接收人用户Id")
    private String receiverUserId;
}
