package org.jeecg.modules.app.bean.vo.station;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 用户专属默认真实仓 VO
 */
@Data
@Accessors(chain = true)
@Schema(description = "用户专属默认真实仓VO")
public class UserRealStationVO implements Serializable {

    @Schema(description = "id")
    private String id;

    @Schema(description = "仓库编号")
    private String stationNo;

    @Schema(description = "用户自定义仓库名")
    private String customName;

    @Schema(description = "收件人")
    private String receiverName;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "省份")
    private String province;

    @Schema(description = "城市")
    private String city;

    @Schema(description = "区县")
    private String district;

    @Schema(description = "详细地址")
    private String address;

    @Schema(description = "经度")
    private BigDecimal longitude;

    @Schema(description = "纬度")
    private BigDecimal latitude;
}
