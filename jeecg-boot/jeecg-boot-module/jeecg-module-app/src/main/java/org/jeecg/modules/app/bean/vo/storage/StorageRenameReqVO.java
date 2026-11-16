package org.jeecg.modules.app.bean.vo.storage;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 重命名用户仓库请求
 */
@Data
@Schema(description = "重命名用户仓库请求")
public class StorageRenameReqVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "仓库Id")
    private String id;

    @Schema(description = "仓库名称")
    private String name;

}
