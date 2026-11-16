package org.jeecg.modules.app.constant;

import java.math.BigDecimal;

/**
 * VIP 会员与用户仓库常量信息
 *
 * @author elinx
 */
public class VipStorageConstant {

    /**
     * VIP 年费金额（元）
     */
    public static final BigDecimal VIP_AMOUNT = new BigDecimal("99.00");

    /**
     * 单次购买 VIP 年数
     */
    public static final int VIP_YEARS = 1;

    /**
     * 支付渠道-微信支付
     */
    public static final String PAY_CHANNEL_WECHAT = "WECHAT";

    /**
     * 支付渠道-支付宝
     */
    public static final String PAY_CHANNEL_ALIPAY = "ALIPAY";

    /**
     * VIP 订单支付状态-已支付
     */
    public static final String PAY_STATUS_PAID = "PAID";

    /**
     * 仓库类型-虚拟仓
     */
    public static final String STORAGE_TYPE_VIRTUAL = "VIRTUAL";

    /**
     * 仓库类型-真实仓
     */
    public static final String STORAGE_TYPE_REAL = "REAL";

    /**
     * 普通用户最多可创建真实个人仓数量
     */
    public static final int STORAGE_LIMIT_NORMAL = 1;

    /**
     * VIP 用户最多可创建真实个人仓数量
     */
    public static final int STORAGE_LIMIT_VIP = 3;

    /**
     * 内置系统虚拟仓 Id（不落库，仅列表兜底返回）
     */
    public static final String BUILTIN_VIRTUAL_STORAGE_ID = "-1";

    /**
     * 内置系统虚拟仓名称
     */
    public static final String BUILTIN_VIRTUAL_STORAGE_NAME = "虚拟仓";

}
