package org.jeecg.modules.app.controller.points;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.app.bean.exception.AppException;
import org.jeecg.modules.app.bean.enums.ExceptionEnum;
import org.jeecg.modules.app.bean.vo.points.MockPayReqVO;
import org.jeecg.modules.app.bean.vo.points.PointsAccountVO;
import org.jeecg.modules.app.bean.vo.points.PointsFlowVO;
import org.jeecg.modules.app.bean.vo.points.RechargeCreateReqVO;
import org.jeecg.modules.app.bean.vo.points.RechargeCreateResultVO;
import org.jeecg.modules.app.bean.vo.points.RechargePlanVO;
import org.jeecg.modules.app.entity.PointsFlow;
import org.jeecg.modules.app.entity.RechargeOrder;
import org.jeecg.modules.app.entity.UserAmount;
import org.jeecg.modules.app.entity.user.AppUser;
import org.jeecg.modules.app.mapper.RechargeOrderMapper;
import org.jeecg.modules.app.service.IPointsFlowService;
import org.jeecg.modules.app.service.IUserAmountService;
import org.jeecg.modules.app.utils.AppAuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;


@Tag(name = "积分充值")
@RestController
@RequestMapping("/app/points")
@Slf4j
public class AppPointsController {

    @Autowired
    private IUserAmountService iUserAmountService;

    @Autowired
    private IPointsFlowService iPointsFlowService;

    @Autowired
    private RechargeOrderMapper rechargeOrderMapper;

    @Operation(summary = "查询积分账户")
    @GetMapping(value = "/account")
    public Result<PointsAccountVO> account() {
        AppUser loginUser = AppAuthUtil.getUserInfo();
        if (ObjectUtil.isEmpty(loginUser)) {
            throw new AppException(ExceptionEnum.ACCESS_DENIED);
        }
        UserAmount userAmount = iUserAmountService.queryByUserId(loginUser.getId());
        if (ObjectUtil.isEmpty(userAmount)) {
            // 老用户无资金账户时补建（赠送注册积分）
            iUserAmountService.createUserAmount(loginUser);
            userAmount = iUserAmountService.queryByUserId(loginUser.getId());
        }
        int points = userAmount == null || userAmount.getGoldCoinAmount() == null ? 0 : userAmount.getGoldCoinAmount();
        return Result.OK("", PointsAccountVO.builder().points(points).build());
    }

    @Operation(summary = "充值套餐列表")
    @GetMapping(value = "/plans")
    public Result<List<RechargePlanVO>> plans() {
        AppUser loginUser = AppAuthUtil.getUserInfo();
        if (ObjectUtil.isEmpty(loginUser)) {
            throw new AppException(ExceptionEnum.ACCESS_DENIED);
        }
        return Result.OK("", buildPlans());
    }

    @Operation(summary = "创建充值订单（预下单）")
    @PostMapping(value = "/recharge/create")
    public Result<RechargeCreateResultVO> rechargeCreate(@RequestBody RechargeCreateReqVO vo) {
        AppUser loginUser = AppAuthUtil.getUserInfo();
        if (ObjectUtil.isEmpty(loginUser)) {
            throw new AppException(ExceptionEnum.ACCESS_DENIED);
        }
        if (vo == null || StrUtil.isEmpty(vo.getPlanId())) {
            return Result.error("套餐不能为空");
        }
        Integer channel = vo.getChannel();
        if (channel == null || (channel != RechargeOrder.CHANNEL_WECHAT && channel != RechargeOrder.CHANNEL_ALIPAY)) {
            return Result.error("支付渠道不支持");
        }
        RechargePlanVO plan = findPlan(vo.getPlanId());
        if (plan == null) {
            return Result.error("套餐不存在");
        }

        // 保证资金账户存在
        UserAmount userAmount = iUserAmountService.queryByUserId(loginUser.getId());
        if (ObjectUtil.isEmpty(userAmount)) {
            iUserAmountService.createUserAmount(loginUser);
        }

        // 创建待支付订单
        RechargeOrder order = new RechargeOrder();
        order.setUserId(loginUser.getId());
        order.setOrderNo(genOrderNo());
        order.setAmountCents(plan.getAmountCents());
        order.setPoints(plan.getPoints());
        order.setChannel(channel);
        order.setStatus(RechargeOrder.STATUS_PENDING);
        order.setCreateTime(DateUtil.date());
        order.setUpdateTime(DateUtil.date());
        rechargeOrderMapper.insert(order);

        return Result.OK("", RechargeCreateResultVO.builder()
                .orderNo(order.getOrderNo())
                .amountCents(order.getAmountCents())
                .points(order.getPoints())
                .channel(channel)
                .build());
    }

