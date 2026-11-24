package org.jeecg.modules.app.entity;

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

/**
 * 系统物品分类（全局统一治理表，C3 分类治理）：
 * AI 建档与检索共享的统一分类体系；用户分类命中系统分类时按此实例化（tbl_user_category.system_id 关联）。
 */
@Data
@TableName("tbl_system_category")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "系统物品分类")
public class SystemCategory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "id")
    private String id;

    @Excel(name = "名称", width = 15)
    @Schema(description = "分类名称（≤4字）")
    private String name;

    @Excel(name = "图标", width = 15)
    @Schema(description = "图标（iconpark 名称）")
    private String icon;

    @Excel(name = "排序", width = 15)
    @Schema(description = "排序（越小越前）")
    private Integer sort;

    @Schema(description = "状态：1启用 0停用")
    private Integer status;

    @Excel(name = "说明", width = 30)
    @Schema(description = "分类边界说明（运营维护，App 展示与治理参考）")
    private String remark;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private java.util.Date createTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private java.util.Date updateTime;
}
