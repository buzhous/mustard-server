package org.jeecg.modules.app.bean.enums;

/**
 * 系统状态码及错误码
 *
 * @version 1.0
 * @description: 状态码及错误码
 */
public enum ExceptionEnum {

    // 系统错误
    SUCCESS(200, "success"),
    SYSTEM_ERROR(-1, "系统异常，请稍后再试"),

    // 登录/用户 10100-10199
    ACCESS_DENIED(10100, "访问未授权"),
    REQUEST_TIMEOUT(10101, "请求超时"),
    TOKEN_INVALID_ERROR(10102, "Token Invalid"),
    USER_INFO_ERROR(10103, "用户信息错误"),
    PASSWORD_ERROR(10104, "账号密码错误"),
    PHONE_ERROR(10105, "手机号格式错误"),
    SMS_CODE_ERROR(10106, "短信验证码错误"),
    PASSWORD_NOT_MATCH(10107, "密码不匹配"),
    USER_INFO_NOT_EXIST(10108, "用户信息不存在"),
    // 物品 10200-10500
    ITEM_NOT_EXIST(10200, "物品不存在"),
    ITEM_DESTROYED(10201, "物品已被销毁"),
    INSUFFICIENT_QUANTITY(10202, "物品数量不足"),
    RUINS_ALREADY_PICKED(10203, "手慢了，已被别人拾取"),
    RUINS_PICKUP_COUPON_NOT_ENOUGH(10204, "拾取券不足，请先领取"),
    // 赠送 10600-10700
    GIFT_NOT_EXIST(10600, "赠送记录不存在"),
    GIFT_CANCELLED(10601, "赠送已撤回"),
    GIFT_EXPIRED(10602, "赠送已过期"),
    GIFT_RECEIVED(10603, "赠送已领完"),
    GIFT_NOT_BELONG_TO_USER(10604, "赠送不属于当前用户"),
    GIFT_PER_LIMIT_EXCEEDED(10605, "已达到每人限领数量"),
    GIFT_INSUFFICIENT_QUANTITY(10606, "赠送剩余数量不足"),
    GIFT_ALREADY_CANCELLED(10607, "赠送已撤回"),
    GIFT_ALREADY_RECEIVED(10608, "赠送已领完"),
    // 仓储站点 10700-10800
    STATION_NOT_EXIST(10700, "仓储站点不存在"),
    STATION_NOT_AVAILABLE(10701, "仓储站点未开放"),
    STATION_ALREADY_BOUND(10702, "已绑定该仓储站点"),
    STATION_BIND_LIMIT_EXCEEDED(10703, "已达到仓储站点绑定上限"),
    STATION_NOT_BOUND(10704, "未绑定该仓储站点"),
    // 用户仓库 10710-10720
    STORAGE_NOT_EXIST(10710, "仓库不存在"),
    STORAGE_LIMIT_EXCEEDED(10711, "普通用户仅可创建1个仓库，开通VIP可创建3个"),
    STORAGE_NOT_REAL(10712, "仅真实仓库支持该操作"),
    // 优惠券 10500-10600
    COUPON_NOT_EXIST(10500, "优惠券不存在"),
    COUPON_ALREADY_PUBLISHED(10501, "优惠券已发布"),
    COUPON_ALREADY_OFFLINE(10502, "优惠券已下架"),
    COUPON_NOT_AVAILABLE(10503, "优惠券不可领取"),
    COUPON_STOCK_EMPTY(10504, "优惠券已领完"),
    COUPON_NOT_IN_VALID_TIME(10505, "优惠券不在领取时间范围内"),
    COUPON_RECEIVE_LIMIT_EXCEEDED(10506, "已达到领取上限"),
    COUPON_DAILY_LIMIT_EXCEEDED(10511, "今日已达领取上限"),
    COUPON_DAILY_ALREADY_CLAIMED(10512, "今日已领取，明天再来吧"),
    USER_COUPON_NOT_EXIST(10507, "用户优惠券不存在"),
    COUPON_NOT_BELONG_TO_USER(10508, "优惠券不属于该用户"),
    COUPON_ALREADY_USED(10509, "优惠券已使用"),
    COUPON_EXPIRED(10510, "优惠券已过期"),
    // 用户分类 10730-10740
    CATEGORY_LIMIT_EXCEEDED(10730, "非会员最多拥有5个分类，开通VIP解锁更多"),
    // 仓库删除 10740-10750
    STATION_DEFAULT_CANNOT_DELETE(10740, "默认仓库不可删除，请先将其他仓库设为默认仓"),
    STATION_NOT_EMPTY(10741, "仓库下仍有物品，移出所有物品后才能删除"),
    STORAGE_DEFAULT_CANNOT_DELETE(10742, "默认仓库不可删除"),
    STORAGE_NOT_EMPTY(10743, "仓库下仍有物品，移出所有物品后才能删除"),
    // 转入/移库/认证 10800-10850
    STORAGE_AUTH_NOT_REAL(10800, "仅自建仓支持平台认证"),
    STORAGE_AUTH_MATERIAL_EMPTY(10801, "认证材料不完整，请补全后提交"),
    // 开放授权密钥 10850-10860
    OPEN_CREDENTIAL_LIMIT(10850, "最多创建10个密钥，请先删除不再使用的密钥"),
    STORAGE_AUTH_PENDING(10802, "认证审核中，请耐心等待"),
    STORAGE_AUTH_APPROVED_ALREADY(10803, "该仓库已认证，无需重复提交"),
    STORAGE_NOT_CERTIFIED(10804, "仓库未认证，认证通过后可转入寄放"),
    STORAGE_IN_NOT_EXIST(10805, "转入/寄放单不存在"),
    STORAGE_IN_STATUS_ERROR(10806, "单据状态不允许该操作"),
    ITEM_IN_TRANSIT(10807, "物品在途或待确认转移，暂不可操作"),
    STORAGE_IN_NOT_OWNER(10808, "仅单据发起人可操作"),
    STORAGE_IN_NOT_RECEIVER(10809, "仅接收人可确认收件"),
    RECEIVER_INFO_EMPTY(10810, "接收方收件信息不完整"),
    KUAIDI_ORDER_FAIL(10811, "快递下单失败，请稍后重试"),
    STORAGE_MOVE_ONLY_STORAGE(10812, "仅自建仓物品支持移库转移"),
    STORAGE_IN_SELF_MOVE(10813, "接收人不能是自己"),
    // 通用业务错误 11000-12000
    DATA_UPDATE_ERROR(11000, "更新失败"),
    DATA_DELETE_ERROR(11001, "删除失败"),
    REQUEST_PARAM_ERROR(11002, "请求参数错误"),
    LOCK_FAILED(11003, "锁获取失败"),
    ;

    private int code;
    private String msg;

    ExceptionEnum() {
    }

    ExceptionEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public static String getValue(int code) {
        for (ExceptionEnum value : ExceptionEnum.values()) {
            if (value.getCode() == code) {
                return value.getMsg();
            }
        }
        return null;
    }

}
