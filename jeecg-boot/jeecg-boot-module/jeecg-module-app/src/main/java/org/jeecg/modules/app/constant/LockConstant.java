package org.jeecg.modules.app.constant;

/**
 * 锁常量信息
 *
 * @author elinx
 */
public class LockConstant {

    /**
     * 物品拾取锁
     */
    public static final String ITEM_PICKUP_LOCK = "lock_ruins_item_pickup:";

    /**
     * 优惠券领取锁
     */
    public static final String COUPON_RECEIVE_LOCK = "lock_coupon_receive:";

    /**
     * 每日领取拾取券锁
     */
    public static final String COUPON_CLAIM_DAILY_LOCK = "lock_coupon_claim_daily:";

    /**
     * 回收站恢复/彻底删除互斥锁（防止并发双重恢复或双重删除）
     */
    public static final String ITEM_RECYCLE_LOCK = "lock_item_recycle:";

    /**
     * 物品销毁/删除入口锁（oriId 维度）：串行化同一物品的销毁事务，
     * 防止并发 dropToRuins/dropToRecycle「先查后改」竞态产生殷墟/回收站双行（重复消费）
     */
    public static final String ITEM_DESTROY_LOCK = "lock_item_destroy:";

}
