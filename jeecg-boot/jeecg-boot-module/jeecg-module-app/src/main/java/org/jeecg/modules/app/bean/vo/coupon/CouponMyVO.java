package org.jeecg.modules.app.bean.vo.coupon;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 我的拾取券VO
 */
@Data
@Schema(description = "我的拾取券对象")
public class CouponMyVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "可用殷墟拾取券数量（未使用且未过期）")
    private Integer availableCount;

    @Schema(description = "今日是否已领满每日限领数量（true时App隐藏领取入口）")
    private Boolean claimedToday;

}
