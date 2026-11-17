package org.jeecg.modules.app.entity.warehouse;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
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

/**
 * 用户仓库（虚拟仓 + 真实个人仓）
 * 物品归属沿用 ItemInfo.stationId/stationType：自有仓 stationId=本表 id，stationType=3
 */
@Data
@TableName("tbl_user_storage")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(name = "用户仓库表", description = "用户仓库表")
public class UserStorage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "id")
    private String id;

    @Schema(description = "用户Id")
    private String userId;

    @Schema(description = "仓库名称")
    private String name;

    @Schema(description = "仓库类型:VIRTUAL|REAL")
    private String storageType;

    @Schema(description = "是否默认仓:0否,1是")
    private Integer isDefault;

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

}
