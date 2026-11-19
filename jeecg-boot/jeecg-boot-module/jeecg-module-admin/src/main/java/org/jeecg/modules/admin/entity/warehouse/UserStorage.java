package org.jeecg.modules.admin.entity.warehouse;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 用户仓库（后台查看）
 */
@Data
@TableName("tbl_user_storage")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "用户仓库表")
public class UserStorage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "id")
    private String id;

    @Excel(name = "用户Id", width = 20)
    @Schema(description = "用户Id")
    private String userId;

    @Excel(name = "仓库编号", width = 15)
    @Schema(description = "仓库编号(US + 仓库Id末6位)")
    private String stationNo;

    @Excel(name = "仓库名称", width = 15)
    @Schema(description = "仓库名称")
    private String name;

    @Excel(name = "仓库类型", width = 10)
    @Schema(description = "仓库类型:VIRTUAL|REAL")
    private String storageType;

    @Excel(name = "是否默认仓", width = 10)
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

    @Excel(name = "详细地址", width = 30)
    @Schema(description = "详细地址")
    private String address;

    @Schema(description = "经度")
    private java.math.BigDecimal longitude;

    @Schema(description = "纬度")
    private java.math.BigDecimal latitude;

    @Schema(description = "是否删除:0否,1是")
    @TableLogic
    private Integer deleted;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "修改时间")
    private Date updateTime;

    /** 用户手机号（列表展示用，非表字段） */
    @TableField(exist = false)
    private String userPhone;

    /** 用户昵称（列表展示用，非表字段） */
    @TableField(exist = false)
    private String nickname;
}
