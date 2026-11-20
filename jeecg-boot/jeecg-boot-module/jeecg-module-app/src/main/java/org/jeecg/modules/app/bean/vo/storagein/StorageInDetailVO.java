package org.jeecg.modules.app.bean.vo.storagein;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;

/**
 * 转入/移库单详情（含快递与收寄双方信息）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Schema(description = "转入/移库单详情")
public class StorageInDetailVO extends StorageInListVO {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "官方仓收件地址(转入官方仓时展示)")
    private String officialAddress;

    @Schema(description = "官方仓联系电话")
    private String officialContactPhone;

    @Schema(description = "寄件人姓名(移库)")
    private String sendName;

    @Schema(description = "寄件人电话(移库)")
    private String sendPhone;

    @Schema(description = "寄件地址(移库)")
    private String sendAddr;

    @Schema(description = "收件人姓名(移库)")
    private String recName;

    @Schema(description = "收件人电话(移库)")
    private String recPhone;

    @Schema(description = "收件地址(移库)")
    private String recAddr;
}
