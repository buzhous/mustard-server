package org.jeecg.modules.app.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.app.bean.enums.ExceptionEnum;
import org.jeecg.modules.app.bean.enums.SyncStatusEnum;
import org.jeecg.modules.app.bean.exception.AppException;
import org.jeecg.modules.app.bean.vo.ruins.ItemRuinsListVO;
import org.jeecg.modules.app.constant.LockConstant;
import org.jeecg.modules.app.entity.ItemRecycle;
import org.jeecg.modules.app.entity.ItemSync;
import org.jeecg.modules.app.entity.ItemUserInventory;
import org.jeecg.modules.app.mapper.ItemRecycleMapper;
import org.jeecg.modules.app.mapper.ItemSyncMapper;
import org.jeecg.modules.app.service.IItemRecycleService;
import org.jeecg.modules.app.service.IItemUserInventoryService;
import org.jeecg.modules.app.utils.FieldElementUtil;
import org.jeecg.boot.starter.lock.annotation.JLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户独立回收站实现：删除物品进入 tbl_item_recycle（与殷墟隔离），
 * 恢复=撤回删除（同步记录/库存复原），彻底删除=物理删除
 */
@Slf4j
@Service
public class ItemRecycleServiceImpl extends ServiceImpl<ItemRecycleMapper, ItemRecycle>
        implements IItemRecycleService {

    /**
     * 回收站保留天数：超期未恢复的删除记录由定时任务彻底清除
     */
    public static final int RECYCLE_KEEP_DAYS = 30;

    private static final int STATUS_IN = 0;
    private static final int STATUS_RESTORED = 2;

    @Autowired
    private ItemSyncMapper itemSyncMapper;

    @Autowired
    private IItemUserInventoryService itemUserInventoryService;

    @Override
    public boolean dropToRecycle(ItemSync itemSync) {
        ItemRecycle recycle = BeanUtil.copyProperties(itemSync, ItemRecycle.class);
        // 每条删除独立成记录（不按物品累加），恢复时按记录数量回补
        recycle.setId(null);
        recycle.setStatus(STATUS_IN);
        recycle.setCreateTime(DateUtil.date());
        return this.save(recycle);
    }

    @Override
    public IPage<ItemRuinsListVO> queryMyRecyclePage(Integer page, Integer size, String userId) {
        // 本人删除、保留期内的记录，按删除时间倒序
        Date expireTime = DateUtil.offsetDay(DateUtil.date(), -RECYCLE_KEEP_DAYS);
        QueryWrapper<ItemRecycle> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(ItemRecycle::getStatus, STATUS_IN)
                .eq(ItemRecycle::getUserId, userId)
                .ge(ItemRecycle::getCreateTime, expireTime)
                .orderByDesc(ItemRecycle::getCreateTime);
        Page<ItemRecycle> recyclePage = this.baseMapper.selectPage(new Page<>(page, size), queryWrapper);
        IPage<ItemRuinsListVO> iPageVO = new Page<>(recyclePage.getCurrent(), recyclePage.getSize(), recyclePage.getTotal());
        List<ItemRuinsListVO> voList = recyclePage.getRecords().stream()
                .map(r -> {
                    ItemRuinsListVO vo = FieldElementUtil.convertObject(r, new ItemRuinsListVO());
                    vo.setRuinsId(String.valueOf(r.getId()));
                    vo.setCategory(r.getCategoryId());
                    vo.setCreateTime(r.getCreateTime());
                    return vo;
                })
                .collect(Collectors.toList());
        iPageVO.setRecords(voList);
        return iPageVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @JLock(lockKey = "'" + LockConstant.ITEM_RECYCLE_LOCK + "' + #recycleId")
    public boolean restoreFromRecycle(String userId, String recycleId, String itemId) {
        ItemRecycle recycle = queryRecycleByRecycleId(recycleId, itemId);
        if (recycle == null || !userId.equals(recycle.getUserId())) {
            throw new AppException(ExceptionEnum.ITEM_NOT_EXIST);
        }
        if (recycle.getStatus() == null || recycle.getStatus() != STATUS_IN) {
            throw new AppException(ExceptionEnum.ITEM_NOT_EXIST);
        }
        // 恢复原物品同步记录与库存
        ItemSync exist = itemSyncMapper.selectOne(new QueryWrapper<ItemSync>().lambda()
                .eq(ItemSync::getOriId, recycle.getOriId())
                .eq(ItemSync::getUserId, userId)
                .eq(ItemSync::getItemId, recycle.getItemId())
                .last("limit 1"));
        if (exist != null) {
            int recycleQuantity = ObjectUtil.defaultIfNull(recycle.getQuantity(), 0);
            ItemSync update = new ItemSync();
            update.setId(exist.getId());
            update.setVersion(ObjectUtil.defaultIfNull(exist.getVersion(), 0) + 1);
            if (SyncStatusEnum.DESTROYED.getCode() == ObjectUtil.defaultIfNull(exist.getStatus(), -1)) {
                // 全量删除：恢复状态即可（删除时未扣库存与记录数量）
                update.setStatus(SyncStatusEnum.SUCCESS.getCode());
                itemSyncMapper.updateById(update);
            } else {
                // 部分删除：同步记录与库存数量累加
                update.setQuantity(ObjectUtil.defaultIfNull(exist.getQuantity(), 0) + recycleQuantity);
                itemSyncMapper.updateById(update);
                ItemUserInventory inventory = itemUserInventoryService.queryUserItem(userId, exist.getItemId());
                if (inventory != null) {
                    ItemUserInventory invUpdate = new ItemUserInventory();
                    invUpdate.setId(inventory.getId());
                    invUpdate.setQuantity(ObjectUtil.defaultIfNull(inventory.getQuantity(), 0) + recycleQuantity);
                    itemUserInventoryService.updateById(invUpdate);
                }
            }
        }
        // 回收站记录标记已恢复
        ItemRecycle restored = new ItemRecycle();
        restored.setId(recycle.getId());
        restored.setStatus(STATUS_RESTORED);
        restored.setUpdateTime(DateUtil.date());
        return this.updateById(restored);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @JLock(lockKey = "'" + LockConstant.ITEM_RECYCLE_LOCK + "' + #recycleId")
    public boolean purgeRecycle(String userId, String recycleId, String itemId) {
        ItemRecycle recycle = queryRecycleByRecycleId(recycleId, itemId);
        if (recycle == null || !userId.equals(recycle.getUserId())) {
            throw new AppException(ExceptionEnum.ITEM_NOT_EXIST);
        }
        if (recycle.getStatus() == null || recycle.getStatus() != STATUS_IN) {
            throw new AppException(ExceptionEnum.ITEM_NOT_EXIST);
        }
        this.baseMapper.deleteById(recycle.getId());
        // 全量删除对应的同步记录（已销毁状态）一并彻底删除；部分删除的正常记录保留
        itemSyncMapper.delete(new QueryWrapper<ItemSync>().lambda()
                .eq(ItemSync::getOriId, recycle.getOriId())
                .eq(ItemSync::getUserId, userId)
                .eq(ItemSync::getItemId, recycle.getItemId())
                .eq(ItemSync::getStatus, SyncStatusEnum.DESTROYED.getCode()));
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int purgeExpiredRecycle() {
        Date expireTime = DateUtil.offsetDay(DateUtil.date(), -RECYCLE_KEEP_DAYS);
        List<ItemRecycle> expired = this.list(new QueryWrapper<ItemRecycle>().lambda()
                .eq(ItemRecycle::getStatus, STATUS_IN)
                .lt(ItemRecycle::getCreateTime, expireTime));
        int count = 0;
        for (ItemRecycle recycle : expired) {
            this.baseMapper.deleteById(recycle.getId());
            itemSyncMapper.delete(new QueryWrapper<ItemSync>().lambda()
                    .eq(ItemSync::getOriId, recycle.getOriId())
                    .eq(ItemSync::getUserId, recycle.getUserId())
                    .eq(ItemSync::getItemId, recycle.getItemId())
                    .eq(ItemSync::getStatus, SyncStatusEnum.DESTROYED.getCode()));
            count++;
        }
        return count;
    }

    private ItemRecycle queryRecycleByRecycleId(String recycleId, String itemId) {
        QueryWrapper<ItemRecycle> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(ItemRecycle::getId, recycleId)
                .eq(ObjectUtil.isNotEmpty(itemId), ItemRecycle::getItemId, itemId);
        queryWrapper.last("limit 1");
        return this.getOne(queryWrapper);
    }

}
