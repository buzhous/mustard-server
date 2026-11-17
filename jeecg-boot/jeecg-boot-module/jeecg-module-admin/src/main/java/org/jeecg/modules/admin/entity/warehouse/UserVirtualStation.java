package org.jeecg.modules.admin.entity.warehouse;

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
 * 用户虚拟仓库点（后台查看）
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

    @Excel(name = "用户Id", width = 20)
    @Schema(description = "用户Id")
    private String userId;

    @Excel(name = "虚拟仓编号", width = 15)
    @Schema(description = "虚拟仓编号")
    private String stationNo;

    @Excel(name = "位置说明", width = 30)
    @Schema(description = "位置说明")
    private String locationDesc;

    @Excel(name = "自定义仓库名", width = 15)
    @Schema(description = "自定义仓库名(空则展示默认名)")
    private String customName;
}
