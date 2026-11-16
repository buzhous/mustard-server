package org.jeecg.modules.app.service.impl;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.app.bean.enums.ExceptionEnum;
import org.jeecg.modules.app.bean.exception.AppException;
import org.jeecg.modules.app.bean.vo.vip.VipInfoVO;
import org.jeecg.modules.app.bean.vo.vip.VipPurchaseRespVO;
import org.jeecg.modules.app.constant.VipStorageConstant;
import org.jeecg.modules.app.entity.VipOrder;
import org.jeecg.modules.app.entity.user.AppUser;
import org.jeecg.modules.app.mapper.VipOrderMapper;
import org.jeecg.modules.app.service.IAppUserService;
import org.jeecg.modules.app.service.IVipOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * VIP 会员服务实现
 * 99 元/年；当前阶段默认模拟支付直接成功，预留微信/支付宝支付网关接入点
 *
 * @author elinx
 */
@Slf4j
@Service
public class VipOrderServiceImpl extends ServiceImpl<VipOrderMapper, VipOrder> implements IVipOrderService {

    @Autowired
    private IAppUserService appUserService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VipPurchaseRespVO purchase(String userId, String payChannel, Boolean mock) {
        if (StrUtil.isEmpty(userId)
                || !VipStorageConstant.PAY_CHANNEL_WECHAT.equals(payChannel)
                && !VipStorageConstant.PAY_CHANNEL_ALIPAY.equals(payChannel)) {
            throw new AppException(ExceptionEnum.REQUEST_PARAM_ERROR);
        }
        boolean mockPay = mock == null || mock;
        AppUser user = appUserService.getById(userId);
        if (user == null) {
            throw new AppException(ExceptionEnum.USER_INFO_NOT_EXIST);
        }

        Date now = new Date();
        // 生成订单
        VipOrder order = new VipOrder()
                .setOrderNo(buildOrderNo(now))
                .setUserId(userId)
                .setAmount(VipStorageConstant.VIP_AMOUNT)
                .setYears(VipStorageConstant.VIP_YEARS)
                .setPayChannel(payChannel)
                .setMock(mockPay);
        order.setCreateTime(now);

        // ---------- payChannelGateway 预留接入点 ----------
        // 真实支付接入时在此按渠道分流，此处仅提供接口流程，默认支付通过：
        // - WECHAT: 调微信支付统一下单(小程序/APP)，返回预支付参数；支付回调验签成功后将订单置为 PAID 并执行续期
        // - ALIPAY: 调支付宝 trade.app.pay / wap.pay，异步通知验签成功后将订单置为 PAID 并执行续期
        // - mock=true: 跳过真实网关，直接标记支付成功（当前默认行为，不引入真实 SDK）
        if (!executePay(payChannel, mockPay)) {
            throw new AppException(ExceptionEnum.SYSTEM_ERROR);
        }
        order.setPayStatus(VipStorageConstant.PAY_STATUS_PAID);
        order.setPayTime(new Date());
        baseMapper.insert(order);

        // 续期：从 max(now, 当前到期时间) 起延长购买年数
        Date base = user.getVipExpireTime() != null && user.getVipExpireTime().after(now)
                ? user.getVipExpireTime() : now;
        Date newExpire = DateUtil.offset(base, DateField.YEAR, order.getYears());
        AppUser update = new AppUser();
        update.setId(userId);
        update.setVipExpireTime(newExpire);
        appUserService.updateById(update);

        log.info("VIP购买成功 userId={}, orderNo={}, payChannel={}, mock={}, expire={}",
                userId, order.getOrderNo(), payChannel, mockPay, DateUtil.formatDateTime(newExpire));

        return new VipPurchaseRespVO()
                .setOrderId(order.getId())
                .setVipExpireTime(newExpire)
                .setAmount(order.getAmount());
    }

    @Override
    public VipInfoVO getVipInfo(String userId) {
        if (StrUtil.isEmpty(userId)) {
            throw new AppException(ExceptionEnum.REQUEST_PARAM_ERROR);
        }
        AppUser user = appUserService.getById(userId);
        if (user == null) {
            throw new AppException(ExceptionEnum.USER_INFO_NOT_EXIST);
        }
        return new VipInfoVO()
                .setIsVip(isVip(userId))
                .setVipExpireTime(user.getVipExpireTime());
    }

    @Override
    public boolean isVip(String userId) {
        if (StrUtil.isEmpty(userId)) {
            return false;
        }
        AppUser user = appUserService.getById(userId);
        return user != null
                && user.getVipExpireTime() != null
                && user.getVipExpireTime().after(new Date());
    }

    /**
     * 执行支付（支付网关分流位）
     * mock=true 直接成功；mock=false 时真实渠道接入后在此返回网关下单/回调结果
     */
    private boolean executePay(String payChannel, boolean mock) {
        if (mock) {
            return true;
        }
        // TODO 真实支付渠道接入位：
        //  WECHAT → WechatPayGateway；ALIPAY → AlipayGateway
        //  接入前非 mock 请求一律视为模拟支付通过，避免阻塞接口流程
        log.warn("真实支付渠道未接入, payChannel={} 按模拟支付处理", payChannel);
        return true;
    }

    /**
     * 订单号：VIP + 时间戳(毫秒) + 4位随机数
     */
    private String buildOrderNo(Date now) {
        return "VIP" + DateUtil.format(now, "yyyyMMddHHmmssSSS") + RandomUtil.randomNumbers(4);
    }

}
