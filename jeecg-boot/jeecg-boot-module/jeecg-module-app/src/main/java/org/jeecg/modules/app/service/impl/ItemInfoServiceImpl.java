package org.jeecg.modules.app.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.app.bean.enums.ExceptionEnum;
import org.jeecg.modules.app.bean.enums.ItemStatusEnum;
import org.jeecg.modules.app.bean.exception.AppException;
import org.jeecg.modules.app.bean.vo.item.ItemInfoCreateVO;
import org.jeecg.modules.app.bean.vo.item.ItemInfoQueryVO;
import org.jeecg.modules.app.constant.StationConstant;
import org.jeecg.modules.app.entity.ItemInfo;
import org.jeecg.modules.app.entity.warehouse.UserStationBind;
import org.jeecg.modules.app.entity.warehouse.UserStorage;
import org.jeecg.modules.app.entity.warehouse.UserVirtualStation;
import org.jeecg.modules.app.mapper.ItemInfoMapper;
import org.jeecg.modules.app.mip.LineageService;
import org.jeecg.modules.app.mip.MipEventType;
import org.jeecg.modules.app.mip.MipWriteSupport;
import org.jeecg.modules.app.service.IItemInfoService;
import org.jeecg.modules.app.service.IUserStationBindService;
import org.jeecg.modules.app.service.IUserStorageService;
import org.jeecg.modules.app.service.IUserVirtualStationService;
import org.jeecg.modules.app.utils.FieldElementUtil;
import org.springframework.stereotype.Service;

import java.util.Map;


@Service
public class ItemInfoServiceImpl extends ServiceImpl<ItemInfoMapper, ItemInfo> implements IItemInfoService {

    private final IUserStationBindService userStationBindService;
    private final IUserVirtualStationService userVirtualStationService;
    private final IUserStorageService userStorageService;
    private final LineageService lineageService;

    public ItemInfoServiceImpl(IUserStationBindService userStationBindService,
                               IUserVirtualStationService userVirtualStationService,
                               IUserStorageService userStorageService,
                               LineageService lineageService) {
        this.userStationBindService = userStationBindService;
        this.userVirtualStationService = userVirtualStationService;
        this.userStorageService = userStorageService;
        this.lineageService = lineageService;
    }

    /**
     * 物品落仓：
     * 1) 客户端显式传 stationId 时校验归属后使用
     * 2) 未指定时自动落入默认仓位：优先用户主站点(is_default=1)真实仓，否则虚拟仓
     */
    private void fillDefaultStation(ItemInfoCreateVO createVO, ItemInfo item) {
        // 显式指定仓位：校验该仓位属于当前用户
        if (ObjectUtil.isNotEmpty(createVO.getStationId()) && createVO.getStationType() != null) {
            validateStationOwnership(createVO.getUserId(), createVO.getStationId(), createVO.getStationType());
            item.setStationId(createVO.getStationId());
            item.setStationType(createVO.getStationType());
            return;
        }
        // 默认落仓：优先主站点
        UserStationBind defaultBind = userStationBindService.getOne(new LambdaQueryWrapper<UserStationBind>()
                .eq(UserStationBind::getUserId, createVO.getUserId())
                .eq(UserStationBind::getIsDefault, StationConstant.IS_DEFAULT_YES)
                .eq(UserStationBind::getStatus, StationConstant.BIND_STATUS_BOUND)
                .last("limit 1"), false);
        if (defaultBind != null) {
            item.setStationId(defaultBind.getStationId());
            item.setStationType(StationConstant.STATION_TYPE_REAL);
            return;
        }
        // 无主站点时落入虚拟仓（不存在则自动初始化）
        UserVirtualStation virtual = userVirtualStationService.ensureUserVirtualStation(createVO.getUserId());
        item.setStationId(virtual.getId());
        item.setStationType(StationConstant.STATION_TYPE_VIRTUAL);
    }

