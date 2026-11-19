package org.jeecg.modules.app.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.app.bean.enums.ExceptionEnum;
import org.jeecg.modules.app.bean.exception.AppException;
import org.jeecg.modules.app.bean.vo.storage.UserStorageVO;
import org.jeecg.modules.app.constant.StationConstant;
import org.jeecg.modules.app.constant.VipStorageConstant;
import org.jeecg.modules.app.entity.ItemInfo;
import org.jeecg.modules.app.entity.warehouse.UserStorage;
import org.jeecg.modules.app.mapper.ItemInfoMapper;
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
 * 虚拟仓为系统内置（不落库，列表兜底返回 id=-1）；用户真实仓落库 tbl_user_storage，
 * 注册默认创建 1 条（不可删），VIP 可继续新增至 3 条（普通 1 / VIP 3，含默认仓）
 * 物品归属沿用 ItemInfo/ItemSync 的 stationId/stationType：真实仓 stationId=仓库id、stationType=3(STATION_TYPE_STORAGE)，
 * 虚拟仓关联沿用既有 UserVirtualStation(stationType=1)，官方绑定站点仓 stationType=2，物品表无需改动
 *
 * @author elinx
 */
@Slf4j
@Service
public class UserStorageServiceImpl extends ServiceImpl<UserStorageMapper, UserStorage> implements IUserStorageService {

    /**
     * 默认真实仓仓名（注册赠送的仓库与新增仓的默认名）
     */
    private static final String DEFAULT_REAL_STORAGE_NAME = "我的真实仓";

    @Autowired
    private IVipOrderService vipOrderService;

    // 直接注入 mapper 统计物品数，避免 IItemInfoService 服务间循环依赖
    @Autowired
    private ItemInfoMapper itemInfoMapper;

    @Override
    public List<UserStorageVO> listUserStorages(String userId) {
        List<UserStorageVO> result = new ArrayList<>();
        // 内置系统虚拟仓（该表只存 REAL 类型，虚拟仓固定兜底返回）
        result.add(new UserStorageVO()
                .setId(VipStorageConstant.BUILTIN_VIRTUAL_STORAGE_ID)
                .setName(VipStorageConstant.BUILTIN_VIRTUAL_STORAGE_NAME)
                .setStorageType(VipStorageConstant.STORAGE_TYPE_VIRTUAL)
                .setIsDefault(1));
        result.addAll(listRealStorages(userId));
        return result;
    }

    @Override
    public List<UserStorageVO> listRealStorages(String userId) {
        List<UserStorage> storages = baseMapper.selectList(new LambdaQueryWrapper<UserStorage>()
                .eq(UserStorage::getUserId, userId)
                .eq(UserStorage::getStorageType, VipStorageConstant.STORAGE_TYPE_REAL)
                .eq(UserStorage::getDeleted, 0)
                .orderByAsc(UserStorage::getCreateTime));
        List<UserStorageVO> result = new ArrayList<>();
        for (UserStorage storage : storages) {
            result.add(BeanUtil.copyProperties(storage, UserStorageVO.class));
        }
        return result;
    }

    @Override
    public UserStorage ensureDefaultStorage(String userId) {
        // 真实仓默认随账号创建：已有则直接返回最早一条（幂等），无则补建（VIP/普通用户规则一致）
        UserStorage exist = baseMapper.selectOne(new LambdaQueryWrapper<UserStorage>()
                .eq(UserStorage::getUserId, userId)
                .eq(UserStorage::getStorageType, VipStorageConstant.STORAGE_TYPE_REAL)
                .eq(UserStorage::getDeleted, 0)
                .orderByAsc(UserStorage::getCreateTime)
                .last("limit 1"));
        if (exist != null) {
            return exist;
        }
        Date now = new Date();
        // 预生成 id 以构造仓库编号（RS + 仓库Id 末 6 位），ASSIGN_ID 对已有值不重复生成
        UserStorage storage = new UserStorage().setId(IdWorker.getIdStr());
        storage.setStationNo(StationConstant.buildStorageStationNo(storage.getId()))
                .setUserId(userId)
                .setName(DEFAULT_REAL_STORAGE_NAME)
                .setStorageType(VipStorageConstant.STORAGE_TYPE_REAL)
                .setIsDefault(1)
                .setDeleted(0);
        storage.setCreateTime(now);
        storage.setUpdateTime(now);
        baseMapper.insert(storage);
        log.info("初始化用户默认真实仓 userId={}, storageId={}, stationNo={}",
                userId, storage.getId(), storage.getStationNo());
        return storage;
    }

    @Override
    public UserStorage createStorage(String userId, UserStorage storage) {
        if (StrUtil.isEmpty(userId) || storage == null || StrUtil.isEmpty(storage.getName())) {
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
        // 预生成 id 以构造仓库编号（US + 仓库Id 末 6 位），ASSIGN_ID 对已有值不重复生成
        storage.setId(IdWorker.getIdStr())
                .setStationNo(StationConstant.buildStorageStationNo(storage.getId()))
                .setUserId(userId)
                .setName(storage.getName().trim())
                .setStorageType(VipStorageConstant.STORAGE_TYPE_REAL)
                .setIsDefault(count == 0 ? 1 : 0)
                .setDeleted(0);
        storage.setCreateTime(now);
        storage.setUpdateTime(now);
        baseMapper.insert(storage);
        log.info("创建用户仓库 userId={}, storageId={}, name={}, count={}/{}",
                userId, storage.getId(), storage.getName(), count + 1, limit);
        return storage;
    }

    @Override
    public boolean updateStorage(String userId, UserStorage storage) {
        if (StrUtil.isEmpty(userId) || storage == null || StrUtil.isEmpty(storage.getId())) {
            throw new AppException(ExceptionEnum.REQUEST_PARAM_ERROR);
        }
        UserStorage exist = getOwnedRealStorage(userId, storage.getId());
        // 仓名传空则保留原名；其余字段按编辑页提交值更新
        if (StrUtil.isEmpty(storage.getName())) {
            storage.setName(exist.getName());
        }
        storage.setUserId(exist.getUserId());
        storage.setStorageType(exist.getStorageType());
        storage.setIsDefault(exist.getIsDefault());
        storage.setDeleted(exist.getDeleted());
        storage.setCreateTime(exist.getCreateTime());
        storage.setUpdateTime(new Date());
        return baseMapper.updateById(storage) > 0;
    }

    @Override
    public boolean deleteStorage(String userId, String id) {
        if (StrUtil.isEmpty(userId) || StrUtil.isEmpty(id)) {
            throw new AppException(ExceptionEnum.REQUEST_PARAM_ERROR);
        }
        UserStorage storage = getOwnedRealStorage(userId, id);
        // 仓库下仍有物品时不可删除（移出所有物品后才能删除）
        long itemCount = itemInfoMapper.selectCount(new LambdaQueryWrapper<ItemInfo>()
                .eq(ItemInfo::getUserId, userId)
                .eq(ItemInfo::getStationId, id)
                .eq(ItemInfo::getStationType, StationConstant.STATION_TYPE_STORAGE));
        if (itemCount > 0) {
            throw new AppException(ExceptionEnum.STORAGE_NOT_EMPTY);
        }
        // 软删：deleted=1（@TableLogic 自动转 update）
        return baseMapper.deleteById(id) > 0;
    }

    /**
     * 校验并返回本人名下的 REAL 类型仓库（虚拟仓 id=-1 不支持编辑/删除）
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
