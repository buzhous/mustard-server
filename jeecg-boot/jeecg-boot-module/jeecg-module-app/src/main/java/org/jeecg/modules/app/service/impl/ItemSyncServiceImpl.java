package org.jeecg.modules.app.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.boot.starter.lock.annotation.JLock;
import org.jeecg.modules.app.bean.enums.ExceptionEnum;
import org.jeecg.modules.app.bean.enums.ItemStatusEnum;
import org.jeecg.modules.app.bean.enums.SyncOpsEnum;
import org.jeecg.modules.app.bean.enums.SyncStatusEnum;
import org.jeecg.modules.app.bean.exception.AppException;
import org.jeecg.modules.app.constant.LockConstant;
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
import org.jeecg.modules.app.mip.LineageService;
import org.jeecg.modules.app.mip.MipEventType;
import org.jeecg.modules.app.mip.MipWriteSupport;
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

    @Autowired
    LineageService lineageService;

    /** D1 流转要约锁（@Lazy 打破与 ItemFlowService→IItemSyncService 的循环引用） */
    @Lazy
    @Autowired
    private org.jeecg.modules.app.service.ItemFlowService itemFlowService;

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
        // 预建行 status 初始为正常：避免 status=NULL 触发下游 null 判定外的隐患
        itemSync.setStatus(ItemStatusEnum.NORMAL.getCode());
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
        // 流转要约锁定期：不可编辑（要约失效/拒绝/撤回/确认后解锁）
        if (itemFlowService.locked(itemSyncExist.getItemId())) {
            throw new AppException(ExceptionEnum.ITEM_FLOW_OFFERED);
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

        // status 为 NULL（外部路径预建）时不得因 int unboxing 抛 NPE
        if (itemSyncExist.getStatus() != null
                && SyncStatusEnum.DESTROYED.getCode() == itemSyncExist.getStatus()) {
            throw new AppException(ExceptionEnum.ITEM_DESTROYED);
        }

        return itemSyncExist;
    }

    private boolean updateItemSyncAndRelated(ItemSync itemSyncExist, ItemSync itemSync, ItemSyncReqVO syncReqVO) {
        // 乐观锁（全量严格）：产品未上线，无旧数据/旧客户端兼容负担。
        // add/edit 上传必带 version（客户端经 makeSyncId 幂等查回最新值），
        // 与云端不一致返回 10205，客户端按"云端为准"消费队列并立即拉取最新
        Integer reqVersion = syncReqVO.getVersion();
        if (reqVersion == null) {
            throw new AppException(ExceptionEnum.REQUEST_PARAM_ERROR);
        }
        Integer curVersion = itemSyncExist.getVersion();
        if (curVersion == null || !reqVersion.equals(curVersion)) {
            throw new AppException(ExceptionEnum.ITEM_VERSION_CONFLICT);
        }
        int newVersion = curVersion + 1;

        itemSync.setId(itemSyncExist.getId());
        itemSync.setSyncStatus(SyncStatusEnum.SUCCESS.getCode());
        itemSync.setVersion(newVersion);
        itemSync.setUserId(itemSyncExist.getUserId());
        itemSync.setItemId(itemSyncExist.getItemId());
        // MIP 写路径补级：fields 追加 sensitivity（服务端单点计算，幂等）
        itemSync.setFields(MipWriteSupport.enrich(itemSync.getFields()));

        // 条件更新（带库内当前版本约束）：读-校验-写非原子，双端并发以 SQL 层 CAS 兜底；
        // 影响行数 0 说明版本已被并发修改，拒绝本次写入
        boolean result = this.update(itemSync, new LambdaUpdateWrapper<ItemSync>()
                .eq(ItemSync::getId, itemSyncExist.getId())
                .eq(ItemSync::getVersion, curVersion));
        if (!result) {
            throw new AppException(ExceptionEnum.ITEM_VERSION_CONFLICT);
        }
        
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
    @JLock(lockKey = "'" + LockConstant.ITEM_DESTROY_LOCK + "' + #syncVO.oriId")
    public boolean syncDestroy(ItemSyncReqVO syncVO) {
        return doDestroy(syncVO, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @JLock(lockKey = "'" + LockConstant.ITEM_DESTROY_LOCK + "' + #syncVO.oriId")
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
        // 流转要约锁定期：不可销毁/删除/投放殷墟（要约失效/拒绝/撤回/确认后解锁）
        if (itemFlowService.locked(itemSyncExist.getItemId())) {
            throw new AppException(ExceptionEnum.ITEM_FLOW_OFFERED);
        }

        ItemUserInventory inventory = itemUserInventoryService.queryUserItem(syncVO.getUserId(), syncVO.getItemId());
        if (ObjectUtil.isEmpty(inventory)) {
            throw new AppException(ExceptionEnum.ITEM_NOT_EXIST);
        }

        int inventoryQuantity = ObjectUtil.isEmpty(inventory.getQuantity()) ? 0 : inventory.getQuantity();
        int destroyQuantity = ObjectUtil.isEmpty(syncVO.getQuantity()) ? inventoryQuantity : syncVO.getQuantity();

        // 库存已清零视为幂等完成；声明的销毁数量大于服务端库存（多端操作/历史扣减导致漂移）时
        // 按剩余库存收敛扣减。均避免同步队列重试时反复报“物品数量不足”卡死队列
        if (inventoryQuantity == 0) {
            return destroyItemCompletely(itemSyncExist, syncVO.getVersion(), toRecycle);
        }

        if (destroyQuantity > inventoryQuantity) {
            destroyQuantity = inventoryQuantity;
        }

        if (destroyQuantity >= inventoryQuantity) {
            return destroyItemCompletely(itemSyncExist, syncVO.getVersion(), toRecycle);
        } else {
            return destroyPartialQuantity(itemSyncExist, inventory, inventoryQuantity, destroyQuantity, syncVO.getVersion(), toRecycle);
        }
    }

    private boolean destroyItemCompletely(ItemSync itemSyncExist, Integer version, boolean toRecycle) {
        // 服务端权威版本号：客户端未传时取库内 version+1 兜底，防止 markDestroyedIfNot 显式写 version=NULL 污染版本链
        Integer newVersion = ObjectUtil.defaultIfNull(version,
                ObjectUtil.defaultIfNull(itemSyncExist.getVersion(), 1) + 1);
        // 条件原子更新：仅未销毁状态可翻转，防止重复销毁并发覆盖
        int rows = this.baseMapper.markDestroyedIfNot(itemSyncExist.getId(), SyncStatusEnum.DESTROYED.getCode(), newVersion);
        if (rows == 0) {
            throw new AppException(ExceptionEnum.ITEM_DESTROYED);
        }

        boolean result = toRecycle ? itemRecycleService.dropToRecycle(itemSyncExist) : itemRuinsService.dropToRuins(itemSyncExist);
        if (result) {
            // MIP 流转账本：进入回收站（recycle）/ 丢入殷墟（ruins_drop），幂等键含同步记录与版本
            appendDestroyLineage(itemSyncExist, newVersion, null,
                    toRecycle ? MipEventType.RECYCLE : MipEventType.RUINS_DROP);
        }
        return result;
    }

    private boolean destroyPartialQuantity(ItemSync itemSyncExist, ItemUserInventory inventory,
                                           int inventoryQuantity, int destroyQuantity, Integer version, boolean toRecycle) {
        // 条件原子扣减库存数量：quantity = quantity - ? WHERE id = ? AND quantity >= ?
        int rows = itemUserInventoryService.decreaseQuantityIfEnough(inventory.getId(), destroyQuantity);
        if (rows == 0) {
            throw new AppException(ExceptionEnum.INSUFFICIENT_QUANTITY);
        }

        // 同步记录数量更新为扣减后的剩余数量（保持云端物品档案与库存一致）
        // 版本号服务端权威计算，客户端未传时 +1 兜底，避免客户端值直写/写 NULL
        ItemSync itemSyncUpdate = new ItemSync();
        itemSyncUpdate.setId(itemSyncExist.getId());
        itemSyncUpdate.setQuantity(inventoryQuantity - destroyQuantity);
        itemSyncUpdate.setVersion(ObjectUtil.defaultIfNull(version,
                ObjectUtil.defaultIfNull(itemSyncExist.getVersion(), 1) + 1));
        this.updateById(itemSyncUpdate);

        // 扣减数量的物品掉入殷墟公海（同一物品多次部分销毁自动累加）
        // 或进入本人独立回收站（每条删除独立成记录）
        ItemSync destroyPart = new ItemSync();
        BeanUtil.copyProperties(itemSyncExist, destroyPart);
        destroyPart.setQuantity(destroyQuantity);
        boolean result = toRecycle ? itemRecycleService.dropToRecycle(destroyPart) : itemRuinsService.dropToRuins(destroyPart);
        if (result) {
            appendDestroyLineage(itemSyncExist, version, destroyQuantity,
                    toRecycle ? MipEventType.RECYCLE : MipEventType.RUINS_DROP);
        }
        return result;
    }

    /**
     * 销毁类流转记账：recycle / ruins_drop（MIP 协议 5.2；幂等键=syncId+版本，队列重试零重复）
     */
    private void appendDestroyLineage(ItemSync itemSyncExist, Integer version, Integer qty, MipEventType type) {
        lineageService.append(new org.jeecg.modules.app.entity.ItemLineage()
                .setOriId(itemSyncExist.getOriId())
                .setEventType(type.getCode())
                .setFromUid(LineageService.hashUid(itemSyncExist.getUserId()))
                .setOperator(LineageService.OPERATOR_USER)
                .setQty(qty)
                .setPayload(toRecyclePayload(itemSyncExist))
                .setIdempotencyKey(type.getCode() + ":" + itemSyncExist.getId() + ":" + version));
    }

    /** 事件摘要：仅标识信息，禁止 fields 内容（协议 5.3） */
    private String toRecyclePayload(ItemSync sync) {
        var p = new com.alibaba.fastjson.JSONObject(true);
        p.put("item_id", sync.getItemId());
        p.put("version", sync.getVersion());
        return p.toJSONString();
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
        // 仅投影 3 列：全量拉取时避免经 WAN 传输整行（fields 等大列），慢库带宽下可省 ~60s
        Map<String, org.jeecg.modules.app.entity.ItemInfo> itemInfoMap = itemInfoService
                .list(new LambdaQueryWrapper<org.jeecg.modules.app.entity.ItemInfo>()
                        .in(org.jeecg.modules.app.entity.ItemInfo::getId, itemIds)
                        .select(org.jeecg.modules.app.entity.ItemInfo::getId,
                                org.jeecg.modules.app.entity.ItemInfo::getStationId,
                                org.jeecg.modules.app.entity.ItemInfo::getStationType))
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

        // 分页（2000 人并发评估）：客户端传 pageSize>0 时启用双游标翻页
        // （update_time 相同毫秒内按 id 续拉，防同毫秒漏页），上限 500；
        // 缺省不分页保持全量返回，旧调用方零影响
        Integer reqPageSize = syncVO.getPageSize();
        boolean paged = reqPageSize != null && reqPageSize > 0;
        int pageSize = paged ? Math.min(reqPageSize, 500) : 0;
        Long cursorId = syncVO.getCursorId();

        LambdaQueryWrapper<ItemSync> queryWrapper = new QueryWrapper<ItemSync>().lambda();
        queryWrapper.eq(ItemSync::getUserId, syncVO.getUserId());

        // 排除空骨架行：makeSyncId 预建行（UNSYNCED 且 fields 为空）在客户端上传失败期间
        // 若被增量下发，会以空内容覆盖本地完整物品并丢弃待上传快照，造成数据丢失。
        // 殷墟拾取行（UNSYNCED 但 fields 有内容）不受影响，正常下发
        queryWrapper.and(w -> w.isNotNull(ItemSync::getFields).ne(ItemSync::getFields, ""));

        if (ObjectUtil.isNotEmpty(lastSyncTime) && lastSyncTime > 0) {
            Date syncDate = new Date(lastSyncTime);
            if (paged && cursorId != null && cursorId > 0) {
                // 双游标：(update_time, id) 单调递增，同毫秒内的行按 id 续拉
                queryWrapper.and(w -> w.gt(ItemSync::getUpdateTime, syncDate)
                        .or(w2 -> w2.eq(ItemSync::getUpdateTime, syncDate).gt(ItemSync::getId, cursorId)));
            } else {
                queryWrapper.gt(ItemSync::getUpdateTime, syncDate);
            }
        }

        queryWrapper.orderByAsc(ItemSync::getUpdateTime).orderByAsc(ItemSync::getId);
        if (paged) {
            queryWrapper.last("LIMIT " + (pageSize + 1));
        }

        List<ItemSync> allChangedItems = this.list(queryWrapper);
        boolean hasMore = paged && allChangedItems.size() > pageSize;
        if (hasMore) {
            allChangedItems = allChangedItems.subList(0, pageSize);
        }

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
        // 全量记录总数（不滤 update_time）：客户端据此发现"水位线越前导致增量拉空但服务端有数据"的漏拉场景。
        // 仅首页（无游标）计算，翻页沿用客户端缓存的首页值，避免每页 COUNT
        if (!paged || cursorId == null || cursorId <= 0) {
            rspVO.setTotal(this.count(new LambdaQueryWrapper<ItemSync>().eq(ItemSync::getUserId, syncVO.getUserId())));
        }
        rspVO.setHasMore(hasMore);
        if (hasMore && !allChangedItems.isEmpty()) {
            ItemSync lastRow = allChangedItems.get(allChangedItems.size() - 1);
            if (lastRow.getUpdateTime() != null) {
                rspVO.setNextCursorTime(lastRow.getUpdateTime().getTime());
                rspVO.setNextCursorId(lastRow.getId());
            } else {
                // 游标行 update_time 异常为 NULL：不产生翻页游标，客户端按本页终止（total 自愈兜底）
                rspVO.setHasMore(false);
            }
        }

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
                    // 回填真实落库态（sync_status/version），避免回显请求值造成客户端误判
                    ItemSync syncedRow = this.queryItemSyncByItemId(itemVO.getItemId(), itemVO.getOriId(), itemVO.getUserId());
                    if (syncedRow != null) {
                        successItem.setSyncStatus(syncedRow.getSyncStatus());
                        successItem.setVersion(syncedRow.getVersion());
                    } else {
                        successItem.setSyncStatus(itemVO.getSyncStatus());
                        successItem.setVersion(itemVO.getVersion());
                    }
                    rspVO.getSuccessList().add(successItem);
                    rspVO.setSuccessCount(rspVO.getSuccessCount() + 1);
                } else {
                    BatchItemSyncRspVO.FailItemSyncRspVO failItem = this.buildFailItem(itemVO,
                            new AppException(ExceptionEnum.REQUEST_PARAM_ERROR, "同步操作失败"));
                    rspVO.getFailList().add(failItem);
                    rspVO.setFailCount(rspVO.getFailCount() + 1);
                }
            } catch (Exception e) {
                rspVO.getFailList().add(this.buildFailItem(itemVO, e));
                rspVO.setFailCount(rspVO.getFailCount() + 1);
            }
        }

        return rspVO;
    }

    /**
     * 构造失败项并分类：AppException 按错误码判定 permanent（物品不存在/已销毁/参数错误为永久性失败，
     * 重试无效；在途/流转锁定期为可重试）；未分类系统异常一律可重试。
     */
    private BatchItemSyncRspVO.FailItemSyncRspVO buildFailItem(ItemSyncReqVO itemVO, Exception e) {
        BatchItemSyncRspVO.FailItemSyncRspVO failItem = new BatchItemSyncRspVO.FailItemSyncRspVO();
        failItem.setOriId(itemVO.getOriId());
        failItem.setItemId(itemVO.getItemId());
        failItem.setErrorMsg(e.getMessage());
        if (e instanceof AppException appEx) {
            int code = appEx.getCode();
            failItem.setCode(code);
            failItem.setPermanent(code == ExceptionEnum.REQUEST_PARAM_ERROR.getCode()
                    || code == ExceptionEnum.ITEM_NOT_EXIST.getCode()
                    || code == ExceptionEnum.ITEM_DESTROYED.getCode()
                    || code == ExceptionEnum.ITEM_VERSION_CONFLICT.getCode());
        } else {
            failItem.setCode(500);
            failItem.setPermanent(false);
        }
        return failItem;
    }

}
