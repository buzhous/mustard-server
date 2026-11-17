package org.jeecg.modules.app.constant;

/**
 * 实物云仓常量信息
 *
 * @author elinx
 */
public class StationConstant {

    /**
     * 普通用户最多可绑定站点数量（VIP 会员体系上线后按等级调整为 3）
     */
    public static final int MAX_BIND_COUNT_NORMAL = 1;

    /**
     * VIP 会员最多可绑定站点数量
     */
    public static final int MAX_BIND_COUNT_VIP = 3;

    /**
     * 站点状态-停用
     */
    public static final int STATION_STATUS_DISABLED = 0;

    /**
     * 站点状态-开放
     */
    public static final int STATION_STATUS_OPEN = 1;

    /**
     * 绑定状态-已解绑
     */
    public static final int BIND_STATUS_UNBOUND = 0;

    /**
     * 绑定状态-已绑定
     */
    public static final int BIND_STATUS_BOUND = 1;

    /**
     * 非主站点
     */
    public static final int IS_DEFAULT_NO = 0;

    /**
     * 主站点
     */
    public static final int IS_DEFAULT_YES = 1;

    /**
     * 仓位类型-虚拟仓（用户固定 1 个，物品默认平台 OSS 位置）
     */
    public static final int STATION_TYPE_VIRTUAL = 1;

    /**
     * 仓位类型-真实站点仓（用户绑定平台站点）
     */
    public static final int STATION_TYPE_REAL = 2;

    /**
     * 仓位类型-用户自有仓（个人真实仓 tbl_user_storage）
     */
    public static final int STATION_TYPE_STORAGE = 3;

    /**
     * 虚拟仓默认位置说明
     */
    public static final String DEFAULT_VIRTUAL_LOCATION_DESC = "芥子空间平台 OSS 仓库";

    /**
     * 虚拟仓编号前缀
     */
    public static final String VIRTUAL_STATION_NO_PREFIX = "VS";

    /**
     * 生成虚拟仓编号：VS + 用户Id 末 6 位（不足补齐），与用户固定关联
     */
    public static String buildVirtualStationNo(String userId) {
        String tail = userId == null ? "000000" : userId;
        tail = tail.length() >= 6 ? tail.substring(tail.length() - 6) : String.format("%06d", Math.abs(tail.hashCode()) % 1000000);
        return VIRTUAL_STATION_NO_PREFIX + tail;
    }

    /**
     * 用户默认真实仓编号前缀
     */
    public static final String REAL_STATION_NO_PREFIX = "RS";

    /**
     * 生成用户默认真实仓编号：RS + 用户Id 末 6 位（不足补齐），与用户固定关联
     */
    public static String buildRealStationNo(String userId) {
        String tail = userId == null ? "000000" : userId;
        tail = tail.length() >= 6 ? tail.substring(tail.length() - 6) : String.format("%06d", Math.abs(tail.hashCode()) % 1000000);
        return REAL_STATION_NO_PREFIX + tail;
    }

}
