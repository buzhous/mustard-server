package org.jeecg.modules.app.bean.vo.storage;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 编辑用户仓库请求（仓名/收件信息/位置，name 为空保留原名）
 */
@Data
@Schema(description = "编辑用户仓库请求")
public class StorageRenameReqVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "仓库Id")
    private String id;

    @Schema(description = "仓库名称(空则保留原名)")
    private String name;

    @Schema(description = "收件人")
    private String receiverName;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "省")
    private String province;

    @Schema(description = "市")
    private String city;

    @Schema(description = "区")
    private String district;

    @Schema(description = "详细地址")
    private String address;

    @Schema(description = "经度")
    private java.math.BigDecimal longitude;

    @Schema(description = "纬度")
    private java.math.BigDecimal latitude;

}