    /**
     * 校验仓位归属：官方绑定仓须为本人已绑定站点，虚拟仓须为本人虚拟仓，自建真实仓须为本人未删除仓库
     */
    private void validateStationOwnership(String userId, String stationId, Integer stationType) {
        if (StationConstant.STATION_TYPE_REAL == stationType) {
            UserStationBind bind = userStationBindService.getOne(new LambdaQueryWrapper<UserStationBind>()
                    .eq(UserStationBind::getUserId, userId)
                    .eq(UserStationBind::getStationId, stationId)
                    .eq(UserStationBind::getStatus, StationConstant.BIND_STATUS_BOUND)
                    .last("limit 1"), false);
            if (ObjectUtil.isEmpty(bind)) {
                throw new AppException(ExceptionEnum.REQUEST_PARAM_ERROR);
            }
        } else if (StationConstant.STATION_TYPE_VIRTUAL == stationType) {
            UserVirtualStation virtual = userVirtualStationService.getById(stationId);
            if (ObjectUtil.isEmpty(virtual) || !userId.equals(virtual.getUserId())) {
                throw new AppException(ExceptionEnum.REQUEST_PARAM_ERROR);
            }
        } else if (StationConstant.STATION_TYPE_STORAGE == stationType) {
            UserStorage storage = userStorageService.getById(stationId);
            // @TableLogic 自动过滤已删除记录
            if (ObjectUtil.isEmpty(storage) || !userId.equals(storage.getUserId())) {
                throw new AppException(ExceptionEnum.REQUEST_PARAM_ERROR);
            }
        } else {
            throw new AppException(ExceptionEnum.REQUEST_PARAM_ERROR);
        }
    }

    @Override
    public ItemInfo queryItemInfo(String itemId) {
        return this.getById(itemId);
    }

