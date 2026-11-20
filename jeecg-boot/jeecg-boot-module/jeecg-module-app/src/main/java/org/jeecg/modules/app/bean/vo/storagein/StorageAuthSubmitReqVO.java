package org.jeecg.modules.app.bean.vo.storagein;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 自建仓平台认证提交请求
 */
@Data
@Schema(description = "自建仓平台认证提交")
public class StorageAuthSubmitReqVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "仓库Id")
    private String storageId;

    @Schema(description = "营业执照图片URL")
    private String licenseImg;

    @Schema(description = "门头照图片URL")
    private String doorImg;

    @Schema(description = "联系人姓名")
    private String contactName;

    @Schema(description = "联系人电话")
    private String contactPhone;
}
