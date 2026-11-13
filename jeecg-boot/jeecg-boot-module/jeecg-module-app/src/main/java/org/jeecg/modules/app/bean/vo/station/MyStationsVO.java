package org.jeecg.modules.app.bean.vo.station;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 我的仓库点位聚合 VO
 * 规则：普通用户固定拥有 1 个虚拟仓库点 + 1 个真实仓库点（绑定站点，槽位不随 VIP 扩容）
 */
@Data
@Accessors(chain = true)
@Schema(description = "我的仓库点位聚合")
public class MyStationsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "虚拟仓库点（固定 1 个）")
    private UserVirtualStationVO virtual;

    @Schema(description = "真实站点绑定列表（主站点在前）")
    private List<UserStationBindVO> real;
}
