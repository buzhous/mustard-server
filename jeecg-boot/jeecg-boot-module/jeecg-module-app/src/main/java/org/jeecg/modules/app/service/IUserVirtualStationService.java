package org.jeecg.modules.app.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.app.entity.warehouse.UserVirtualStation;

/**
 * 实物云仓-用户虚拟仓库点服务
 *
 * @author elinx
 */
public interface IUserVirtualStationService extends IService<UserVirtualStation> {

    /**
     * 获取用户虚拟仓库点（不存在则自动初始化，保证每用户固定 1 个虚拟仓）
     *
     * @param userId 用户Id
     * @return 虚拟仓库点
     */
    UserVirtualStation ensureUserVirtualStation(String userId);

    /**
     * 查询用户虚拟仓库点（不初始化）
     *
     * @param userId 用户Id
     * @return 虚拟仓库点，可能为 null
     */
    UserVirtualStation queryByUserId(String userId);

    /**
     * 更新虚拟仓库点位置说明
     *
     * @param userId       用户Id
     * @param locationDesc 位置说明
     * @return 是否成功
     */
    boolean updateLocationDesc(String userId, String locationDesc);
}
