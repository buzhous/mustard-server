package org.jeecg.modules.app.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.app.entity.PointsFlow;
import org.jeecg.modules.app.entity.UserAmount;
import org.jeecg.modules.app.mapper.UserAmountMapper;
import org.jeecg.modules.app.service.IPointsFlowService;
import org.jeecg.modules.app.service.IUserAmountService;
import org.jeecg.modules.app.entity.user.AppUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static org.jeecg.modules.app.service.PointsConstants.REGISTER_GIFT_POINTS;


@Slf4j
@Service
public class UserAmountServiceImpl extends ServiceImpl<UserAmountMapper, UserAmount> implements IUserAmountService {

    @Autowired
    private IPointsFlowService iPointsFlowService;

    @Override
    public UserAmount queryByUserId(String userId) {
        QueryWrapper<UserAmount> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(UserAmount::getUserId, userId);
        queryWrapper.last("limit 1");
        return this.getOne(queryWrapper);
    }

    @Override
    public boolean createUserAmount(AppUser appUser) {
        if (ObjectUtil.isEmpty(appUser) || StrUtil.isEmpty(appUser.getId())) {
            return false;
        }

        UserAmount userAmount = this.queryByUserId(appUser.getId());
        if (ObjectUtil.isNotEmpty(userAmount)) {
            return true;
        }

        userAmount = new UserAmount();
        // 注册赠送积分
        userAmount.setGoldCoinAmount(REGISTER_GIFT_POINTS);
        userAmount.setFairyStoneAmount(0);
        userAmount.setUserId(appUser.getId());
        userAmount.setIsLock(0);
        userAmount.setCreateTime(DateUtil.date());
        userAmount.setUpdateTime(DateUtil.date());

        boolean saved = this.save(userAmount);
        if (saved) {
            // 记录注册赠送流水
            this.saveFlow(appUser.getId(), PointsFlow.TYPE_REGISTER_GIFT, REGISTER_GIFT_POINTS,
                    REGISTER_GIFT_POINTS, "注册赠送积分", null);
        }
        return saved;
    }

    @Override
    public Integer addGoldCoin(String userId, int points, int flowType, String remark, String refId) {
        if (StrUtil.isEmpty(userId) || points <= 0) {
            return null;
        }
        // SQL 原子自增，避免并发读改写丢失更新
        int rows = this.baseMapper.addGoldCoin(userId, points);
        if (rows <= 0) {
            log.warn("addGoldCoin failed, userId={}, points={}", userId, points);
            return null;
        }
        UserAmount latest = this.queryByUserId(userId);
        int balanceAfter = latest == null || latest.getGoldCoinAmount() == null ? 0 : latest.getGoldCoinAmount();
        this.saveFlow(userId, flowType, points, balanceAfter, remark, refId);
        return balanceAfter;
    }

    private void saveFlow(String userId, int type, int points, int balanceAfter, String remark, String refId) {
        try {
            PointsFlow flow = new PointsFlow();
            flow.setUserId(userId);
            flow.setType(type);
            flow.setPoints(points);
            flow.setBalanceAfter(balanceAfter);
            flow.setRemark(remark);
            flow.setRefId(refId);
            flow.setCreateTime(DateUtil.date());
            flow.setUpdateTime(DateUtil.date());
            iPointsFlowService.save(flow);
        } catch (Exception e) {
            // 流水失败不影响主流程
            log.error("savePointsFlow error, userId={}", userId, e);
        }
    }

}
