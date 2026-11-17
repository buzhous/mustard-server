package org.jeecg.modules.app.entity.warehouse;

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
import java.math.BigDecimal;
import java.util.Date;

/**
 * 实物云仓-用户专属默认真实仓
 * 每个账号创建时默认生成 1 个，支持修改收件人、电话、收件地址与定位
 */
@Data
@TableName("tbl_user_real_station")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "用户专属默认真实仓")
public class UserRealStation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "仓库编号")
    private String stationNo;

    @Schema(description = "用户自定义仓库名")
    private String customName;

    @Schema(description = "收件人")
    private String receiverName;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "省份")
    private String province;

    @Schema(description = "城市")
    private String city;

    @Schema(description = "区县")
    private String district;

    @Schema(description = "详细地址")
    private String address;

    @Schema(description = "经度")
    private BigDecimal longitude;

    @Schema(description = "纬度")
    private BigDecimal latitude;

    @Schema(description = "状态:0停用,1正常")
    private Integer status;
}
