package org.jeecg.modules.app.entity.warehouse;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
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
 * 实物云仓-用户虚拟仓库点
 * 规则：普通用户固定拥有 1 个虚拟仓库点，仅含编号与位置说明
 */
@Data
@TableName("tbl_user_virtual_station")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "实物云仓-用户虚拟仓库点")
public class UserVirtualStation implements Serializable {

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

    @Excel(name = "用户Id", width = 15)
    @Schema(description = "用户Id")
    private String userId;

    @Excel(name = "虚拟仓编号", width = 15)
    @Schema(description = "虚拟仓编号(如 VS10000001)")
    private String stationNo;

    @Excel(name = "位置说明", width = 30)
    @Schema(description = "位置说明(默认:芥子空间平台 OSS 仓库)")
    private String locationDesc;
}
