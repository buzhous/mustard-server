package org.jeecg.modules.app.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.app.bean.enums.ExceptionEnum;
import org.jeecg.modules.app.bean.enums.GiftStatusEnum;
import org.jeecg.modules.app.bean.enums.GiftTypeEnum;
import org.jeecg.modules.app.bean.enums.SyncStatusEnum;
import org.jeecg.modules.app.bean.exception.AppException;
import org.jeecg.modules.app.bean.vo.gift.ItemGiftListVO;
import org.jeecg.modules.app.bean.vo.gift.ItemGiftReceiveVO;
import org.jeecg.modules.app.bean.vo.gift.ItemGiftReqVO;
import org.jeecg.modules.app.bean.vo.gift.ItemGiftRspVO;
import org.jeecg.modules.app.bean.vo.gift.ReceiveGiftReqVO;
import org.jeecg.modules.app.constant.StorageInConstant;
import org.jeecg.modules.app.entity.*;
import org.jeecg.modules.app.entity.user.AppUser;
import org.jeecg.modules.app.mapper.ItemGiftMapper;
import org.jeecg.modules.app.mapper.ItemGiftReceiveMapper;
import org.jeecg.modules.app.mip.LineageService;
import org.jeecg.modules.app.mip.MipEventType;
import org.jeecg.modules.app.service.IAppUserService;
import org.jeecg.modules.app.service.IItemGiftService;
import org.jeecg.modules.app.service.IItemInfoService;
import org.jeecg.modules.app.service.IItemSyncService;
import org.jeecg.modules.app.service.IItemUserInventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ItemGiftServiceImpl extends ServiceImpl<ItemGiftMapper, ItemGift> implements IItemGiftService {

    @Lazy
    @Autowired
    private IItemUserInventoryService itemUserInventoryService;

    @Lazy
    @Autowired
    private IItemInfoService itemInfoService;

    @Lazy
    @Autowired
    private IItemSyncService itemSyncService;

    @Lazy
    @Autowired
    private IAppUserService appUserService;

    @Autowired
    private ItemGiftReceiveMapper itemGiftReceiveMapper;

    @Autowired
    private org.jeecg.modules.app.mapper.ItemStorageInMapper itemStorageInMapper;

    @Lazy
    @Autowired
    private LineageService lineageService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ItemGiftRspVO createGift(ItemGiftReqVO reqVO, String userId) {
        return this.createGift(reqVO, userId, LineageService.OPERATOR_USER);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ItemGiftRspVO createGift(ItemGiftReqVO reqVO, String userId, String operator) {
        // 在途/待确认转移的物品锁定：不可赠送
        Long activeCount = itemStorageInMapper.selectCount(
                new LambdaQueryWrapper<ItemStorageIn>()
                        .eq(ItemStorageIn::getItemId, reqVO.getItemId())
                        .in(ItemStorageIn::getStatus, StorageInConstant.ACTIVE_STATUSES));
        if (activeCount != null && activeCount > 0) {
            throw new AppException(ExceptionEnum.ITEM_IN_TRANSIT);
        }
        ItemUserInventory inventory = itemUserInventoryService.queryUserItem(userId, reqVO.getItemId());
        if (ObjectUtil.isEmpty(inventory)) {
            throw new AppException(ExceptionEnum.ITEM_NOT_EXIST);
        }

        int inventoryQuantity = ObjectUtil.isEmpty(inventory.getQuantity()) ? 0 : inventory.getQuantity();
        if (reqVO.getQuantity() > inventoryQuantity) {
            throw new AppException(ExceptionEnum.INSUFFICIENT_QUANTITY);
        }

        ItemGift itemGift = new ItemGift();
        itemGift.setGiftType(reqVO.getGiftType());
        itemGift.setStatus(GiftStatusEnum.PENDING.getCode());
        itemGift.setSenderUserId(userId);
        itemGift.setReceiverUserId(reqVO.getReceiverUserId());
        itemGift.setItemId(reqVO.getItemId());
        itemGift.setOriId(reqVO.getOriId());
        itemGift.setInventoryId(inventory.getId());
        itemGift.setTotalQuantity(reqVO.getQuantity());
        itemGift.setReceivedQuantity(0);
        itemGift.setRemark(reqVO.getRemark());
        itemGift.setCreateTime(new Date());
        itemGift.setUpdateTime(new Date());
        itemGift.setValidStartTime(new Date());
        itemGift.setValidEndTime(DateUtil.offsetDay(new Date(), 999));
        if (GiftTypeEnum.PUBLIC.getCode() == reqVO.getGiftType()) {
            itemGift.setGiftCode(generateGiftCode());
            itemGift.setQrCode(generateQrCode(itemGift.getGiftCode()));
            itemGift.setValidStartTime(new Date());
            itemGift.setValidEndTime(reqVO.getValidEndTime());
            itemGift.setPerLimit(ObjectUtil.isEmpty(reqVO.getPerLimit()) ? 1 : reqVO.getPerLimit());
        }

        this.save(itemGift);

        inventory.setQuantity(inventoryQuantity - reqVO.getQuantity());
        itemUserInventoryService.updateItemById(inventory);

        ItemSync itemSync = itemSyncService.getItemSyncByItemId(reqVO.getItemId());
        if (ObjectUtil.isNotEmpty(itemSync)) {
            itemSync.setQuantity(inventory.getQuantity());
            if (inventory.getQuantity() <= 0) {
                itemSync.setStatus(SyncStatusEnum.DESTROYED.getCode());
            }
            itemSyncService.updateById(itemSync);
        }

        // MIP 流转账本：赠送发出（custody 挪出到托管；幂等键=赠送单）
        lineageService.append(new org.jeecg.modules.app.entity.ItemLineage()
                .setOriId(itemGift.getOriId())
                .setEventType(MipEventType.GIFT_OUT.getCode())
                .setFromUid(LineageService.hashUid(userId))
                .setToUid(GiftTypeEnum.DIRECT.getCode() == itemGift.getGiftType()
                        ? LineageService.hashUid(itemGift.getReceiverUserId()) : null)
                .setOperator(operator)
                .setQty(reqVO.getQuantity())
                .setIdempotencyKey("gift_out:" + itemGift.getId()));

        return BeanUtil.copyProperties(itemGift, ItemGiftRspVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> receiveGift(ReceiveGiftReqVO reqVO, String userId) {
        ItemGift itemGift = null;

        if (ObjectUtil.isNotEmpty(reqVO.getGiftCode())) {
            itemGift = this.getGiftByCode(reqVO.getGiftCode());
        } else if (ObjectUtil.isNotEmpty(reqVO.getGiftId())) {
            itemGift = this.getById(reqVO.getGiftId());
        }

        if (ObjectUtil.isEmpty(itemGift)) {
            throw new AppException(ExceptionEnum.GIFT_NOT_EXIST);
        }

        if (GiftStatusEnum.CANCELLED.getCode() == itemGift.getStatus()) {
            throw new AppException(ExceptionEnum.GIFT_CANCELLED);
        }

        if (GiftStatusEnum.EXPIRED.getCode() == itemGift.getStatus()) {
            throw new AppException(ExceptionEnum.GIFT_EXPIRED);
        }

        if (GiftStatusEnum.FULL_RECEIVED.getCode() == itemGift.getStatus()) {
            throw new AppException(ExceptionEnum.GIFT_RECEIVED);
        }

        Date now = new Date();
        if (ObjectUtil.isNotEmpty(itemGift.getValidEndTime()) && now.after(itemGift.getValidEndTime())) {
            itemGift.setStatus(GiftStatusEnum.EXPIRED.getCode());
            this.updateById(itemGift);
            throw new AppException(ExceptionEnum.GIFT_EXPIRED);
        }

        if (GiftTypeEnum.DIRECT.getCode() == itemGift.getGiftType()) {
            if (!userId.equals(itemGift.getReceiverUserId())) {
                throw new AppException(ExceptionEnum.GIFT_NOT_BELONG_TO_USER);
            }
            // 领取全部数量物品
            reqVO.setQuantity(itemGift.getTotalQuantity());
        } else {
            if (itemGift.getPerLimit() != null && itemGift.getPerLimit() > 0) {
                int receivedCount = countUserReceivedGift(itemGift.getId(), userId);
                if (receivedCount >= itemGift.getPerLimit()) {
                    throw new AppException(ExceptionEnum.GIFT_PER_LIMIT_EXCEEDED);
                }
            }
            if (itemGift.getPerLimit() == null || itemGift.getPerLimit() == 0) {
                throw new AppException(ExceptionEnum.GIFT_PER_LIMIT_EXCEEDED);
            }
            // 领取数量不得超过限制数量
            if (reqVO.getQuantity() > itemGift.getPerLimit()) {
                throw new AppException(ExceptionEnum.GIFT_PER_LIMIT_EXCEEDED);
            }
        }

        int receiveQuantity = ObjectUtil.isEmpty(reqVO.getQuantity()) ? 1 : reqVO.getQuantity();
        int remainQuantity = itemGift.getTotalQuantity() - itemGift.getReceivedQuantity();

        if (receiveQuantity > remainQuantity) {
            throw new AppException(ExceptionEnum.GIFT_INSUFFICIENT_QUANTITY);
        }

        ItemSync itemSync = itemSyncService.getItemSyncByItemId(itemGift.getItemId());
        if (ObjectUtil.isEmpty(itemSync)) {
            throw new AppException(ExceptionEnum.ITEM_NOT_EXIST);
        }

        ItemInfo itemInfo = itemInfoService.getById(itemGift.getItemId());
        if (ObjectUtil.isEmpty(itemInfo)) {
            throw new AppException(ExceptionEnum.ITEM_NOT_EXIST);
        }

        ItemUserInventory userInventory = itemUserInventoryService.queryUserItem(userId, itemGift.getItemId());
        ItemSync userItemSync = itemSyncService.getItemSyncByOriIdAndUserId(itemGift.getOriId(), userId);

        if (ObjectUtil.isEmpty(userInventory)) {
            userInventory = new ItemUserInventory();
            userInventory.setItemId(itemGift.getItemId());
            userInventory.setUserId(userId);
            userInventory.setQuantity(receiveQuantity);
            userInventory.setStatus(0);
            userInventory.setIsDrop(0);
            userInventory.setIsSync(0);
            userInventory.setCreateTime(new Date());
            userInventory.setGetTime(new Date());
            itemUserInventoryService.save(userInventory);

            if (ObjectUtil.isEmpty(userItemSync)) {
                ItemSync newItemSync = new ItemSync();
                BeanUtil.copyProperties(itemSync, newItemSync);
                newItemSync.setId(null);
                newItemSync.setUserId(userId);
                newItemSync.setQuantity(receiveQuantity);
                newItemSync.setSyncStatus(SyncStatusEnum.UNSYNCED.getCode());
                // 源记录可能是赠出方的销毁行：领取方副本必须重置为有效状态并挂接自己的库存
                newItemSync.setStatus(SyncStatusEnum.SUCCESS.getCode());
                newItemSync.setInventoryId(userInventory.getId());
                newItemSync.setVersion(1);
                newItemSync.setCreateTime(new Date());
                newItemSync.setUpdateTime(new Date());
                itemSyncService.save(newItemSync);
            } else {
                userItemSync.setQuantity(userItemSync.getQuantity() + receiveQuantity);
                userItemSync.setUpdateTime(new Date());
                itemSyncService.updateById(userItemSync);
            }
        } else {
            userInventory.setQuantity(userInventory.getQuantity() + receiveQuantity);
            itemUserInventoryService.updateItemById(userInventory);

            if (ObjectUtil.isNotEmpty(userItemSync)) {
                userItemSync.setQuantity(userItemSync.getQuantity() + receiveQuantity);
                userItemSync.setUpdateTime(new Date());
                itemSyncService.updateById(userItemSync);
            }
        }

        itemGift.setReceivedQuantity(itemGift.getReceivedQuantity() + receiveQuantity);
        if (itemGift.getReceivedQuantity() >= itemGift.getTotalQuantity()) {
            itemGift.setStatus(GiftStatusEnum.FULL_RECEIVED.getCode());
        } else {
            itemGift.setStatus(GiftStatusEnum.PARTIAL_RECEIVED.getCode());
        }
        itemGift.setUpdateTime(new Date());
        this.updateById(itemGift);

        // MIP 流转账本：赠送领取（custody 交接；幂等键=赠送单+领取人+第N次领取）
        int receiveSeq = countUserReceivedGift(itemGift.getId(), userId) + 1;
        saveGiftReceiveRecord(itemGift.getId(), userId, receiveQuantity);
        lineageService.append(new org.jeecg.modules.app.entity.ItemLineage()
                .setOriId(itemGift.getOriId())
                .setEventType(MipEventType.GIFT_IN.getCode())
                .setFromUid(LineageService.hashUid(itemGift.getSenderUserId()))
                .setToUid(LineageService.hashUid(userId))
                .setOperator(LineageService.OPERATOR_USER)
                .setQty(receiveQuantity)
                .setIdempotencyKey("gift_in:" + itemGift.getId() + ":" + userId + ":" + receiveSeq));

        // 自建仓实物：领取即赠送完成（归属已转移），自动创建发运单（快递寄送，待接收方确认落仓）
        String shipRecordId = null;
        String shipDepositNo = null;
        ItemInfo info = itemInfoService.getById(itemGift.getItemId());
        if (info != null && info.getStationType() != null && info.getStationType() == 3
                && StrUtil.isNotBlank(info.getStationId())) {
            ItemStorageIn ship = new ItemStorageIn()
                    .setUserId(itemGift.getSenderUserId())
                    .setItemId(itemGift.getItemId())
                    .setOriId(itemGift.getOriId())
                    .setInventoryId(userInventory.getId())
                    .setStationId(info.getStationId())
                    .setSource(StorageInConstant.SOURCE_STORAGE)
                    .setBizType(StorageInConstant.BIZ_MOVE)
                    .setStatus(StorageInConstant.ST_MOVE_WAIT_RECEIVE)
                    .setReceiverUserId(userId)
                    .setGiftId(itemGift.getId())
                    .setQuantity(receiveQuantity)
                    .setCreateTime(new Date())
                    .setUpdateTime(new Date());
            itemStorageInMapper.insert(ship);
            ship.setDepositNo(StorageInConstant.buildDepositNo(ship.getId()));
            itemStorageInMapper.updateById(ship);
            shipRecordId = String.valueOf(ship.getId());
            shipDepositNo = ship.getDepositNo();
        }
        Map<String, Object> data = new HashMap<>();
        data.put("shipRecordId", shipRecordId);
        data.put("shipDepositNo", shipDepositNo);
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelGift(String giftId, String userId) {
        ItemGift itemGift = this.getById(giftId);
        if (ObjectUtil.isEmpty(itemGift)) {
            throw new AppException(ExceptionEnum.GIFT_NOT_EXIST);
        }

        if (!userId.equals(itemGift.getSenderUserId())) {
            throw new AppException(ExceptionEnum.GIFT_NOT_BELONG_TO_USER);
        }

        if (GiftStatusEnum.CANCELLED.getCode() == itemGift.getStatus()) {
            throw new AppException(ExceptionEnum.GIFT_ALREADY_CANCELLED);
        }

        if (GiftStatusEnum.FULL_RECEIVED.getCode() == itemGift.getStatus()) {
            throw new AppException(ExceptionEnum.GIFT_ALREADY_RECEIVED);
        }

        int remainQuantity = itemGift.getTotalQuantity() - itemGift.getReceivedQuantity();

        if (remainQuantity > 0) {
            ItemUserInventory inventory = itemUserInventoryService.queryUserItem(userId, itemGift.getItemId());
            if (ObjectUtil.isEmpty(inventory)) {
                inventory = new ItemUserInventory();
                inventory.setItemId(itemGift.getItemId());
                inventory.setUserId(userId);
                inventory.setQuantity(remainQuantity);
                inventory.setStatus(0);
                inventory.setIsDrop(0);
                inventory.setIsSync(0);
                inventory.setCreateTime(new Date());
                inventory.setGetTime(new Date());
                itemUserInventoryService.save(inventory);
            } else {
                inventory.setQuantity(inventory.getQuantity() + remainQuantity);
                itemUserInventoryService.updateItemById(inventory);
            }

            ItemSync itemSync = itemSyncService.getItemSyncByItemId(itemGift.getItemId());
            if (ObjectUtil.isNotEmpty(itemSync) && itemSync.getStatus() != null && itemSync.getStatus() == 3) {
                itemSync.setStatus(0);
                itemSyncService.updateById(itemSync);
            }
        }

        itemGift.setStatus(GiftStatusEnum.CANCELLED.getCode());
        itemGift.setCancelTime(new Date());
        itemGift.setUpdateTime(new Date());
        this.updateById(itemGift);

        return true;
    }

    @Override
    public Page<ItemGiftListVO> queryGiftList(Page<ItemGift> page, String userId, Integer type) {
        LambdaQueryWrapper<ItemGift> queryWrapper = new LambdaQueryWrapper<>();

        if (type != null && type == 1) {
            // 我送出的
            queryWrapper.eq(ItemGift::getSenderUserId, userId);
        } else if (type != null && type == 2) {
            // 我收到的：定向赠送给我的 + 我领取过的赠送（领取明细反查，含公开赠送多人领取）
            queryWrapper.and(w -> w.eq(ItemGift::getReceiverUserId, userId)
                    .eq(ItemGift::getGiftType, GiftTypeEnum.DIRECT.getCode()));
            LambdaQueryWrapper<ItemGiftReceive> receiveWrapper = new LambdaQueryWrapper<>();
            receiveWrapper.eq(ItemGiftReceive::getUserId, userId);
            List<ItemGiftReceive> receiveList = itemGiftReceiveMapper.selectList(receiveWrapper);
            List<String> receivedGiftIds = receiveList.stream()
                    .map(ItemGiftReceive::getGiftId)
                    .filter(ObjectUtil::isNotEmpty)
                    .distinct()
                    .collect(Collectors.toList());
            if (CollectionUtil.isNotEmpty(receivedGiftIds)) {
                queryWrapper.or(w -> w.in(ItemGift::getId, receivedGiftIds));
            }
        }

        queryWrapper.orderByDesc(ItemGift::getCreateTime);
        Page<ItemGift> resultPage = this.page(page, queryWrapper);

        Page<ItemGiftListVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        List<ItemGiftListVO> voList = new java.util.ArrayList<>();
        Set<String> userIds = new HashSet<>();

        for (ItemGift gift : resultPage.getRecords()) {
            ItemGiftListVO vo = convertGiftVO(gift);

            if (ObjectUtil.isNotEmpty(gift.getSenderUserId())) {
                userIds.add(gift.getSenderUserId());
            }
            if (ObjectUtil.isNotEmpty(gift.getReceiverUserId())) {
                userIds.add(gift.getReceiverUserId());
            }

            voList.add(vo);
        }

        // 批量组装赠送者/接收者昵称与头像，避免循环内逐条查询用户
        if (CollectionUtil.isNotEmpty(userIds)) {
            Map<String, AppUser> userMap = new HashMap<>();
            for (AppUser user : appUserService.listByIds(userIds)) {
                userMap.put(user.getId(), user);
            }
            for (ItemGiftListVO vo : voList) {
                AppUser sender = userMap.get(vo.getSenderUserId());
                vo.setSenderNickname(getDisplayNickname(sender));
                vo.setSenderAvatar(sender == null ? null : sender.getAvatar());
                AppUser receiver = userMap.get(vo.getReceiverUserId());
                vo.setReceiverNickname(getDisplayNickname(receiver));
                vo.setReceiverAvatar(receiver == null ? null : receiver.getAvatar());
            }
        }

        voPage.setRecords(voList);
        return voPage;
    }

    /** 赠送单 → 列表VO 基础转换（类型/状态名 + 物品信息），列表与详情共用 */
    private ItemGiftListVO convertGiftVO(ItemGift gift) {
        ItemGiftListVO vo = cn.hutool.core.bean.BeanUtil.copyProperties(gift, ItemGiftListVO.class);

        if (gift.getGiftType() != null) {
            for (GiftTypeEnum enumItem : GiftTypeEnum.values()) {
                if (enumItem.getCode() == gift.getGiftType()) {
                    vo.setGiftTypeName(enumItem.getName());
                    break;
                }
            }
        }

        if (gift.getStatus() != null) {
            for (GiftStatusEnum enumItem : GiftStatusEnum.values()) {
                if (enumItem.getCode() == gift.getStatus()) {
                    vo.setStatusName(enumItem.getName());
                    break;
                }
            }
        }

        if (ObjectUtil.isNotEmpty(gift.getItemId())) {
            ItemInfo itemInfo = itemInfoService.getById(gift.getItemId());
            vo.setItemInfo(itemInfo);
            if (itemInfo != null) {
                vo.setItemName(itemInfo.getName());
                vo.setItemIcon(itemInfo.getIcon());
                vo.setItemLevel(itemInfo.getLevel());
            }
        }
        return vo;
    }

    @Override
    public ItemGiftListVO queryGiftDetail(String giftId, String currentUserId) {
        ItemGift gift = this.getById(giftId);
        if (gift == null) {
            throw new AppException(ExceptionEnum.GIFT_NOT_EXIST);
        }
        // 权限：寄件人/受赠人可见（未领取的公开赠送 receiverUserId 为空，仅寄件人可见）
        boolean isSender = Objects.equals(gift.getSenderUserId(), currentUserId);
        boolean isReceiver = ObjectUtil.isNotEmpty(gift.getReceiverUserId())
                && Objects.equals(gift.getReceiverUserId(), currentUserId);
        if (!isSender && !isReceiver) {
            throw new AppException(ExceptionEnum.GIFT_NOT_EXIST);
        }

        ItemGiftListVO vo = convertGiftVO(gift);

        // 双方昵称与头像
        Set<String> userIds = new HashSet<>();
        if (ObjectUtil.isNotEmpty(gift.getSenderUserId())) {
            userIds.add(gift.getSenderUserId());
        }
        if (ObjectUtil.isNotEmpty(gift.getReceiverUserId())) {
            userIds.add(gift.getReceiverUserId());
        }
        if (CollectionUtil.isNotEmpty(userIds)) {
            for (AppUser user : appUserService.listByIds(userIds)) {
                if (Objects.equals(user.getId(), gift.getSenderUserId())) {
                    vo.setSenderNickname(getDisplayNickname(user));
                    vo.setSenderAvatar(user.getAvatar());
                } else if (Objects.equals(user.getId(), gift.getReceiverUserId())) {
                    vo.setReceiverNickname(getDisplayNickname(user));
                    vo.setReceiverAvatar(user.getAvatar());
                }
            }
        }

        // 领取记录
        vo.setReceiveRecords(queryGiftReceivers(giftId));

        // 实物发运单（giftId 关联，bizType=2）
        List<ItemStorageIn> ships = itemStorageInMapper.selectList(new LambdaQueryWrapper<ItemStorageIn>()
                .eq(ItemStorageIn::getGiftId, giftId)
                .eq(ItemStorageIn::getBizType, StorageInConstant.BIZ_MOVE)
                .orderByDesc(ItemStorageIn::getCreateTime));
        List<Map<String, Object>> shipRecords = new ArrayList<>();
        for (ItemStorageIn ship : ships) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", ship.getId());
            m.put("depositNo", ship.getDepositNo());
            m.put("status", ship.getStatus());
            m.put("statusDesc", StorageInConstant.statusDesc(ship.getStatus()));
            m.put("expressNo", ship.getExpressNo());
            m.put("quantity", ship.getQuantity());
            m.put("receiverStationId", ship.getReceiverStationId());
            m.put("receiverStationType", ship.getReceiverStationType());
            m.put("createTime", ship.getCreateTime());
            shipRecords.add(m);
        }
        vo.setShipRecords(shipRecords);
        return vo;
    }

    @Override
    public List<ItemGiftReceiveVO> queryGiftReceivers(String giftId) {
        LambdaQueryWrapper<ItemGiftReceive> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ItemGiftReceive::getGiftId, giftId);
        queryWrapper.orderByDesc(ItemGiftReceive::getReceiveTime);
        List<ItemGiftReceive> receiveList = itemGiftReceiveMapper.selectList(queryWrapper);

        List<ItemGiftReceiveVO> voList = new java.util.ArrayList<>();
        if (CollectionUtil.isEmpty(receiveList)) {
            return voList;
        }

        // 批量查询领取者昵称与头像
        Set<String> userIds = new HashSet<>();
        for (ItemGiftReceive receive : receiveList) {
            if (ObjectUtil.isNotEmpty(receive.getUserId())) {
                userIds.add(receive.getUserId());
            }
        }
        Map<String, AppUser> userMap = new HashMap<>();
        if (CollectionUtil.isNotEmpty(userIds)) {
            for (AppUser user : appUserService.listByIds(userIds)) {
                userMap.put(user.getId(), user);
            }
        }

        for (ItemGiftReceive receive : receiveList) {
            ItemGiftReceiveVO vo = new ItemGiftReceiveVO();
            vo.setId(receive.getId());
            vo.setUserId(receive.getUserId());
            vo.setQuantity(receive.getQuantity());
            vo.setReceiveTime(receive.getReceiveTime());
            AppUser user = userMap.get(receive.getUserId());
            if (user != null) {
                vo.setNickname(getDisplayNickname(user));
                vo.setAvatar(user.getAvatar());
            }
            voList.add(vo);
        }
        return voList;
    }

    private String getDisplayNickname(AppUser user) {
        if (user == null) {
            return "";
        }
        return ObjectUtil.isNotEmpty(user.getNickname()) ? user.getNickname() : user.getUsername();
    }

    @Override
    public ItemGift getGiftByCode(String giftCode) {
        LambdaQueryWrapper<ItemGift> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ItemGift::getGiftCode, giftCode);
        queryWrapper.last("limit 1");
        ItemGift itemGift = this.getOne(queryWrapper);
        // 扫码领取需要展示物品信息与赠送人昵称
        if (itemGift != null) {
            if (ObjectUtil.isNotEmpty(itemGift.getItemId())) {
                ItemInfo itemInfo = itemInfoService.getById(itemGift.getItemId());
                if (itemInfo == null && ObjectUtil.isNotEmpty(itemGift.getOriId())) {
                    ItemSync itemSync = itemSyncService.getItemSyncByOriIdAndUserId(
                            itemGift.getOriId(), itemGift.getSenderUserId());
                    if (itemSync != null) {
                        itemInfo = itemInfoService.getById(itemSync.getItemId());
                    }
                }
                itemGift.setItemInfo(itemInfo);
            }
            if (ObjectUtil.isNotEmpty(itemGift.getSenderUserId())) {
                AppUser sender = appUserService.getById(itemGift.getSenderUserId());
                if (sender != null) {
                    String nickname = ObjectUtil.isNotEmpty(sender.getNickname())
                            ? sender.getNickname() : sender.getUsername();
                    itemGift.setSenderNickname(nickname);
                }
            }
        }
        return itemGift;
    }

    private String generateGiftCode() {
        return RandomUtil.randomString(8).toUpperCase();
    }

    private String generateQrCode(String giftCode) {
        return "QR_GIFT_CODE:" + giftCode;
    }

    private int countUserReceivedGift(String giftId, String userId) {
        LambdaQueryWrapper<ItemGiftReceive> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ItemGiftReceive::getGiftId, giftId);
        queryWrapper.eq(ItemGiftReceive::getUserId, userId);
        return Math.toIntExact(itemGiftReceiveMapper.selectCount(queryWrapper));
    }

    private void saveGiftReceiveRecord(String giftId, String userId, int quantity) {
        ItemGiftReceive receiveRecord = new ItemGiftReceive();
        receiveRecord.setGiftId(giftId);
        receiveRecord.setUserId(userId);
        receiveRecord.setQuantity(quantity);
        receiveRecord.setReceiveTime(new Date());
        receiveRecord.setCreateTime(new Date());
        itemGiftReceiveMapper.insert(receiveRecord);
    }

}