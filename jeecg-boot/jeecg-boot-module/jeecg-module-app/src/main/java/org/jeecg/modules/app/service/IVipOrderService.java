package org.jeecg.modules.app.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.app.bean.vo.vip.VipInfoVO;
import org.jeecg.modules.app.bean.vo.vip.VipPurchaseRespVO;
import org.jeecg.modules.app.entity.VipOrder;

/**
 * VIP 会员服务
 *
 * @author elinx
 */
public interface IVipOrderService extends IService<VipOrder> {

    /**
     * 购买/续费 VIP（99 元/年，当前默认模拟支付直接成功）
     *
     * @param userId     用户Id
     * @param payChannel 支付渠道:WECHAT|ALIPAY
     * @param mock       是否模拟支付
     * @return 订单Id、最新到期时间、支付金额
     */
    VipPurchaseRespVO purchase(String userId, String payChannel, Boolean mock);

    /**
     * 查询用户 VIP 信息
     *
     * @param userId 用户Id
     * @return 是否VIP、到期时间
     */
    VipInfoVO getVipInfo(String userId);

    /**
     * 用户当前是否为 VIP（vipExpireTime > now）
     *
     * @param userId 用户Id
     */
    boolean isVip(String userId);

}
