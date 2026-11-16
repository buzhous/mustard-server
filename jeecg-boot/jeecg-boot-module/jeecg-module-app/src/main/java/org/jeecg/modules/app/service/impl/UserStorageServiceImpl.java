package org.jeecg.modules.app.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.app.bean.enums.ExceptionEnum;
import org.jeecg.modules.app.bean.exception.AppException;
import org.jeecg.modules.app.bean.vo.storage.UserStorageVO;
import org.jeecg.modules.app.constant.VipStorageConstant;
import org.jeecg.modules.app.entity.warehouse.UserStorage;
import org.jeecg.modules.app.mapper.UserStorageMapper;
import org.jeecg.modules.app.service.IUserStorageService;
import org.jeecg.modules.app.service.IVipOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 用户仓库服务实现
 * 虚拟仓为系统内置（不落库，列表兜底返回 id=-1）；真实个人仓落库 tbl_user_storage
 * 物品归属沿用 ItemInfo/ItemSync 的 stationId/stationType：REAL 仓 stationId=仓库id、stationType=2(真实仓)，
 * 虚拟仓关联沿用既有 UserVirtualStation(stationType=1)，物品表无需改动
 *
 * @author elinx
 */
@Slf4j
@Service
public class UserStorageServiceImpl extends ServiceImpl<UserStorageMapper, UserStorage> implements IUserStorageService {

    @Autowired
    private IVipOrderService vipOrderService;

    @Override
    public List<UserStorageVO> listUserStorages(String userId) {
        List<UserStorageVO> result = new ArrayList<>();
        // 内置系统虚拟仓（该表只存 REAL 类型，虚拟仓固定兜底返回）
        result.add(new UserStorageVO()
                .setId(VipStorageConstant.BUILTIN_VIRTUAL_STORAGE_ID)
                .setName(VipStorageConstant.BUILTIN_VIRTUAL_STORAGE_NAME)
                .setStorageType(VipStorageConstant.STORAGE_TYPE_VIRTUAL)
                .setIsDefault(1));
        List<UserStorage> storages = baseMapper.selectList(new LambdaQueryWrapper<UserStorage>()
                .eq(UserStorage::getUserId, userId)
                .eq(UserStorage::getDeleted, 0)
                .orderByAsc(UserStorage::getCreateTime));
        for (UserStorage storage : storages) {
            result.add(new UserStorageVO()
                    .setId(storage.getId())
                    .setName(storage.getName())
                    .setStorageType(storage.getStorageType())
                    .setIsDefault(storage.getIsDefault())
                    .setCreateTime(storage.getCreateTime()));
        }
        return result;
    }

    @Override
    public UserStorage createStorage(String userId, String name) {
        if (StrUtil.isEmpty(userId) || StrUtil.isEmpty(name)) {
            throw new AppException(ExceptionEnum.REQUEST_PARAM_ERROR);
        }
        long count = baseMapper.selectCount(new LambdaQueryWrapper<UserStorage>()
                .eq(UserStorage::getUserId, userId)
                .eq(UserStorage::getStorageType, VipStorageConstant.STORAGE_TYPE_REAL)
                .eq(UserStorage::getDeleted, 0));
        int limit = vipOrderService.isVip(userId)
                ? VipStorageConstant.STORAGE_LIMIT_VIP : VipStorageConstant.STORAGE_LIMIT_NORMAL;
        if (count >= limit) {
            throw new AppException(ExceptionEnum.STORAGE_LIMIT_EXCEEDED);
        }
        Date now = new Date();
        UserStorage storage = new UserStorage()
                .setUserId(userId)
                .setName(name)
                .setStorageType(VipStorageConstant.STORAGE_TYPE_REAL)
                .setIsDefault(count == 0 ? 1 : 0)
                .setDeleted(0);
        storage.setCreateTime(now);
        storage.setUpdateTime(now);
        baseMapper.insert(storage);
        log.info("创建用户仓库 userId={}, storageId={}, name={}, count={}/{}",
                userId, storage.getId(), name, count + 1, limit);
        return storage;
    }

    @Override
    public boolean renameStorage(String userId, String id, String name) {
        if (StrUtil.isEmpty(userId) || StrUtil.isEmpty(id) || StrUtil.isEmpty(name)) {
            throw new AppException(ExceptionEnum.REQUEST_PARAM_ERROR);
        }
        UserStorage storage = getOwnedRealStorage(userId, id);
        storage.setName(name);
        storage.setUpdateTime(new Date());
        return baseMapper.updateById(storage) > 0;
    }

    @Override
    public boolean deleteStorage(String userId, String id) {
        if (StrUtil.isEmpty(userId) || StrUtil.isEmpty(id)) {
            throw new AppException(ExceptionEnum.REQUEST_PARAM_ERROR);
        }
        getOwnedRealStorage(userId, id);
        // 软删：deleted=1（@TableLogic 自动转 update）
        return baseMapper.deleteById(id) > 0;
    }

    /**
     * 校验并返回本人名下的 REAL 类型仓库（虚拟仓 id=-1 不支持改名/删除）
     */
    private UserStorage getOwnedRealStorage(String userId, String id) {
        if (VipStorageConstant.BUILTIN_VIRTUAL_STORAGE_ID.equals(id)) {
            throw new AppException(ExceptionEnum.STORAGE_NOT_REAL);
        }
        UserStorage storage = baseMapper.selectById(id);
        if (storage == null || !userId.equals(storage.getUserId()) || storage.getDeleted() != null && storage.getDeleted() == 1) {
            throw new AppException(ExceptionEnum.STORAGE_NOT_EXIST);
        }
        if (!VipStorageConstant.STORAGE_TYPE_REAL.equals(storage.getStorageType())) {
            throw new AppException(ExceptionEnum.STORAGE_NOT_REAL);
        }
        return storage;
    }

}
