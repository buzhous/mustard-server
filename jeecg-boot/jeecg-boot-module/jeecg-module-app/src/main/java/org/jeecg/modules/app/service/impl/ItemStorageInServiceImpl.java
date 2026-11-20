package org.jeecg.modules.app.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.app.bean.enums.ExceptionEnum;
import org.jeecg.modules.app.bean.enums.SyncStatusEnum;
import org.jeecg.modules.app.bean.exception.AppException;
import org.jeecg.modules.app.bean.vo.storagein.MoveCreateReqVO;
import org.jeecg.modules.app.bean.vo.storagein.MoveReceiverConfirmReqVO;
import org.jeecg.modules.app.bean.vo.storagein.StorageAuthSubmitReqVO;
import org.jeecg.modules.app.bean.vo.storagein.StorageInDetailVO;
import org.jeecg.modules.app.bean.vo.storagein.StorageInListVO;
import org.jeecg.modules.app.bean.vo.storagein.TransferInReqVO;
import org.jeecg.modules.app.constant.StationConstant;
import org.jeecg.modules.app.constant.StorageInConstant;
import org.jeecg.modules.app.entity.ItemInfo;
import org.jeecg.modules.app.entity.ItemStorageIn;
import org.jeecg.modules.app.entity.ItemSync;
import org.jeecg.modules.app.entity.ItemUserInventory;
import org.jeecg.modules.app.entity.KuaidiOrder;
import org.jeecg.modules.app.entity.user.AppUser;
import org.jeecg.modules.app.entity.warehouse.UserStorage;
import org.jeecg.modules.app.entity.warehouse.UserVirtualStation;
import org.jeecg.modules.app.entity.warehouse.WarehouseStation;
import org.jeecg.modules.app.mapper.AppUserMapper;
import org.jeecg.modules.app.mapper.ItemStorageInMapper;
import org.jeecg.modules.app.mapper.UserStorageMapper;
import org.jeecg.modules.app.mapper.UserVirtualStationMapper;
import org.jeecg.modules.app.mapper.WarehouseStationMapper;
import org.jeecg.modules.app.service.IItemInfoService;
import org.jeecg.modules.app.service.IItemStorageInService;
import org.jeecg.modules.app.service.IItemSyncService;
import org.jeecg.modules.app.service.IItemUserInventoryService;
import org.jeecg.modules.app.service.Kuaidi100Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 物品转入/移库 + 自建仓认证实现
 *
 * @author elinx
 */
