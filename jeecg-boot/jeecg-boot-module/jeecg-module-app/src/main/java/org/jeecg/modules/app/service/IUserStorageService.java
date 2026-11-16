package org.jeecg.modules.app.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.app.bean.vo.storage.UserStorageVO;
import org.jeecg.modules.app.entity.warehouse.UserStorage;

import java.util.List;

/**
 * 用户仓库服务（虚拟仓 + 真实个人仓）
 *
 * @author elinx
 */
public interface IUserStorageService extends IService<UserStorage> {

    /**
     * 当前用户仓库列表（头部固定含系统虚拟仓，无记录时返回内置虚拟仓 id=-1）
     *
     * @param userId 用户Id
     */
    List<UserStorageVO> listUserStorages(String userId);

    /**
     * 创建真实个人仓（普通用户上限 1 个，VIP 上限 3 个）
     *
     * @param userId 用户Id
     * @param name   仓库名称
     */
    UserStorage createStorage(String userId, String name);

    /**
     * 重命名仓库（仅 REAL 类型）
     *
     * @param userId 用户Id
     * @param id     仓库Id
     * @param name   新名称
     */
    boolean renameStorage(String userId, String id, String name);

    /**
     * 删除仓库（软删，仅 REAL 类型）
     *
     * @param userId 用户Id
     * @param id     仓库Id
     */
    boolean deleteStorage(String userId, String id);

}
