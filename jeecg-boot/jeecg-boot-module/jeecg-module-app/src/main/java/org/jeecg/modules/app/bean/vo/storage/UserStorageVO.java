package org.jeecg.modules.app.bean.vo.storage;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户仓库 VO
 */
@Data
@Accessors(chain = true)
@Schema(description = "用户仓库")
public class UserStorageVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "仓库Id(内置虚拟仓固定-1)")
    private String id;

    @Schema(description = "仓库编号")
    private String stationNo;

    @Schema(description = "仓库名称")
    private String name;

    @Schema(description = "仓库类型:VIRTUAL|REAL")
    private String storageType;

    @Schema(description = "是否默认仓:0否,1是")
    private Integer isDefault;

    @Schema(description = "收件人")
    private String receiverName;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "省")
    private String province;

    @Schema(description = "市")
    private String city;

    @Schema(description = "区")
    private String district;

    @Schema(description = "详细地址")
    private String address;

    @Schema(description = "经度")
    private BigDecimal longitude;

    @Schema(description = "纬度")
    private BigDecimal latitude;

    @Schema(description = "是否删除:0否,1是")
    private Integer deleted;

    @Schema(description = "认证状态:0未认证,1审核中,2已认证,3已驳回")
    private Integer authStatus;

    @Schema(description = "认证驳回原因")
    private String authReason;

    @Schema(description = "认证材料JSON")
    private String authMaterials;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "认证通过时间")
    private Date authTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最近提交认证时间")
    private Date authApplyTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;

}
