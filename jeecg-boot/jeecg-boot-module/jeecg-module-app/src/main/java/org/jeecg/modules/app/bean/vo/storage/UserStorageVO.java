package org.jeecg.modules.app.bean.vo.storage;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 用户仓库 VO
 */
@Data
@Accessors(chain = true)
@Schema(description = "用户仓库")
public class UserStorageVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "仓库Id(内置虚拟仓固定-1)")
    private String id;

    @Schema(description = "仓库名称")
    private String name;

    @Schema(description = "仓库类型:VIRTUAL|REAL")
    private String storageType;

    @Schema(description = "是否默认仓:0否,1是")
    private Integer isDefault;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;

}
