package org.jeecg.modules.app.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.app.bean.enums.ExceptionEnum;
import org.jeecg.modules.app.bean.enums.ItemStatusEnum;
import org.jeecg.modules.app.bean.enums.SyncOpsEnum;
import org.jeecg.modules.app.bean.enums.SyncStatusEnum;
import org.jeecg.modules.app.bean.exception.AppException;
import org.jeecg.modules.app.bean.vo.inventory.ItemInventoryVO;
import org.jeecg.modules.app.bean.vo.item.ItemInfoCreateVO;
import org.jeecg.modules.app.bean.vo.queue.*;
import org.jeecg.modules.app.constant.StorageInConstant;
import org.jeecg.modules.app.entity.ItemInfo;
import org.jeecg.modules.app.entity.ItemStorageIn;
import org.jeecg.modules.app.entity.ItemSync;
import org.jeecg.modules.app.entity.ItemUserInventory;
import org.jeecg.modules.app.mapper.ItemStorageInMapper;
import org.jeecg.modules.app.mapper.ItemSyncMapper;
import org.jeecg.modules.app.service.IItemInfoService;
import org.jeecg.modules.app.service.IItemRecycleService;
import org.jeecg.modules.app.service.IItemRuinsService;
import org.jeecg.modules.app.service.IItemSyncService;
import org.jeecg.modules.app.service.IItemUserInventoryService;
import org.jeecg.modules.app.utils.FieldElementUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ItemSyncServiceImpl extends ServiceImpl<ItemSyncMapper, ItemSync> implements IItemSyncService {

    @Lazy
    @Autowired
    IItemRuinsService itemRuinsService;

    @Lazy
    @Autowired
    IItemUserInventoryService itemUserInventoryService;

    @Lazy
    @Autowired
    IItemInfoService itemInfoService;

    @Lazy
    @Autowired
    IItemRecycleService itemRecycleService;

    @Autowired
    ItemStorageInMapper itemStorageInMapper;

    /** 物品是否存在在途/待确认转入/移库单（锁定判定） */
    private boolean isItemLocked(String itemId) {
        if (ObjectUtil.isEmpty(itemId)) {
            return false;
        }
        Long count = itemStorageInMapper.selectCount(new LambdaQueryWrapper<ItemStorageIn>()
                .eq(ItemStorageIn::getItemId, itemId)
                .in(ItemStorageIn::getStatus, StorageInConstant.ACTIVE_STATUSES));
        return count != null && count > 0;
    }

    // 根据itemId查询物品同步记录方法
    @Override
    public ItemSync getItemSyncByItemId(String itemId) {
        LambdaQueryWrapper<ItemSync> queryWrapper = new QueryWrapper<ItemSync>().lambda();
        queryWrapper.eq(ItemSync::getItemId, itemId);
        queryWrapper.last("limit 1");
        return this.getOne(queryWrapper);
    }

    @Override
    public ItemSync queryItemSyncByItemId(String itemId, String oriId, String userId) {
        LambdaQueryWrapper<ItemSync> queryWrapper = new QueryWrapper<ItemSync>().lambda();
        queryWrapper.eq(ItemSync::getItemId, itemId);
        queryWrapper.eq(ItemSync::getOriId, oriId);
        queryWrapper.eq(ItemSync::getUserId, userId);
        queryWrapper.last("limit 1");
        return this.getOne(queryWrapper);
    }

    // 根据oriId查询物品同步记录方法
    @Override
    public ItemSync getItemSyncByOriId(String oriId, String inventoryId) {
        LambdaQueryWrapper<ItemSync> queryWrapper = new QueryWrapper<ItemSync>().lambda();
        queryWrapper.eq(ItemSync::getOriId, oriId);
        queryWrapper.eq(ItemSync::getInventoryId, inventoryId);
        queryWrapper.last("limit 1");
        return this.getOne(queryWrapper);
    }

    // 检查原始Id+用户ID查找是否存在
    @Override
    public ItemSync getItemSyncByOriIdAndUserId(String oriId, String userId) {
        LambdaQueryWrapper<ItemSync> queryWrapper = new QueryWrapper<ItemSync>().lambda();
        queryWrapper.eq(ItemSync::getOriId, oriId);
        queryWrapper.eq(ItemSync::getUserId, userId);
        queryWrapper.last("limit 1");
        return this.getOne(queryWrapper);
    }

    // 检测线上物品ID是否存在
    @Override
    public ItemSync queryItemSync(MakeSyncIdReqVO syncVO) {
        if (ObjectUtil.isNotEmpty(syncVO.getItemId())) {
            ItemSync itemSync = this.getItemSyncByItemId(syncVO.getItemId());
            if (ObjectUtil.isNotEmpty(itemSync)) {
                return itemSync;
            }
        }
        // 检查是否存在队列id+原始Id
        if (ObjectUtil.isNotEmpty(syncVO.getQueueId()) && ObjectUtil.isNotEmpty(syncVO.getOriId())) {
            ItemSync itemSync = this.getItemSyncByOriId(syncVO.getOriId(), syncVO.getInventoryId());
            if (ObjectUtil.isNotEmpty(itemSync)) {
                return itemSync;
            }
        }
        // 检查原始Id+用户ID查找是否存在
        if (ObjectUtil.isNotEmpty(syncVO.getOriId()) && ObjectUtil.isNotEmpty(syncVO.getUserId())) {
            ItemSync itemSync = this.getItemSyncByOriIdAndUserId(syncVO.getOriId(), syncVO.getUserId());
            if (ObjectUtil.isNotEmpty(itemSync)) {
                return itemSync;
            }
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ItemSync makeSyncId(MakeSyncIdReqVO syncIdReqVO) {
        // 校验物品Id是否存在
        ItemSync itemSyncExist = this.queryItemSync(syncIdReqVO);
        if (ObjectUtil.isNotEmpty(itemSyncExist)) {
            return itemSyncExist;
        }
        // 校验用户ID是否存在
        if (ObjectUtil.isEmpty(syncIdReqVO.getUserId())) {
            throw new AppException(ExceptionEnum.USER_INFO_NOT_EXIST);
        }

        ItemSync itemSync = new ItemSync();
        itemSync.setOriId(syncIdReqVO.getOriId());
        itemSync.setUserId(syncIdReqVO.getUserId());
        itemSync.setCategoryId("0");
        itemSync.setVersion(1);
        itemSync.setSyncStatus(SyncStatusEnum.UNSYNCED.getCode());

        // 创建线上物品
        ItemInfoCreateVO itemInfoCreateVO = new ItemInfoCreateVO();
        itemInfoCreateVO.setOriId(syncIdReqVO.getOriId());
        itemInfoCreateVO.setUserId(syncIdReqVO.getUserId());
        itemInfoService.createItemInfoTmp(itemInfoCreateVO);
        // 创建线上用户物品库存
        ItemInventoryVO itemInventoryVO = new ItemInventoryVO();
        itemInventoryVO.setItemId(itemInfoCreateVO.getItemId());
        itemInventoryVO.setUserId(syncIdReqVO.getUserId());
        itemUserInventoryService.createItemUserInventoryTmp(itemInventoryVO);

        // 补全物品Id、持有Id
        itemSync.setItemId(itemInfoCreateVO.getItemId());
        itemSync.setInventoryId(itemInventoryVO.getId());

        // 创建同步物品
        this.saveOrUpdate(itemSync);

        return itemSync;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean syncUploadAdd(ItemSyncReqVO syncReqVO) {
        ItemSync itemSyncExist = validateAndGetItemSync(syncReqVO);
        
        ItemSync itemSync = FieldElementUtil.convertToJsonFields(syncReqVO, new ItemSync());
        return updateItemSyncAndRelated(itemSyncExist, itemSync, syncReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean syncUploadEdit(ItemSyncReqVO syncReqVO) {
        ItemSync itemSyncExist = validateAndGetItemSync(syncReqVO);
        // 在途/待确认转移的物品锁定：不可编辑（转入/移库完成或取消后解锁）
        if (isItemLocked(itemSyncExist.getItemId())) {
            throw new AppException(ExceptionEnum.ITEM_IN_TRANSIT);
        }

        ItemSync itemSync = FieldElementUtil.convertToJsonFields(syncReqVO, new ItemSync());
        return updateItemSyncAndRelated(itemSyncExist, itemSync, syncReqVO);
    }

    private ItemSync validateAndGetItemSync(ItemSyncReqVO syncReqVO) {
        if (ObjectUtil.isEmpty(syncReqVO.getItemId()) || 
            ObjectUtil.isEmpty(syncReqVO.getOriId()) || 
            ObjectUtil.isEmpty(syncReqVO.getUserId())) {
            throw new AppException(ExceptionEnum.REQUEST_PARAM_ERROR);
        }

        ItemSync itemSyncExist = this.queryItemSyncByItemId(
            syncReqVO.getItemId(), 
            syncReqVO.getOriId(), 
            syncReqVO.getUserId()
        );

        if (ObjectUtil.isEmpty(itemSyncExist)) {
            throw new AppException(ExceptionEnum.ITEM_NOT_EXIST);
        }

        if (SyncStatusEnum.DESTROYED.getCode() == itemSyncExist.getStatus()) {
            throw new AppException(ExceptionEnum.ITEM_DESTROYED);
        }

        return itemSyncExist;
    }

    private boolean updateItemSyncAndRelated(ItemSync itemSyncExist, ItemSync itemSync, ItemSyncReqVO syncReqVO) {
        int newVersion = itemSyncExist.getVersion() + 1;

        itemSync.setId(itemSyncExist.getId());
        itemSync.setSyncStatus(SyncStatusEnum.SUCCESS.getCode());
        itemSync.setVersion(newVersion);
        itemSync.setUserId(itemSyncExist.getUserId());
        itemSync.setItemId(itemSyncExist.getItemId());

        boolean result = this.updateById(itemSync);
        
        if (result) {
            ItemInfoCreateVO itemInfoCreateVO = FieldElementUtil.convertObject(itemSync, new ItemInfoCreateVO());
            itemInfoService.updateItemInfo(itemInfoCreateVO);

            ItemInventoryVO itemInventoryVO = FieldElementUtil.convertObject(itemSync, new ItemInventoryVO());
            itemUserInventoryService.updateItemUserInventory(itemInventoryVO);

            syncReqVO.setSyncStatus(SyncStatusEnum.SUCCESS.getCode());
            syncReqVO.setVersion(newVersion);
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean syncDestroy(ItemSyncReqVO syncVO) {
        return doDestroy(syncVO, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean syncDelete(ItemSyncReqVO syncVO) {
        return doDestroy(syncVO, true);
    }

    /**
     * 销毁/删除共用流程：库存校验、全量或部分扣减；
     * toRecycle=true 时落入本人独立回收站（不进殷墟公海），false 时掉落殷墟
     */
    private boolean doDestroy(ItemSyncReqVO syncVO, boolean toRecycle) {
        ItemSync itemSyncExist = validateAndGetItemSync(syncVO);
        // 在途/待确认转移的物品锁定：不可销毁/删除
        if (isItemLocked(itemSyncExist.getItemId())) {
            throw new AppException(ExceptionEnum.ITEM_IN_TRANSIT);
        }

        ItemUserInventory inventory = itemUserInventoryService.queryUserItem(syncVO.getUserId(), syncVO.getItemId());
        if (ObjectUtil.isEmpty(inventory)) {
            throw new AppException(ExceptionEnum.ITEM_NOT_EXIST);
        }

        int inventoryQuantity = ObjectUtil.isEmpty(inventory.getQuantity()) ? 0 : inventory.getQuantity();
        int destroyQuantity = ObjectUtil.isEmpty(syncVO.getQuantity()) ? inventoryQuantity : syncVO.getQuantity();

        // 库存已清零时销毁视为幂等完成：直接标记销毁并掉落废墟/回收站，
        // 避免同步队列重试时反复报“物品数量不足”卡死队列
        if (inventoryQuantity == 0) {
            return destroyItemCompletely(itemSyncExist, syncVO.getVersion(), toRecycle);
        }

        if (destroyQuantity > inventoryQuantity) {
            throw new AppException(ExceptionEnum.INSUFFICIENT_QUANTITY);
        }

        if (destroyQuantity >= inventoryQuantity) {
            return destroyItemCompletely(itemSyncExist, syncVO.getVersion(), toRecycle);
        } else {
            return destroyPartialQuantity(itemSyncExist, inventory, inventoryQuantity, destroyQuantity, syncVO.getVersion(), toRecycle);
        }
    }

    private boolean destroyItemCompletely(ItemSync itemSyncExist, Integer version, boolean toRecycle) {
        // 条件原子更新：仅未销毁状态可翻转，防止重复销毁并发覆盖
        int rows = this.baseMapper.markDestroyedIfNot(itemSyncExist.getId(), SyncStatusEnum.DESTROYED.getCode(), version);
        if (rows == 0) {
            throw new AppException(ExceptionEnum.ITEM_DESTROYED);
        }

        return toRecycle ? itemRecycleService.dropToRecycle(itemSyncExist) : itemRuinsService.dropToRuins(itemSyncExist);
    }

    private boolean destroyPartialQuantity(ItemSync itemSyncExist, ItemUserInventory inventory,
                                           int inventoryQuantity, int destroyQuantity, Integer version, boolean toRecycle) {
        // 条件原子扣减库存数量：quantity = quantity - ? WHERE id = ? AND quantity >= ?
        int rows = itemUserInventoryService.decreaseQuantityIfEnough(inventory.getId(), destroyQuantity);
        if (rows == 0) {
            throw new AppException(ExceptionEnum.INSUFFICIENT_QUANTITY);
        }

        // 同步记录数量更新为扣减后的剩余数量（保持云端物品档案与库存一致）
        ItemSync itemSyncUpdate = new ItemSync();
        itemSyncUpdate.setId(itemSyncExist.getId());
        itemSyncUpdate.setQuantity(inventoryQuantity - destroyQuantity);
        itemSyncUpdate.setVersion(version);
        this.updateById(itemSyncUpdate);

        // 扣减数量的物品掉入殷墟公海（同一物品多次部分销毁自动累加）
        // 或进入本人独立回收站（每条删除独立成记录）
        ItemSync destroyPart = new ItemSync();
        BeanUtil.copyProperties(itemSyncExist, destroyPart);
        destroyPart.setQuantity(destroyQuantity);
        return toRecycle ? itemRecycleService.dropToRecycle(destroyPart) : itemRuinsService.dropToRuins(destroyPart);
    }

    /**
     * 从 ItemInfo 批量补充仓位信息到同步记录（仅内存透传，不落库）
     */
    private void fillStationFromItemInfo(List<ItemSync> items) {
        if (ObjectUtil.isEmpty(items)) {
            return;
        }
        List<String> itemIds = items.stream()
                .map(ItemSync::getItemId)
                .filter(ObjectUtil::isNotEmpty)
                .distinct()
                .toList();
        if (ObjectUtil.isEmpty(itemIds)) {
            return;
        }
        Map<String, org.jeecg.modules.app.entity.ItemInfo> itemInfoMap = itemInfoService.listByIds(itemIds)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        info -> String.valueOf(info.getId()), info -> info, (a, b) -> a));
        for (ItemSync item : items) {
            org.jeecg.modules.app.entity.ItemInfo info = itemInfoMap.get(item.getItemId());
            if (info == null) {
                continue;
            }
            item.setStationId(info.getStationId());
            item.setStationType(info.getStationType());
        }
        // 在途/待确认角标：批量回填活动转入/移库单状态与单号（不落库）
        List<ItemStorageIn> activeRecords = itemStorageInMapper.selectList(
                new LambdaQueryWrapper<ItemStorageIn>()
                        .in(ItemStorageIn::getItemId, itemIds)
                        .in(ItemStorageIn::getStatus, StorageInConstant.ACTIVE_STATUSES));
        if (ObjectUtil.isNotEmpty(activeRecords)) {
            Map<String, ItemStorageIn> activeMap = activeRecords.stream()
                    .collect(java.util.stream.Collectors.toMap(
                            ItemStorageIn::getItemId, record -> record, (a, b) -> a));
            for (ItemSync item : items) {
                ItemStorageIn record = activeMap.get(item.getItemId());
                if (record != null) {
                    item.setStorageInStatus(record.getStatus());
                    item.setStorageInNo(record.getDepositNo());
                }
            }
        }
    }

    @Override
    public ItemSyncDownloadRspVO syncDownload(ItemSyncDownloadReqVO syncVO) {
        // 查询单个同步队列信息
        ItemSync itemSync = BeanUtil.copyProperties(syncVO, ItemSync.class);
        itemSync.setSyncStatus(SyncStatusEnum.SUCCESS.getCode());
        // 根据itemId、oriId更新同步状态
        LambdaQueryWrapper<ItemSync> queryWrapper = new QueryWrapper<ItemSync>().lambda();
        queryWrapper.eq(ItemSync::getItemId, itemSync.getItemId());
        queryWrapper.eq(ItemSync::getOriId, itemSync.getOriId());
        queryWrapper.eq(ItemSync::getInventoryId, itemSync.getInventoryId());
        queryWrapper.eq(ItemSync::getUserId, syncVO.getUserId());
        queryWrapper.last("limit 1");
        ItemSync itemSyncExist = this.getOne(queryWrapper);
        if (ObjectUtil.isEmpty(itemSyncExist)) {
            throw new AppException(ExceptionEnum.ITEM_NOT_EXIST);
        }
        ItemSyncDownloadRspVO rspVO = new ItemSyncDownloadRspVO();
        BeanUtil.copyProperties(itemSyncExist, rspVO);
        return rspVO;
    }

    @Override
    public ItemSyncDownloadIncrementalRspVO syncDownloadIncremental(ItemSyncDownloadReqVO syncVO) {
        Long lastSyncTime = syncVO.getLastSyncTime();
        Date now = new Date();

        LambdaQueryWrapper<ItemSync> queryWrapper = new QueryWrapper<ItemSync>().lambda();
        queryWrapper.eq(ItemSync::getUserId, syncVO.getUserId());

        if (ObjectUtil.isNotEmpty(lastSyncTime) && lastSyncTime > 0) {
            Date syncDate = new Date(lastSyncTime);
            queryWrapper.gt(ItemSync::getUpdateTime, syncDate);
        }

        queryWrapper.orderByAsc(ItemSync::getUpdateTime);

        List<ItemSync> allChangedItems = this.list(queryWrapper);

        List<ItemSync> needAddOrUpdateList = new ArrayList<>();
        List<String> needDeleteList = new ArrayList<>();

        for (ItemSync item : allChangedItems) {
            if (item.getStatus() != null && item.getStatus() == 3) {
                needDeleteList.add(item.getItemId());
            } else {
                needAddOrUpdateList.add(item);
            }
        }

        // 补充仓位信息（ItemSync 不落库仓位字段，从 ItemInfo 实时补充，供客户端展示/同步）
        fillStationFromItemInfo(needAddOrUpdateList);

        ItemSyncDownloadIncrementalRspVO rspVO = new ItemSyncDownloadIncrementalRspVO();
        rspVO.setServerTime(now.getTime());

        if (ObjectUtil.isEmpty(lastSyncTime) || lastSyncTime <= 0) {
            rspVO.setNeedAddList(needAddOrUpdateList);
            rspVO.setNeedUpdateList(new ArrayList<>());
        } else {
            rspVO.setNeedAddList(new ArrayList<>());
            rspVO.setNeedUpdateList(needAddOrUpdateList);
        }

        rspVO.setNeedDeleteList(needDeleteList);
        rspVO.setTotalCount(needAddOrUpdateList.size() + needDeleteList.size());
        rspVO.setHasMore(false);

        return rspVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ItemSync syncFromRuins(ItemSyncReqVO syncVO) {
        if (ObjectUtil.isEmpty(syncVO.getUserId()) || ObjectUtil.isEmpty(syncVO.getOriId())) {
            throw new AppException(ExceptionEnum.REQUEST_PARAM_ERROR);
        }
        // 创建拾取者名下线上物品（同oriId已持有则复用），数量取殷墟记录
        ItemInfoCreateVO itemInfoCreateVO = FieldElementUtil.convertObject(syncVO, new ItemInfoCreateVO());
        itemInfoCreateVO.setUserId(syncVO.getUserId());
        itemInfoCreateVO.setOriId(syncVO.getOriId());
        itemInfoCreateVO.setQuantity(syncVO.getQuantity());
        itemInfoCreateVO.setCategory("0");
        itemInfoCreateVO.setStatus(ItemStatusEnum.NORMAL.getCode());
        itemInfoService.createItemInfo(itemInfoCreateVO);
        String newItemId = itemInfoCreateVO.getItemId();

        // 库存：未持有则新建库存记录，已持有（重新拾取）则原子累加数量
        String inventoryId;
        ItemUserInventory existInventory = itemUserInventoryService.queryUserItem(syncVO.getUserId(), newItemId);
        if (ObjectUtil.isEmpty(existInventory)) {
            ItemInventoryVO inventoryVO = FieldElementUtil.convertObject(syncVO, new ItemInventoryVO());
            inventoryVO.setUserId(syncVO.getUserId());
            inventoryVO.setItemId(newItemId);
            inventoryVO.setQuantity(syncVO.getQuantity());
            itemUserInventoryService.createItemUserInventory(inventoryVO);
            inventoryId = inventoryVO.getId();
        } else {
            itemUserInventoryService.increaseQuantity(existInventory.getId(), syncVO.getQuantity());
            inventoryId = existInventory.getId();
        }

        // 同步记录：不存在则新增，已存在（重新拾取）则更新
        ItemSync itemSync = JSON.parseObject(JSON.toJSONString(syncVO), ItemSync.class);
        itemSync.setItemId(newItemId);
        itemSync.setInventoryId(inventoryId);
        itemSync.setUserId(syncVO.getUserId());
        itemSync.setSyncStatus(SyncStatusEnum.UNSYNCED.getCode());
        ItemSync itemSyncExist = this.queryItemSyncByItemId(newItemId, syncVO.getOriId(), syncVO.getUserId());
        if (ObjectUtil.isEmpty(itemSyncExist)) {
            itemSync.setVersion(1);
            this.save(itemSync);
        } else {
            itemSync.setId(itemSyncExist.getId());
            itemSync.setVersion(ObjectUtil.isEmpty(itemSyncExist.getVersion()) ? 1 : itemSyncExist.getVersion() + 1);
            this.updateById(itemSync);
        }

        // 补充仓位信息返回给app（ItemSync不落库仓位字段，从ItemInfo实时补充）
        ItemInfo itemInfo = itemInfoService.getById(newItemId);
        if (ObjectUtil.isNotEmpty(itemInfo)) {
            itemSync.setStationId(itemInfo.getStationId());
            itemSync.setStationType(itemInfo.getStationType());
        }
        return itemSync;
    }

    @Override
    public BatchItemSyncRspVO batchSyncUpload(BatchItemSyncReqVO batchVO) {
        BatchItemSyncRspVO rspVO = new BatchItemSyncRspVO();
        
        if (ObjectUtil.isEmpty(batchVO.getItems())) {
            return rspVO;
        }

        for (ItemSyncReqVO itemVO : batchVO.getItems()) {
            try {
                boolean result = switch (SyncOpsEnum.fromCode(itemVO.getSyncOps())) {
                    case ADD -> syncUploadAdd(itemVO);
                    case EDIT -> syncUploadEdit(itemVO);
                    case DESTROY -> syncDestroy(itemVO);
                    case DELETE -> syncDelete(itemVO);
                    case DEFAULT -> false;
                };

                if (result) {
                    ItemSyncRspVO successItem = new ItemSyncRspVO();
                    successItem.setOriId(itemVO.getOriId());
                    successItem.setItemId(itemVO.getItemId());
                    successItem.setInventoryId(itemVO.getInventoryId());
                    successItem.setSyncStatus(itemVO.getSyncStatus());
                    successItem.setVersion(itemVO.getVersion());
                    rspVO.getSuccessList().add(successItem);
                    rspVO.setSuccessCount(rspVO.getSuccessCount() + 1);
                } else {
                    BatchItemSyncRspVO.FailItemSyncRspVO failItem = new BatchItemSyncRspVO.FailItemSyncRspVO();
                    failItem.setOriId(itemVO.getOriId());
                    failItem.setItemId(itemVO.getItemId());
                    failItem.setErrorMsg("同步操作失败");
                    rspVO.getFailList().add(failItem);
                    rspVO.setFailCount(rspVO.getFailCount() + 1);
                }
            } catch (Exception e) {
                BatchItemSyncRspVO.FailItemSyncRspVO failItem = new BatchItemSyncRspVO.FailItemSyncRspVO();
                failItem.setOriId(itemVO.getOriId());
                failItem.setItemId(itemVO.getItemId());
                failItem.setErrorMsg(e.getMessage());
                rspVO.getFailList().add(failItem);
                rspVO.setFailCount(rspVO.getFailCount() + 1);
            }
        }
        
        return rspVO;
    }

}
