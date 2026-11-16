package org.jeecg.modules.admin.constant;

/**
 * 优惠券管理后台常量
 */
public class CouponAdminConstant {

    /** 券模板状态: 0-未发布 */
    public static final Integer COUPON_STATUS_UNPUBLISHED = 0;
    /** 券模板状态: 1-已发布 */
    public static final Integer COUPON_STATUS_PUBLISHED = 1;
    /** 券模板状态: 2-已下架 */
    public static final Integer COUPON_STATUS_OFFLINE = 2;

    /** 用户券状态: 0-未使用 */
    public static final Integer UC_STATUS_UNUSED = 0;
    /** 用户券状态: 1-已使用 */
    public static final Integer UC_STATUS_USED = 1;
    /** 用户券状态: 2-已过期 */
    public static final Integer UC_STATUS_EXPIRED = 2;
    /** 用户券状态: 3-已作废 */
    public static final Integer UC_STATUS_INVALID = 3;

    /** 获取方式: 2-系统发放 */
    public static final Integer OBTAIN_TYPE_GRANT = 2;

    /** 优惠券类型: 1-满减券 */
    public static final Integer TYPE_FULL_REDUCTION = 1;
    /** 优惠券类型: 2-折扣券 */
    public static final Integer TYPE_DISCOUNT = 2;
    /** 优惠券类型: 3-兑换券 */
    public static final Integer TYPE_EXCHANGE = 3;

}
