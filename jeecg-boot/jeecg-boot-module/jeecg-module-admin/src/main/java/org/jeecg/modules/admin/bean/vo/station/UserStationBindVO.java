package org.jeecg.modules.admin.bean.vo.station;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@Schema(description = "用户绑定站点VO")
public class UserStationBindVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "绑定记录Id")
    private String id;

    @Schema(description = "站点Id")
    private String stationId;

    @Schema(description = "是否主站点:0否,1是")
    private Integer isDefault;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "绑定时间")
    private Date bindTime;

    @Schema(description = "站点信息")
    private WarehouseStationVO station;
}
