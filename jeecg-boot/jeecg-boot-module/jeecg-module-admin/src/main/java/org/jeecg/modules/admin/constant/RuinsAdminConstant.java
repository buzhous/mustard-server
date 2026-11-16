package org.jeecg.modules.admin.constant;

/**
 * 殷墟管理常量（与 app 模块 RuinsStatusEnum 保持一致）
 */
public class RuinsAdminConstant {

    /** 状态：0待拾取 */
    public static final int STATUS_NOT_PICKUP = 0;
    /** 状态：1已拾取 */
    public static final int STATUS_PICKED = 1;
    /** 状态：2收回 */
    public static final int STATUS_RECALLED = 2;
    /** 状态：3充公 */
    public static final int STATUS_CONFISCATED = 3;
    /** 状态：4已下架（App 广场不展示、不可拾取） */
    public static final int STATUS_OFF_SHELF = 4;

    public static String statusLabel(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case STATUS_NOT_PICKUP -> "待拾取";
            case STATUS_PICKED -> "已拾取";
            case STATUS_RECALLED -> "已收回";
            case STATUS_CONFISCATED -> "已充公";
            case STATUS_OFF_SHELF -> "已下架";
            default -> "未知";
        };
    }

    private RuinsAdminConstant() {
    }
}