    @Override
    public ItemInfo queryItemInfoByOriId(String userId, String oriId) {
        LambdaQueryWrapper<ItemInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ItemInfo::getOriId, oriId);
        queryWrapper.eq(ItemInfo::getUserId, userId);
        queryWrapper.last("limit 1");
        return this.getOne(queryWrapper);
    }

    @Override
    public boolean createItemInfo(ItemInfoCreateVO createVO) {
        if (ObjectUtil.isEmpty(createVO.getUserId()) || ObjectUtil.isEmpty(createVO.getOriId())) {
            throw new AppException(ExceptionEnum.REQUEST_PARAM_ERROR);
        }
        ItemInfo itemInfo = this.queryItemInfoByOriId(createVO.getUserId(), createVO.getOriId());
        if (ObjectUtil.isNotEmpty(itemInfo)) {
            // TODO 存在修改本地恢复风险
            createVO.setItemId(String.valueOf(itemInfo.getId()));
            return true;
        }
        // 解析数据，生成物品
        ItemInfo item = BeanUtil.copyProperties(createVO, ItemInfo.class);
        item.setCreateTime(DateUtil.date());
        item.setUpdateTime(DateUtil.date());
        // MIP 写路径补级：fields 落库前追加 sensitivity（服务端单点计算，幂等）
        item.setFields(MipWriteSupport.enrich(item.getFields()));
        // 物品落仓：未指定仓位时自动落入用户默认仓位（优先主站点，其次虚拟仓）
        this.fillDefaultStation(createVO, item);
        // 创建物品信息
        boolean result = this.saveOrUpdate(item);
        if (result) {
            // MIP 流转账本：建档 mint（链首事件，幂等键含 itemId 防重复建档污染）
            lineageService.append(new org.jeecg.modules.app.entity.ItemLineage()
                    .setOriId(createVO.getOriId())
                    .setEventType(MipEventType.MINT.getCode())
                    .setToUid(LineageService.hashUid(createVO.getUserId()))
                    .setOperator(LineageService.OPERATOR_USER)
                    .setIdempotencyKey("mint:" + item.getId()));
        }
        createVO.setItemId(String.valueOf(item.getId()));
        return result;
    }

    @Override
    public boolean createItemInfoTmp(ItemInfoCreateVO itemInfoCreateVO) {
        // 创建线上物品
        itemInfoCreateVO.setOriId(itemInfoCreateVO.getOriId());
        itemInfoCreateVO.setUserId(itemInfoCreateVO.getUserId());
        itemInfoCreateVO.setStatus(ItemStatusEnum.TEMPORARY.getCode());
        itemInfoCreateVO.setCategory("0");
        return this.createItemInfo(itemInfoCreateVO);
    }

    @Override
    public boolean updateItemInfo(ItemInfoCreateVO createVO) {
        if (ObjectUtil.isEmpty(createVO.getUserId()) || ObjectUtil.isEmpty(createVO.getItemId())) {
            throw new AppException(ExceptionEnum.REQUEST_PARAM_ERROR);
        }
        ItemInfo itemInfoExist = this.queryItemInfo(createVO.getItemId());
        if (ObjectUtil.isEmpty(itemInfoExist)) {
            throw new AppException(ExceptionEnum.USER_INFO_NOT_EXIST);
        }
        // 解析数据，生成物品
        ItemInfo itemInfo = FieldElementUtil.convertToJsonFields(createVO, new ItemInfo());
        itemInfo.setId(itemInfoExist.getId());
        itemInfo.setCreateTime(DateUtil.date());
        itemInfo.setUpdateTime(DateUtil.date());
        // MIP 写路径补级：fields 追加 sensitivity，并按组件值为权威回写 identity 列（E-SYNC）
        itemInfo.setFields(MipWriteSupport.enrich(itemInfo.getFields()));
        MipWriteSupport.IdentitySync sync = MipWriteSupport.computeIdentitySync(itemInfo.getFields());
        if (sync.name() != null) {
            itemInfo.setName(sync.name());
        }
        if (sync.icon() != null) {
            itemInfo.setIcon(sync.icon());
        }
        if (sync.category() != null) {
            itemInfo.setCategory(sync.category());
        }
        if (sync.level() != null) {
            itemInfo.setLevel(sync.level());
        }
        // 状态：客户端显式传入时透传，否则恢复为正常
        itemInfo.setStatus(ObjectUtil.defaultIfNull(createVO.getStatus(), ItemStatusEnum.NORMAL.getCode()));
        // 仓位字段成对更新：仅当 stationId 与 stationType 同时显式指定时才生效，避免半更新
        if (ObjectUtil.isEmpty(itemInfo.getStationId()) || itemInfo.getStationType() == null) {
            itemInfo.setStationId(null);
            itemInfo.setStationType(null);
        } else {
            validateStationOwnership(createVO.getUserId(), itemInfo.getStationId(), itemInfo.getStationType());
        }
        // 更新物品信息
        boolean result = this.updateById(itemInfo);
        createVO.setItemId(String.valueOf(itemInfo.getId()));
        return result;
    }

    @Override
    public Page<ItemInfo> listByCondition(ItemInfoQueryVO queryVO) {
        LambdaQueryWrapper<ItemInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ItemInfo::getUserId, queryVO.getUserId());
        if (ObjectUtil.isNotEmpty(queryVO.getKeyword())) {
            String keyword = queryVO.getKeyword().trim();
            wrapper.and(q -> q.like(ItemInfo::getName, keyword).or().like(ItemInfo::getDescription, keyword));
        }
        if (ObjectUtil.isNotEmpty(queryVO.getStationId())) {
            wrapper.eq(ItemInfo::getStationId, queryVO.getStationId().trim());
        }
        if (queryVO.getStationType() != null) {
            wrapper.eq(ItemInfo::getStationType, queryVO.getStationType());
        }
        if (queryVO.getStatus() != null) {
            wrapper.eq(ItemInfo::getStatus, queryVO.getStatus());
        }
        // 排序字段白名单，防注入
        Map<String, SFunction<ItemInfo, ?>> sortColumns = Map.of(
                "created", ItemInfo::getCreateTime,
                "name", ItemInfo::getName,
                "price", ItemInfo::getPrice,
                "quantity", ItemInfo::getQuantity,
                "status", ItemInfo::getStatus
        );
        SFunction<ItemInfo, ?> sortColumn = sortColumns.getOrDefault(
                ObjectUtil.isEmpty(queryVO.getOrderBy()) ? "created" : queryVO.getOrderBy(),
                ItemInfo::getCreateTime);
        wrapper.orderBy(true, Boolean.TRUE.equals(queryVO.getAscending()), sortColumn);
        return this.page(new Page<>(queryVO.getPageNo(), queryVO.getPageSize()), wrapper);
    }

}
