package org.jeecg.modules.app.bean.vo.station;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import org.jeecg.modules.app.bean.vo.storage.UserStorageVO;

import java.io.Serializable;
import java.util.List;

/**
 * 我的仓库点位聚合 VO
 * 规则：虚拟仓固定 1 个；自有真实仓普通用户最多 1 个、VIP 最多 3 个；官方站点绑定普通 1 个、VIP 3 个
 */
@Data
@Accessors(chain = true)
@Schema(description = "我的仓库点位聚合")
public class MyStationsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "虚拟仓库点（固定 1 个）")
    private UserVirtualStationVO virtual;

    @Schema(description = "官方站点绑定列表（主站点在前）")
    private List<UserStationBindVO> real;

    @Schema(description = "自有真实仓列表（tbl_user_storage，按创建时间正序）")
    private List<UserStorageVO> storages;
}