@Slf4j
@Service
public class ItemStorageInServiceImpl extends ServiceImpl<ItemStorageInMapper, ItemStorageIn>
        implements IItemStorageInService {

    @Resource
    private UserStorageMapper userStorageMapper;
    @Resource
    private WarehouseStationMapper warehouseStationMapper;
    @Resource
    private UserVirtualStationMapper userVirtualStationMapper;
    @Resource
    private AppUserMapper appUserMapper;
    @Autowired
    @Lazy
    private IItemSyncService itemSyncService;
    @Autowired
    @Lazy
    private IItemUserInventoryService itemUserInventoryService;
    @Autowired
    @Lazy
    private IItemInfoService itemInfoService;
    @Autowired
    private Kuaidi100Service kuaidi100Service;

    // ==================== 自建仓认证 ====================

    @Override
    public void submitAuth(String userId, StorageAuthSubmitReqVO reqVO) {
        if (reqVO == null || StrUtil.isBlank(reqVO.getStorageId())) {
            throw new AppException(ExceptionEnum.STORAGE_NOT_EXIST);
        }
        UserStorage storage = userStorageMapper.selectById(reqVO.getStorageId());
        if (storage == null || !Objects.equals(storage.getUserId(), userId)) {
            throw new AppException(ExceptionEnum.STORAGE_AUTH_NOT_REAL);
        }
        if (storage.getAuthStatus() != null && storage.getAuthStatus() == StorageInConstant.AUTH_PENDING) {
            throw new AppException(ExceptionEnum.STORAGE_AUTH_PENDING);
        }
        if (storage.getAuthStatus() != null && storage.getAuthStatus() == StorageInConstant.AUTH_APPROVED) {
            throw new AppException(ExceptionEnum.STORAGE_AUTH_APPROVED_ALREADY);
        }
        if (StrUtil.hasBlank(reqVO.getLicenseImg(), reqVO.getDoorImg(),
                reqVO.getContactName(), reqVO.getContactPhone())) {
            throw new AppException(ExceptionEnum.STORAGE_AUTH_MATERIAL_EMPTY);
        }
        Map<String, Object> materials = new HashMap<>();
        materials.put("licenseImg", reqVO.getLicenseImg());
        materials.put("doorImg", reqVO.getDoorImg());
        materials.put("contactName", reqVO.getContactName());
        materials.put("contactPhone", reqVO.getContactPhone());
        UserStorage update = new UserStorage()
                .setId(storage.getId())
                .setAuthStatus(StorageInConstant.AUTH_PENDING)
                .setAuthMaterials(JSONUtil.toJsonStr(materials))
                .setAuthApplyTime(new Date());
        userStorageMapper.updateById(update);
    }

    @Override
    public UserStorage queryAuthStatus(String userId, String storageId) {
        UserStorage storage = userStorageMapper.selectById(storageId);
        if (storage == null || !Objects.equals(storage.getUserId(), userId)) {
            throw new AppException(ExceptionEnum.STORAGE_AUTH_NOT_REAL);
        }
        return storage;
    }

    // ==================== 转入 ====================

    @Override
    public ItemStorageIn transferIn(String userId, TransferInReqVO reqVO) {
        if (reqVO == null || StrUtil.hasBlank(reqVO.getItemId(), reqVO.getTargetStationId())) {
            throw new AppException(ExceptionEnum.REQUEST_PARAM_ERROR);
        }
        if (isItemLocked(reqVO.getItemId())) {
            throw new AppException(ExceptionEnum.ITEM_IN_TRANSIT);
        }
        ItemSync itemSync = itemSyncService.getItemSyncByOriIdAndUserId(reqVO.getOriId(), userId);
        if (itemSync == null || !Objects.equals(itemSync.getItemId(), reqVO.getItemId())
                || (itemSync.getStatus() != null && itemSync.getStatus() != SyncStatusEnum.SUCCESS.getCode())) {
            throw new AppException(ExceptionEnum.ITEM_NOT_EXIST);
        }
        ItemUserInventory inventory = itemUserInventoryService.queryUserItem(userId, reqVO.getItemId());
        if (inventory == null || inventory.getQuantity() == null || inventory.getQuantity() <= 0) {
            throw new AppException(ExceptionEnum.INSUFFICIENT_QUANTITY);
        }

        ItemStorageIn record = new ItemStorageIn()
                .setUserId(userId)
                .setItemId(reqVO.getItemId())
                .setOriId(itemSync.getOriId())
                .setInventoryId(inventory.getId())
                .setQuantity(inventory.getQuantity())
                .setBizType(StorageInConstant.BIZ_TRANSFER_IN)
                .setCreateTime(new Date())
                .setUpdateTime(new Date());
        int targetType = reqVO.getTargetType() == null ? 3 : reqVO.getTargetType();
        if (targetType == StationConstant.STATION_TYPE_STORAGE) {
            // 自建仓：需已认证
            UserStorage storage = userStorageMapper.selectById(reqVO.getTargetStationId());
            if (storage == null || !Objects.equals(storage.getUserId(), userId)) {
                throw new AppException(ExceptionEnum.STORAGE_NOT_EXIST);
            }
            if (storage.getAuthStatus() == null || storage.getAuthStatus() != StorageInConstant.AUTH_APPROVED) {
                throw new AppException(ExceptionEnum.STORAGE_NOT_CERTIFIED);
            }
            record.setStationId(storage.getId())
                    .setSource(StorageInConstant.SOURCE_STORAGE)
                    .setStatus(StorageInConstant.ST_WAIT_CONFIRM);
        } else {
            // 官方仓
            WarehouseStation station = warehouseStationMapper.selectById(reqVO.getTargetStationId());
            if (station == null || station.getStatus() == null
                    || station.getStatus() != StationConstant.STATION_STATUS_OPEN) {
                throw new AppException(ExceptionEnum.STATION_NOT_AVAILABLE);
            }
            record.setStationId(station.getId())
                    .setSource(StorageInConstant.SOURCE_OFFICIAL)
                    .setStatus(StorageInConstant.ST_WAIT_MAIL);
        }
        this.save(record);
        record.setDepositNo(StorageInConstant.buildDepositNo(record.getId()));
        this.updateById(record);
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ItemStorageIn confirmSelf(String userId, String recordId) {
        ItemStorageIn record = getById(recordId);
        if (record == null) {
            throw new AppException(ExceptionEnum.STORAGE_IN_NOT_EXIST);
        }
        if (!Objects.equals(record.getUserId(), userId)) {
            throw new AppException(ExceptionEnum.STORAGE_IN_NOT_OWNER);
        }
        if (record.getBizType() != StorageInConstant.BIZ_TRANSFER_IN
                || record.getSource() != StorageInConstant.SOURCE_STORAGE
                || record.getStatus() != StorageInConstant.ST_WAIT_CONFIRM) {
            throw new AppException(ExceptionEnum.STORAGE_IN_STATUS_ERROR);
        }
        record.setStatus(StorageInConstant.ST_STORED)
                .setConfirmUserId(userId)
                .setConfirmTime(new Date())
                .setUpdateTime(new Date());
        this.updateById(record);
        // 物品落自建仓
        moveItemStation(record.getItemId(), record.getStationId(), 3);
        return record;
    }

    @Override
    public ItemStorageIn expressBack(String userId, String recordId, String expressNo) {
        if (StrUtil.isBlank(expressNo)) {
            throw new AppException(ExceptionEnum.REQUEST_PARAM_ERROR);
        }
        ItemStorageIn record = getById(recordId);
        if (record == null) {
            throw new AppException(ExceptionEnum.STORAGE_IN_NOT_EXIST);
        }
        if (!Objects.equals(record.getUserId(), userId)) {
            throw new AppException(ExceptionEnum.STORAGE_IN_NOT_OWNER);
        }
        if (record.getBizType() != StorageInConstant.BIZ_TRANSFER_IN
                || record.getSource() != StorageInConstant.SOURCE_OFFICIAL
                || record.getStatus() != StorageInConstant.ST_WAIT_MAIL) {
            throw new AppException(ExceptionEnum.STORAGE_IN_STATUS_ERROR);
        }
        record.setStatus(StorageInConstant.ST_MAILED)
                .setExpressNo(expressNo)
                .setUpdateTime(new Date());
        this.updateById(record);
        return record;
    }

    @Override
    public ItemStorageIn cancel(String userId, String recordId) {
        ItemStorageIn record = getById(recordId);
        if (record == null) {
            throw new AppException(ExceptionEnum.STORAGE_IN_NOT_EXIST);
        }
        if (!Objects.equals(record.getUserId(), userId)) {
            throw new AppException(ExceptionEnum.STORAGE_IN_NOT_OWNER);
        }
        if (record.getBizType() == StorageInConstant.BIZ_MOVE) {
            // 发运单归属已随赠送领取转移给接收方，不可取消（须完成寄送）
            throw new AppException(ExceptionEnum.STORAGE_IN_STATUS_ERROR);
        }
        if (!StorageInConstant.CANCELLABLE_STATUSES.contains(record.getStatus())) {
            throw new AppException(ExceptionEnum.STORAGE_IN_STATUS_ERROR);
        }
        record.setStatus(StorageInConstant.ST_CANCELLED)
                .setUpdateTime(new Date());
        this.updateById(record);
        return record;
    }

    // ==================== 发运（赠送领取后快递寄送） ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ItemStorageIn moveReceiverConfirm(String userId, MoveReceiverConfirmReqVO reqVO) {
        ItemStorageIn record = getById(reqVO.getRecordId());
        if (record == null) {
            throw new AppException(ExceptionEnum.STORAGE_IN_NOT_EXIST);
        }
        if (record.getBizType() != StorageInConstant.BIZ_MOVE
                || record.getStatus() != StorageInConstant.ST_MOVE_WAIT_RECEIVE) {
            throw new AppException(ExceptionEnum.STORAGE_IN_STATUS_ERROR);
        }
        if (!Objects.equals(record.getReceiverUserId(), userId)) {
            throw new AppException(ExceptionEnum.STORAGE_IN_NOT_RECEIVER);
        }
        // 解析接收方收件信息
        String[] recInfo = resolveReceiverContact(userId, reqVO.getStationId(),
                reqVO.getStationType() == null ? 1 : reqVO.getStationType());
        // 解析寄件方信息（发起人自建仓 / 本人）
        String[] sendInfo = resolveSenderContact(record.getUserId(), record.getStationId());

        KuaidiOrder order = kuaidi100Service.createOrder(record.getId(),
                sendInfo[0], sendInfo[1], sendInfo[2], recInfo[0], recInfo[1], recInfo[2]);

        record.setStatus(StorageInConstant.ST_MOVE_ORDERED)
                .setReceiverStationId(reqVO.getStationId())
                .setReceiverStationType(reqVO.getStationType() == null ? 1 : reqVO.getStationType())
                .setUpdateTime(new Date());
        this.updateById(record);
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ItemStorageIn moveOfflineDeliver(String userId, String recordId) {
        ItemStorageIn record = getById(recordId);
        if (record == null) {
            throw new AppException(ExceptionEnum.STORAGE_IN_NOT_EXIST);
        }
        if (record.getBizType() != StorageInConstant.BIZ_MOVE
                || record.getStatus() != StorageInConstant.ST_MOVE_WAIT_RECEIVE) {
            throw new AppException(ExceptionEnum.STORAGE_IN_STATUS_ERROR);
        }
        if (!Objects.equals(record.getUserId(), userId)) {
            throw new AppException(ExceptionEnum.STORAGE_IN_NOT_OWNER);
        }
        record.setStatus(StorageInConstant.ST_MOVE_OFFLINE)
                .setConfirmUserId(userId)
                .setUpdateTime(new Date());
        this.updateById(record);
        log.info("发运单线下当面交付 recordId={}, sender={}", recordId, userId);
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ItemStorageIn moveOfflineConfirm(String userId, MoveReceiverConfirmReqVO reqVO) {
        ItemStorageIn record = getById(reqVO.getRecordId());
        if (record == null) {
            throw new AppException(ExceptionEnum.STORAGE_IN_NOT_EXIST);
        }
        if (record.getBizType() != StorageInConstant.BIZ_MOVE
                || record.getStatus() != StorageInConstant.ST_MOVE_OFFLINE) {
            throw new AppException(ExceptionEnum.STORAGE_IN_STATUS_ERROR);
        }
        if (!Objects.equals(record.getReceiverUserId(), userId)) {
            throw new AppException(ExceptionEnum.STORAGE_IN_NOT_RECEIVER);
        }
        int stationType = reqVO.getStationType() == null ? 1 : reqVO.getStationType();
        record.setStatus(StorageInConstant.ST_MOVE_DONE)
                .setReceiverStationId(reqVO.getStationId())
                .setReceiverStationType(stationType)
                .setConfirmUserId(userId)
                .setConfirmTime(new Date())
                .setUpdateTime(new Date());
        this.updateById(record);
        // 实体落接收方所选仓（与快递签收同一入库逻辑：发送方库存清零才迁仓）
        ItemUserInventory senderInv = itemUserInventoryService.queryUserItem(record.getUserId(), record.getItemId());
        boolean senderEmpty = senderInv == null || senderInv.getQuantity() == null || senderInv.getQuantity() <= 0;
        if (senderEmpty) {
            moveItemStation(record.getItemId(), reqVO.getStationId(), stationType);
        }
        log.info("发运单线下收货入库完成 recordId={}, receiver={}, station={}, moved={}",
                record.getId(), userId, reqVO.getStationId(), senderEmpty);
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ItemStorageIn receiverSign(String userId, String recordId) {
        ItemStorageIn record = getById(recordId);
        if (record == null) {
            throw new AppException(ExceptionEnum.STORAGE_IN_NOT_EXIST);
        }
        // 快递下单后（待揽收/运输中）允许接收方手动确认签收（如当面签收、物流回调延迟）
        if (record.getBizType() != StorageInConstant.BIZ_MOVE
                || (record.getStatus() != StorageInConstant.ST_MOVE_ORDERED
                && record.getStatus() != StorageInConstant.ST_MOVE_SHIPPING)) {
            throw new AppException(ExceptionEnum.STORAGE_IN_STATUS_ERROR);
        }
        if (!Objects.equals(record.getReceiverUserId(), userId)) {
            throw new AppException(ExceptionEnum.STORAGE_IN_NOT_RECEIVER);
        }
        record.setStatus(StorageInConstant.ST_MOVE_DONE)
                .setConfirmUserId(userId)
                .setConfirmTime(new Date())
                .setUpdateTime(new Date());
        this.updateById(record);
        // 与快递回调签收同一入库逻辑：发送方库存清零才迁实体到接收方落仓
        ItemUserInventory senderInv = itemUserInventoryService.queryUserItem(record.getUserId(), record.getItemId());
        boolean senderEmpty = senderInv == null || senderInv.getQuantity() == null || senderInv.getQuantity() <= 0;
        if (senderEmpty && StrUtil.isNotBlank(record.getReceiverStationId())) {
            moveItemStation(record.getItemId(), record.getReceiverStationId(), record.getReceiverStationType());
        }
        log.info("发运单手动确认签收完成 recordId={}, receiver={}, moved={}",
                record.getId(), userId, senderEmpty);
        return record;
    }

    // ==================== 快递联动 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onKuaidiStatus(KuaidiOrder order, int newStatus) {
        ItemStorageIn record = getById(order.getRecordId());
        if (record == null || record.getBizType() != StorageInConstant.BIZ_MOVE) {
            return;
        }
        if (newStatus == 5) {
            // 签收：归属已在赠送领取时转移，此处仅实体落接收方仓（入库）
            if (record.getStatus() == StorageInConstant.ST_MOVE_DONE) {
                return;
            }
            record.setStatus(StorageInConstant.ST_MOVE_DONE)
                    .setConfirmUserId(record.getReceiverUserId())
                    .setConfirmTime(new Date())
                    .setUpdateTime(new Date());
            this.updateById(record);
            // 发送方（领取前持有人）库存已清零时，实体迁往接收方落仓
            ItemUserInventory senderInv = itemUserInventoryService.queryUserItem(record.getUserId(), record.getItemId());
            boolean senderEmpty = senderInv == null || senderInv.getQuantity() == null || senderInv.getQuantity() <= 0;
            if (senderEmpty && StrUtil.isNotBlank(record.getReceiverStationId())) {
                moveItemStation(record.getItemId(), record.getReceiverStationId(), record.getReceiverStationType());
            }
            log.info("发运单签收入库完成 recordId={}, item={}, receiver={}, moved={}",
                    record.getId(), record.getItemId(), record.getReceiverUserId(), senderEmpty);
        } else if (newStatus >= 2 && newStatus <= 4) {
            if (record.getStatus() != StorageInConstant.ST_MOVE_DONE
                    && record.getStatus() != StorageInConstant.ST_MOVE_SHIPPING) {
                record.setStatus(StorageInConstant.ST_MOVE_SHIPPING).setUpdateTime(new Date());
                this.updateById(record);
            }
        }
        // 0/1 已下单待揽收：31 不再变更
    }

    // ==================== 查询 ====================

    @Override
    public Page<StorageInListVO> queryList(Page<ItemStorageIn> page, String userId, Integer queryType, Integer bizType) {
        LambdaQueryWrapper<ItemStorageIn> wrapper = new LambdaQueryWrapper<>();
        if (queryType != null && queryType == 2) {
            // 我接收/待我接收的移库单
            wrapper.eq(ItemStorageIn::getReceiverUserId, userId)
                    .eq(ItemStorageIn::getBizType, StorageInConstant.BIZ_MOVE);
        } else {
            wrapper.eq(ItemStorageIn::getUserId, userId);
            if (bizType != null) {
                wrapper.eq(ItemStorageIn::getBizType, bizType);
            }
        }
        wrapper.orderByDesc(ItemStorageIn::getCreateTime);
        Page<ItemStorageIn> result = this.page(page, wrapper);
        return convertToVO(result, userId);
    }

    @Override
    public StorageInDetailVO queryDetail(String userId, String recordId) {
        ItemStorageIn record = getById(recordId);
        if (record == null) {
            throw new AppException(ExceptionEnum.STORAGE_IN_NOT_EXIST);
        }
        boolean related = Objects.equals(record.getUserId(), userId)
                || Objects.equals(record.getReceiverUserId(), userId);
        if (!related) {
            throw new AppException(ExceptionEnum.STORAGE_IN_NOT_OWNER);
        }
        List<StorageInDetailVO> detailList = new ArrayList<>();
        detailList.add(new StorageInDetailVO());
        fillVO(detailList, Collections.singletonList(record));
        StorageInDetailVO vo = detailList.get(0);
        // 官方仓收件地址
        if (record.getSource() == StorageInConstant.SOURCE_OFFICIAL) {
            WarehouseStation station = warehouseStationMapper.selectById(record.getStationId());
            if (station != null) {
                vo.setOfficialAddress(joinAddress(station.getProvince(), station.getCity(),
                        station.getDistrict(), station.getAddress()));
                vo.setOfficialContactPhone(station.getContactPhone());
            }
        }
        // 快递收寄双方信息
        KuaidiOrder order = kuaidi100Service.getLatestByRecordId(record.getId());
        if (order != null) {
            vo.setSendName(order.getSendName()).setSendPhone(order.getSendPhone()).setSendAddr(order.getSendAddr())
                    .setRecName(order.getRecName()).setRecPhone(order.getRecPhone()).setRecAddr(order.getRecAddr());
        }
        return vo;
    }

    // ==================== 官方后台 ====================

    @Override
    public Page<StorageInListVO> adminPage(Integer pageNo, Integer pageSize, String depositNo, Integer status, Integer bizType) {
        Page<ItemStorageIn> page = new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize);
        LambdaQueryWrapper<ItemStorageIn> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(depositNo)) {
            wrapper.like(ItemStorageIn::getDepositNo, depositNo);
        }
        if (status != null) {
            wrapper.eq(ItemStorageIn::getStatus, status);
        }
        if (bizType != null) {
            wrapper.eq(ItemStorageIn::getBizType, bizType);
        }
        wrapper.orderByDesc(ItemStorageIn::getCreateTime);
        Page<ItemStorageIn> result = this.page(page, wrapper);
        List<ItemStorageIn> records = result.getRecords() == null ? Collections.emptyList() : result.getRecords();
        List<StorageInListVO> vos = new ArrayList<>();
        for (ItemStorageIn ignored : records) {
            vos.add(new StorageInListVO());
        }
        fillVO(vos, records);
        Page<StorageInListVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(vos);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ItemStorageIn adminConfirmOfficial(String recordId) {
        ItemStorageIn record = getById(recordId);
        if (record == null) {
            throw new AppException(ExceptionEnum.STORAGE_IN_NOT_EXIST);
        }
        if (record.getBizType() != StorageInConstant.BIZ_TRANSFER_IN
                || record.getSource() != StorageInConstant.SOURCE_OFFICIAL
                || record.getStatus() != StorageInConstant.ST_MAILED) {
            throw new AppException(ExceptionEnum.STORAGE_IN_STATUS_ERROR);
        }
        record.setStatus(StorageInConstant.ST_STORED_OFFICIAL)
                .setConfirmTime(new Date())
                .setUpdateTime(new Date());
        this.updateById(record);
        // 物品落官方仓
        moveItemStation(record.getItemId(), record.getStationId(), 2);
        return record;
    }

    @Override
    public StorageInDetailVO adminDetail(String recordId) {
        ItemStorageIn record = getById(recordId);
        if (record == null) {
            throw new AppException(ExceptionEnum.STORAGE_IN_NOT_EXIST);
        }
        List<StorageInDetailVO> detailList = new ArrayList<>();
        detailList.add(new StorageInDetailVO());
        fillVO(detailList, Collections.singletonList(record));
        StorageInDetailVO vo = detailList.get(0);
        if (record.getSource() == StorageInConstant.SOURCE_OFFICIAL) {
            WarehouseStation station = warehouseStationMapper.selectById(record.getStationId());
            if (station != null) {
                vo.setOfficialAddress(joinAddress(station.getProvince(), station.getCity(),
                        station.getDistrict(), station.getAddress()));
                vo.setOfficialContactPhone(station.getContactPhone());
            }
        }
        KuaidiOrder order = kuaidi100Service.getLatestByRecordId(record.getId());
        if (order != null) {
            vo.setSendName(order.getSendName()).setSendPhone(order.getSendPhone()).setSendAddr(order.getSendAddr())
                    .setRecName(order.getRecName()).setRecPhone(order.getRecPhone()).setRecAddr(order.getRecAddr());
        }
        return vo;
    }

    // ==================== 校验 ====================

    @Override
    public boolean isItemLocked(String itemId) {
        Long count = this.getBaseMapper().selectCount(new LambdaQueryWrapper<ItemStorageIn>()
                .eq(ItemStorageIn::getItemId, itemId)
                .in(ItemStorageIn::getStatus, StorageInConstant.ACTIVE_STATUSES));
        return count != null && count > 0;
    }

    // ==================== 私有 ====================

    private void moveItemStation(String itemId, String stationId, Integer stationType) {
        ItemInfo info = itemInfoService.getById(itemId);
        if (info != null && stationId != null) {
            info.setStationId(stationId);
            info.setStationType(stationType);
            itemInfoService.updateById(info);
        }
    }

    /**
     * 接收方收件信息：[姓名, 电话, 地址]
     */
    private String[] resolveReceiverContact(String receiverId, String stationId, int stationType) {
        AppUser receiver = appUserMapper.selectById(receiverId);
        String name = receiver == null || StrUtil.isBlank(receiver.getNickname())
                ? receiverId : receiver.getNickname();
        String phone = receiver == null ? "" : StrUtil.nullToEmpty(receiver.getPhone());
        String addr;
        if (stationType == 2) {
            WarehouseStation station = warehouseStationMapper.selectById(stationId);
            if (station == null) {
                throw new AppException(ExceptionEnum.STATION_NOT_EXIST);
            }
            addr = joinAddress(station.getProvince(), station.getCity(), station.getDistrict(), station.getAddress());
            phone = StrUtil.blankToDefault(station.getContactPhone(), phone);
        } else if (stationType == 3) {
            UserStorage storage = userStorageMapper.selectById(stationId);
            if (storage == null || !Objects.equals(storage.getUserId(), receiverId)) {
                throw new AppException(ExceptionEnum.STORAGE_NOT_EXIST);
            }
            name = StrUtil.blankToDefault(storage.getReceiverName(), name);
            phone = StrUtil.blankToDefault(storage.getPhone(), phone);
            addr = joinAddress(storage.getProvince(), storage.getCity(), storage.getDistrict(), storage.getAddress());
        } else {
            UserVirtualStation virtual = userVirtualStationMapper.selectOne(
                    new LambdaQueryWrapper<UserVirtualStation>().eq(UserVirtualStation::getUserId, receiverId));
            addr = virtual == null || StrUtil.isBlank(virtual.getLocationDesc())
                    ? "芥子空间平台 OSS 仓库" : virtual.getLocationDesc();
        }
        if (StrUtil.hasBlank(name, phone, addr)) {
            throw new AppException(ExceptionEnum.RECEIVER_INFO_EMPTY);
        }
        return new String[]{name, phone, addr};
    }

    /**
     * 寄件方信息（发起人自建仓收件人信息兜底本人）：[姓名, 电话, 地址]
     */
    private String[] resolveSenderContact(String senderId, String sourceStationId) {
        UserStorage storage = userStorageMapper.selectById(sourceStationId);
        if (storage != null && StrUtil.isNotBlank(storage.getReceiverName())) {
            String addr = joinAddress(storage.getProvince(), storage.getCity(),
                    storage.getDistrict(), storage.getAddress());
            return new String[]{storage.getReceiverName(), StrUtil.nullToEmpty(storage.getPhone()), addr};
        }
        AppUser sender = appUserMapper.selectById(senderId);
        if (sender == null || StrUtil.hasBlank(sender.getNickname(), sender.getPhone())) {
            throw new AppException(ExceptionEnum.RECEIVER_INFO_EMPTY);
        }
        return new String[]{sender.getNickname(), sender.getPhone(), "芥子空间平台 OSS 仓库"};
    }

    private String joinAddress(Object province, Object city, Object district, Object address) {
        return String.join(" ", StrUtil.nullToEmpty((String) province), StrUtil.nullToEmpty((String) city),
                StrUtil.nullToEmpty((String) district), StrUtil.nullToEmpty((String) address)).trim();
    }

    private Page<StorageInListVO> convertToVO(Page<ItemStorageIn> page, String userId) {
        List<ItemStorageIn> records = page.getRecords() == null ? Collections.emptyList() : page.getRecords();
        List<StorageInListVO> vos = new ArrayList<>();
        for (ItemStorageIn ignored : records) {
            vos.add(new StorageInListVO());
        }
        fillVO(vos, records);
        Page<StorageInListVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(vos);
        return result;
    }

    /**
     * 填充列表/详情公共字段（物品信息、仓库名、用户昵称、快递信息）
     */
    private void fillVO(List<? extends StorageInListVO> vos, List<ItemStorageIn> records) {
        if (records.isEmpty()) {
            return;
        }
        Set<String> itemIds = records.stream().map(ItemStorageIn::getItemId).collect(Collectors.toSet());
        Map<String, ItemInfo> itemMap = new HashMap<>();
        for (ItemInfo info : itemInfoService.listByIds(itemIds)) {
            itemMap.put(String.valueOf(info.getId()), info);
        }
        Map<String, ItemSync> syncMap = new HashMap<>();
        for (ItemStorageIn record : records) {
            ItemSync sync = itemSyncService.getItemSyncByOriIdAndUserId(record.getOriId(), record.getUserId());
            if (sync != null) {
                syncMap.put(record.getId(), sync);
            }
        }
        Set<String> userIds = new java.util.HashSet<>();
        records.forEach(r -> {
            userIds.add(r.getUserId());
            if (r.getReceiverUserId() != null) {
                userIds.add(r.getReceiverUserId());
            }
        });
        Map<String, AppUser> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : appUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(AppUser::getId, Function.identity(), (a, b) -> a));
        Map<String, String> recordStationNames = new HashMap<>();
        for (ItemStorageIn record : records) {
            recordStationNames.put(record.getId(), resolveStationName(record.getStationId()));
        }
        Map<String, KuaidiOrder> orderMap = new HashMap<>();
        for (ItemStorageIn record : records) {
            KuaidiOrder order = kuaidi100Service.getLatestByRecordId(record.getId());
            if (order != null) {
                orderMap.put(record.getId(), order);
            }
        }
        for (int i = 0; i < records.size(); i++) {
            ItemStorageIn record = records.get(i);
            StorageInListVO vo = vos.get(i);
            vo.setId(record.getId())
                    .setDepositNo(record.getDepositNo())
                    .setBizType(record.getBizType())
                    .setSource(record.getSource())
                    .setStatus(record.getStatus())
                    .setStatusDesc(StorageInConstant.statusDesc(record.getStatus()))
                    .setItemId(record.getItemId())
                    .setQuantity(record.getQuantity())
                    .setStationId(record.getStationId())
                    .setStationName(recordStationNames.get(record.getId()))
                    .setUserId(record.getUserId())
                    .setReceiverUserId(record.getReceiverUserId())
                    .setExpressNo(record.getExpressNo())
                    .setCreateTime(record.getCreateTime())
                    .setUpdateTime(record.getUpdateTime());
            ItemInfo info = itemMap.get(record.getItemId());
            if (info != null) {
                vo.setItemName(info.getName());
                vo.setItemIcon(info.getIcon());
            } else {
                ItemSync sync = syncMap.get(record.getId());
                if (sync != null) {
                    vo.setItemName(sync.getName());
                    vo.setItemIcon(sync.getIcon());
                }
            }
            AppUser user = userMap.get(record.getUserId());
            if (user != null) {
                vo.setUserNickname(user.getNickname());
            }
            AppUser receiver = userMap.get(record.getReceiverUserId());
            if (receiver != null) {
                vo.setReceiverNickname(receiver.getNickname());
            }
            KuaidiOrder order = orderMap.get(record.getId());
            if (order != null) {
                vo.setKuaidiStatus(order.getOrderStatus())
                        .setKuaidinum(order.getKuaidinum())
                        .setPickCode(order.getPickCode());
            }
        }
    }

    private String resolveStationName(String stationId) {
        if (StrUtil.isBlank(stationId)) {
            return "";
        }
        UserStorage storage = userStorageMapper.selectById(stationId);
        if (storage != null) {
            return storage.getName();
        }
        WarehouseStation station = warehouseStationMapper.selectById(stationId);
        if (station != null) {
            return station.getStationName();
        }
        return stationId;
    }
}
