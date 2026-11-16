package org.jeecg.modules.app.bean.vo.storage;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 创建用户仓库请求
 */
@Data
@Schema(description = "创建用户仓库请求")
public class StorageCreateReqVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "仓库名称")
    private String name;

}