    @Operation(summary = "模拟支付回调（服务端模拟支付成功）")
    @PostMapping(value = "/recharge/mockPay")
    public Result<PointsAccountVO> mockPay(@RequestBody MockPayReqVO vo) {
        AppUser loginUser = AppAuthUtil.getUserInfo();
        if (ObjectUtil.isEmpty(loginUser)) {
            throw new AppException(ExceptionEnum.ACCESS_DENIED);
        }
        if (vo == null || StrUtil.isEmpty(vo.getOrderNo())) {
            return Result.error("订单号不能为空");
        }
        RechargeOrder order = queryByOrderNo(vo.getOrderNo());
        if (order == null || !StrUtil.equals(order.getUserId(), loginUser.getId())) {
            return Result.error("订单不存在");
        }
        if (order.getStatus() != null && order.getStatus() == RechargeOrder.STATUS_SUCCESS) {
            // 幂等：已支付直接返回当前余额
            return Result.OK("", buildAccountVO(loginUser.getId()));
        }
        if (order.getStatus() == null || order.getStatus() != RechargeOrder.STATUS_PENDING) {
            return Result.error("订单状态异常");
        }

        // 乐观锁定单为支付成功，防止重复回调重复加积分
        int rows = rechargeOrderMapper.casUpdateStatus(order.getOrderNo(), RechargeOrder.STATUS_PENDING, RechargeOrder.STATUS_SUCCESS);
        if (rows <= 0) {
            // 并发下已被其他回调处理
            return Result.OK("", buildAccountVO(loginUser.getId()));
        }

        // 积分入账 + 流水
        Integer balance = iUserAmountService.addGoldCoin(
                loginUser.getId(),
                order.getPoints() == null ? 0 : order.getPoints(),
                PointsFlow.TYPE_RECHARGE,
                channelDesc(order.getChannel()) + "充值",
                order.getOrderNo());
        if (balance == null) {
            log.error("mockPay addGoldCoin failed, orderNo={}, userId={}", order.getOrderNo(), loginUser.getId());
            return Result.error("积分入账失败，请联系客服");
        }

        log.info("mockPay success, orderNo={}, channel={}, points={}, balance={}",
                order.getOrderNo(), order.getChannel(), order.getPoints(), balance);
        return Result.OK("", PointsAccountVO.builder().points(balance).build());
    }

    @Operation(summary = "积分流水（type=10 充值记录，type=30 消费记录，不传查全部）")
    @GetMapping(value = "/flow")
    public Result<Page<PointsFlowVO>> flow(HttpServletRequest request) {
        AppUser loginUser = AppAuthUtil.getUserInfo();
        if (ObjectUtil.isEmpty(loginUser)) {
            throw new AppException(ExceptionEnum.ACCESS_DENIED);
        }
        String type = request.getParameter("type");
        long pageNo = parseLong(request.getParameter("pageNo"), 1);
        long pageSize = parseLong(request.getParameter("pageSize"), 20);

        QueryWrapper<PointsFlow> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PointsFlow::getUserId, loginUser.getId());
        if (StrUtil.isNotEmpty(type)) {
            queryWrapper.lambda().eq(PointsFlow::getType, Integer.valueOf(type));
        }
        queryWrapper.lambda().orderByDesc(PointsFlow::getCreateTime);
        queryWrapper.lambda().orderByDesc(PointsFlow::getId);

        Page<PointsFlow> page = iPointsFlowService.page(new Page<>(pageNo, pageSize), queryWrapper);

        Page<PointsFlowVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<PointsFlowVO> records = new ArrayList<>();
        for (PointsFlow flow : page.getRecords()) {
            PointsFlowVO flowVO = new PointsFlowVO();
            flowVO.setId(flow.getId());
            flowVO.setType(flow.getType());
            flowVO.setPoints(flow.getPoints());
            flowVO.setBalanceAfter(flow.getBalanceAfter());
            flowVO.setRemark(flow.getRemark());
            flowVO.setRefId(flow.getRefId());
            flowVO.setCreateTime(flow.getCreateTime());
            records.add(flowVO);
        }
        voPage.setRecords(records);
        return Result.OK(voPage);
    }

    // ====================================================================
    // 内部方法
    // ====================================================================

    private PointsAccountVO buildAccountVO(String userId) {
        UserAmount userAmount = iUserAmountService.queryByUserId(userId);
        int points = userAmount == null || userAmount.getGoldCoinAmount() == null ? 0 : userAmount.getGoldCoinAmount();
        return PointsAccountVO.builder().points(points).build();
    }

    private RechargeOrder queryByOrderNo(String orderNo) {
        QueryWrapper<RechargeOrder> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(RechargeOrder::getOrderNo, orderNo);
        queryWrapper.last("limit 1");
        return rechargeOrderMapper.selectOne(queryWrapper);
    }

    private String genOrderNo() {
        return "R" + DateUtil.format(DateUtil.date(), "yyyyMMddHHmmssSSS") + RandomUtil.randomNumbers(4);
    }

    private String channelDesc(Integer channel) {
        if (channel != null && channel == RechargeOrder.CHANNEL_ALIPAY) {
            return "支付宝";
        }
        return "微信";
    }

    private long parseLong(String value, long defaultValue) {
        try {
            if (StrUtil.isNotEmpty(value)) {
                return Long.parseLong(value);
            }
        } catch (NumberFormatException ignored) {
        }
        return defaultValue;
    }

    private List<RechargePlanVO> buildPlans() {
        List<RechargePlanVO> plans = new ArrayList<>();
        plans.add(RechargePlanVO.builder().planId("P1").points(100).amountCents(100).build());
        plans.add(RechargePlanVO.builder().planId("P2").points(600).amountCents(600).build());
        plans.add(RechargePlanVO.builder().planId("P3").points(1200).amountCents(1200).build());
        plans.add(RechargePlanVO.builder().planId("P4").points(5000).amountCents(5000).build());
        plans.add(RechargePlanVO.builder().planId("P5").points(9800).amountCents(9800).tag("超值").build());
        plans.add(RechargePlanVO.builder().planId("P6").points(29800).amountCents(29800).tag("尊享").build());
        return plans;
    }

    private RechargePlanVO findPlan(String planId) {
        for (RechargePlanVO plan : buildPlans()) {
            if (plan.getPlanId().equals(planId)) {
                return plan;
            }
        }
        return null;
    }

}
