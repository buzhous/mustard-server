package org.jeecg.modules.app.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;


@Data
@TableName("tbl_points_flow")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(name = "tbl_points_flow对象", description = "积分流水")
public class PointsFlow implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 充值 */
    public static final int TYPE_RECHARGE = 10;
    /** 注册赠送 */
    public static final int TYPE_REGISTER_GIFT = 20;
    /** 消费 */
    public static final int TYPE_CONSUME = 30;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "id")
    private String id;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "修改时间")
    private Date updateTime;

    @Schema(description = "用户Id")
    private String userId;

    @Schema(description = "类型 10充值 20注册赠送 30消费")
    private Integer type;

    @Schema(description = "积分变动")
    private Integer points;

    @Schema(description = "变动后余额")
    private Integer balanceAfter;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "关联单号")
    private String refId;

}
