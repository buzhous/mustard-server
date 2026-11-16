package org.jeecg.modules.app.bean.vo.storage;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 删除用户仓库请求
 */
@Data
@Schema(description = "删除用户仓库请求")
public class StorageDeleteReqVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "仓库Id")
    private String id;

}
