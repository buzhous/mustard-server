package org.jeecg.modules.app.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.app.bean.vo.storage.UserStorageVO;
import org.jeecg.modules.app.entity.warehouse.UserStorage;

import java.util.List;

/**
 * 用户仓库服务（虚拟仓 + 真实仓）
 *
 * @author elinx
 */
public interface IUserStorageService extends IService<UserStorage> {

    /**
     * 确保用户拥有默认真实仓（注册随账号创建 1 条，幂等；不可删除）
     *
     * @param userId 用户Id
     */
    UserStorage ensureDefaultStorage(String userId);

    /**
     * 当前用户仓库列表（头部固定含系统虚拟仓，无记录时返回内置虚拟仓 id=-1）
     *
     * @param userId 用户Id
     */
    List<UserStorageVO> listUserStorages(String userId);

    /**
     * 当前用户真实仓列表（不含系统虚拟仓，按创建时间正序）
     *
     * @param userId 用户Id
     */
    List<UserStorageVO> listRealStorages(String userId);

    /**
     * 创建真实仓（普通用户上限 1 个，VIP 上限 3 个；字段与真实仓编辑页一致）
     *
     * @param userId  用户Id
     * @param storage 仓库信息（仓名/收件人/电话/省市区/详细地址/经纬度）
     */
    UserStorage createStorage(String userId, UserStorage storage);

    /**
     * 编辑仓库（仓名为空保留原名，其余字段全量更新；仅 REAL 类型）
     *
     * @param userId  用户Id
     * @param storage 仓库信息（id 必填）
     */
    boolean updateStorage(String userId, UserStorage storage);

    /**
     * 删除仓库（软删，仅 REAL 类型）
     *
     * @param userId 用户Id
     * @param id     仓库Id
     */
    boolean deleteStorage(String userId, String id);

}
