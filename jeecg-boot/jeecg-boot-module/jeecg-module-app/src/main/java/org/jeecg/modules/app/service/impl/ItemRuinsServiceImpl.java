package org.jeecg.modules.app.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.boot.starter.lock.annotation.JLock;
import org.jeecg.modules.app.bean.enums.ExceptionEnum;
import org.jeecg.modules.app.bean.enums.RuinsStatusEnum;
import org.jeecg.modules.app.bean.exception.AppException;
import org.jeecg.modules.app.bean.vo.queue.ItemSyncReqVO;
import org.jeecg.modules.app.bean.vo.ruins.ItemRuinsListVO;
import org.jeecg.modules.app.constant.LockConstant;
import org.jeecg.modules.app.entity.ItemRuins;
import org.jeecg.modules.app.entity.ItemSync;
import org.jeecg.modules.app.entity.UserCoupon;
import org.jeecg.modules.app.mapper.ItemRuinsMapper;
import org.jeecg.modules.app.service.IItemRuinsService;
import org.jeecg.modules.app.service.IItemSyncService;
import org.jeecg.modules.app.service.IUserCouponService;
import org.jeecg.modules.app.utils.FieldElementUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@Service
public class ItemRuinsServiceImpl extends ServiceImpl<ItemRuinsMapper, ItemRuins> implements IItemRuinsService {

    @Autowired
    IItemSyncService itemSyncService;

    @Autowired
    IUserCouponService userCouponService;

    @Override
    public IPage<ItemRuinsListVO> queryNotPickupRuinsPage(Integer page, Integer size, String userId) {
        // 查询分页列表
        QueryWrapper<ItemRuins> queryWrapper = new QueryWrapper<>();
        // 获取未拾取
        queryWrapper.lambda().eq(ItemRuins::getStatus, RuinsStatusEnum.NOT_PICKUP.getCode());
        // 管理员指定拾取者的记录（pickup_user 非空）仅对被指定用户可见
        queryWrapper.lambda().and(w -> w.isNull(ItemRuins::getPickupUser)
                .or().eq(ItemRuins::getPickupUser, "")
                .or().eq(ItemRuins::getPickupUser, userId));
        Page<ItemRuins> page1 = new Page<>(page, size);
        IPage<ItemRuins> ruinsPage = this.baseMapper.selectPage(page1, queryWrapper);
        // 转换分页数据到IPage<ItemRuinsListVO>（tags为JSON字符串，需通过FieldElementUtil解析为List）
        IPage<ItemRuinsListVO> iPageVO = new Page<>(ruinsPage.getCurrent(), ruinsPage.getSize(), ruinsPage.getTotal());
        List<ItemRuinsListVO> voList = ruinsPage.getRecords().stream()
                .map(r -> {
                    ItemRuinsListVO vo = FieldElementUtil.convertObject(r, new ItemRuinsListVO());
                    // 殷墟记录主键，app 拾取入参 ruinsId
                    vo.setRuinsId(String.valueOf(r.getId()));
                    return vo;
                })
                .collect(Collectors.toList());
        iPageVO.setRecords(voList);
        return iPageVO;
    }

    @Override
    public ItemRuins queryRuinByItemId(String itemId, String oriId) {
        QueryWrapper<ItemRuins> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ItemRuins::getItemId, itemId);
        queryWrapper.lambda().eq(ItemRuins::getOriId, oriId);
        queryWrapper.last("limit 1");
        return this.getOne(queryWrapper);
    }

    @Override
    public ItemRuins queryRuinByRuinsId(String ruinsId, String itemId) {
        QueryWrapper<ItemRuins> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ItemRuins::getItemId, itemId);
        queryWrapper.lambda().eq(ItemRuins::getId, ruinsId);
        queryWrapper.last("limit 1");
        return this.getOne(queryWrapper);
    }

    @Override
    public boolean dropToRuins(ItemSync itemSync) {
        ItemRuins itemRuins = BeanUtil.copyProperties(itemSync, ItemRuins.class);
        ItemRuins ruinExist = queryRuinByItemId(itemRuins.getItemId(), itemRuins.getOriId());
        if (ruinExist == null) {
            itemRuins.setStatus(RuinsStatusEnum.NOT_PICKUP.getCode());
            return this.saveOrUpdate(itemRuins);
        } else {
            itemRuins.setStatus(RuinsStatusEnum.NOT_PICKUP.getCode());
            // 同一物品多次销毁丢入殷墟：未拾取记录累加库存数量
            if (ruinExist.getStatus() != null
                    && ruinExist.getStatus() == RuinsStatusEnum.NOT_PICKUP.getCode()
                    && itemRuins.getQuantity() != null) {
                int existQuantity = ruinExist.getQuantity() == null ? 0 : ruinExist.getQuantity();
                itemRuins.setQuantity(existQuantity + itemRuins.getQuantity());
            }
            return this.updateById(itemRuins);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @JLock(lockKey = "'" + LockConstant.ITEM_PICKUP_LOCK + "' + #runsId")
    public ItemSync pickup(String runsId, String itemId, String userId) {
        ItemRuins ruins = queryRuinByRuinsId(runsId, itemId);
        if (ruins == null) {
            throw new AppException(ExceptionEnum.ITEM_NOT_EXIST);
        }

        // a) 原子锁定一张可用殷墟拾取券（FOR UPDATE 行锁，事务内生效）
        UserCoupon userCoupon = userCouponService.lockAvailableRuinsPickupCoupon(userId);
        if (userCoupon == null) {
            throw new AppException(ExceptionEnum.RUINS_PICKUP_COUPON_NOT_ENOUGH);
        }

        // b) 原子翻转殷墟行：仅未拾取可更新，影响行数为0表示已被别人抢走，事务回滚券不扣
        int rows = this.baseMapper.pickupRuins(ruins.getId(), userId, DateUtil.date());
        if (rows == 0) {
            throw new AppException(ExceptionEnum.RUINS_ALREADY_PICKED);
        }

        // c) 物品同步进拾取者名下（oriId保持，userId换成拾取者，数量取殷墟记录）
        ItemSyncReqVO syncVO = FieldElementUtil.convertObject(ruins, new ItemSyncReqVO());
        syncVO.setUserId(userId);
        syncVO.setOriId(ruins.getOriId());
        syncVO.setQuantity(ruins.getQuantity());
        ItemSync itemSync = itemSyncService.syncFromRuins(syncVO);
        // 更新殷墟记录关联新itemId
        this.baseMapper.updateRuinsItemId(ruins.getId(), itemSync.getItemId());

        // d) 标记券已使用
        userCouponService.useCoupon(userId, userCoupon.getId(), runsId);
        return itemSync;
    }

}