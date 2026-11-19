package org.jeecg.modules.app.bean.vo.storage;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 创建用户仓库请求（界面与真实仓编辑页字段一致）
 */
@Data
@Schema(description = "创建用户仓库请求")
public class StorageCreateReqVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "仓库名称(空则默认「我的仓库」)")
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
