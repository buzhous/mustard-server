package org.jeecg.modules.app.bean.vo.points;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;


@Data
@Schema(description = "积分流水")
public class PointsFlowVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "id")
    private String id;

    @Schema(description = "类型 10充值 20注册赠送 30消费")
    private Integer type;

    @Schema(description = "积分变动")
    private Integer points;

    @Schema(description = "变动后余额")
    private Integer balanceAfter;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "关联单号")
    private String refId;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "时间")
    private Date createTime;

}
