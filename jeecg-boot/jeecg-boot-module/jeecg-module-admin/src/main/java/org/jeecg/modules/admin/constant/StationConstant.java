package org.jeecg.modules.admin.constant;

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

}
