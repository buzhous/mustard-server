package org.jeecg.modules.app.bean.vo.station;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 用户虚拟仓库点 VO
 */
@Data
@Accessors(chain = true)
@Schema(description = "用户虚拟仓库点")
public class UserVirtualStationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "虚拟仓Id")
    private String id;

    @Schema(description = "虚拟仓编号(如 VS10000001)")
    private String stationNo;

    @Schema(description = "位置说明(默认:芥子空间平台 OSS 仓库)")
    private String locationDesc;
}
