package org.jeecg.modules.app.constant;

/**
 * 优惠券常量信息
 *
 * @author elinx
 */
public class CouponConstant {

    /**
     * 殷墟拾取券固定编码
     */
    public static final String RUINS_PICKUP_CODE = "RUINS_PICKUP";

    /**
     * 殷墟拾取券名称
     */
    public static final String RUINS_PICKUP_NAME = "殷墟拾取券";

    /**
     * 拾取券库存极大值（惰性创建时初始化）
     */
    public static final Integer RUINS_PICKUP_TOTAL_COUNT = 99999999;

    /**
     * 优惠券类型: 3-兑换券
     */
    public static final Integer COUPON_TYPE_EXCHANGE = 3;

    /**
     * 优惠券状态: 1-已发布
     */
    public static final Integer COUPON_STATUS_PUBLISHED = 1;

    /**
     * 解析每日领取限制（后台券配置 dailyLimit 可调），未配置（null/<=0）时默认1张
     */
    public static int resolveDailyClaimLimit(Integer dailyLimit) {
        return (dailyLimit == null || dailyLimit <= 0) ? 1 : dailyLimit;
    }

}
