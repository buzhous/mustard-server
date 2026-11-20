package org.jeecg.modules.app.constant;

import java.util.List;

/**
 * 物品转入/移库 + 自建仓认证常量
 *
 * @author elinx
 */
public class StorageInConstant {

    /** 来源类型-自建仓 */
    public static final int SOURCE_STORAGE = 1;
    /** 来源类型-官方仓 */
    public static final int SOURCE_OFFICIAL = 2;

    /** 业务类型-转入 */
    public static final int BIZ_TRANSFER_IN = 1;
    /** 业务类型-移库（快递寄送） */
    public static final int BIZ_MOVE = 2;

    // ---- 单据状态 ----
    /** 自建仓-待确认转移 */
    public static final int ST_WAIT_CONFIRM = 10;
    /** 自建仓-已入仓 */
    public static final int ST_STORED = 20;
    /** 官方仓-待邮寄 */
    public static final int ST_WAIT_MAIL = 15;
    /** 官方仓-已邮寄 */
    public static final int ST_MAILED = 16;
    /** 官方仓-已入库 */
    public static final int ST_STORED_OFFICIAL = 25;
    /** 发运-待接收方确认落仓（提供收件信息） */
    public static final int ST_MOVE_WAIT_RECEIVE = 30;
    /** 发运-已下单待揽收 */
    public static final int ST_MOVE_ORDERED = 31;
    /** 发运-运输中 */
    public static final int ST_MOVE_SHIPPING = 32;
    /** 发运-已签收（实体入库接收方仓，归属已在赠送领取时转移） */
    public static final int ST_MOVE_DONE = 33;
    /** 发运-线下已当面交付（待接收方确认收货入库） */
    public static final int ST_MOVE_OFFLINE = 40;
    /** 已取消 */
    public static final int ST_CANCELLED = 99;

    /** 认证状态-未认证 */
    public static final int AUTH_NONE = 0;
    /** 认证状态-审核中 */
    public static final int AUTH_PENDING = 1;
    /** 认证状态-已认证 */
    public static final int AUTH_APPROVED = 2;
    /** 认证状态-已驳回 */
    public static final int AUTH_REJECTED = 3;

    /** 入库单号前缀 */
    public static final String DEPOSIT_NO_PREFIX = "SI";

    /** 在途/待确认状态集合：命中则锁定物品（不可赠送/销毁/编辑） */
    public static final List<Integer> ACTIVE_STATUSES =
            List.of(ST_WAIT_CONFIRM, ST_WAIT_MAIL, ST_MAILED,
                    ST_MOVE_WAIT_RECEIVE, ST_MOVE_ORDERED, ST_MOVE_SHIPPING,
                    ST_MOVE_OFFLINE);

    /** 可取消状态：自建待确认 / 官方待邮寄 / 官方已邮寄（发运单归属已转移，不可取消） */
    public static final List<Integer> CANCELLABLE_STATUSES =
            List.of(ST_WAIT_CONFIRM, ST_WAIT_MAIL, ST_MAILED);

    /** 生成入库单号：SI + 单Id 末 6 位 */
    public static String buildDepositNo(String id) {
        String tail = id == null ? "000000" : id;
        tail = tail.length() >= 6 ? tail.substring(tail.length() - 6)
                : String.format("%06d", Math.abs(tail.hashCode()) % 1000000);
        return DEPOSIT_NO_PREFIX + tail;
    }

    public static String statusDesc(int status) {
        return switch (status) {
            case ST_WAIT_CONFIRM -> "待确认转移";
            case ST_STORED -> "已入仓";
            case ST_WAIT_MAIL -> "待邮寄";
            case ST_MAILED -> "已邮寄";
            case ST_STORED_OFFICIAL -> "已入库";
            case ST_MOVE_WAIT_RECEIVE -> "待确认落仓";
            case ST_MOVE_ORDERED -> "已下单待揽收";
            case ST_MOVE_SHIPPING -> "运输中";
            case ST_MOVE_DONE -> "已签收入库";
            case ST_MOVE_OFFLINE -> "待确认收货";
            case ST_CANCELLED -> "已取消";
            default -> "未知";
        };
    }
}
