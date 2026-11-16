package org.jeecg.modules.admin.entity.ruins;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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

/**
 * 殷墟物品 后台管理实体（表 tbl_item_ruins，与 app 模块 ItemRuins 同表）
 */
@Data
@TableName("tbl_item_ruins")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "殷墟物品管理")
public class ItemRuinsAdmin implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "id")
    private String id;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private java.util.Date createTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "修改时间")
    private java.util.Date updateTime;

    @Schema(description = "原始Id")
    private String oriId;

    @Schema(description = "物品编码")
    private String itemId;

    @Schema(description = "分类ID")
    private String categoryId;

    @Schema(description = "物品名称")
    private String name;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "数量")
    private Integer quantity;

    @Schema(description = "等级")
    private Integer level;

    @Schema(description = "价格")
    private java.math.BigDecimal price;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "原归属用户Id")
    private String userId;

    @Schema(description = "指定拾取者用户Id（非空时仅该用户可见可拾取，拾取后为实际拾取者）")
    private String pickupUser;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "拾取时间")
    private java.util.Date pickupTime;

    @Schema(description = "状态: 0待拾取，1已拾取，2收回，3充公，4已下架")
    private Integer status;

    @Schema(description = "组件列表")
    private String fields;

    @Schema(description = "拓展数据")
    private String extendData;

    @Schema(description = "属性列表")
    private String attributes;

    @Schema(description = "标签列表")
    private String tags;

    @TableField(exist = false)
    @Schema(description = "指定拾取者昵称（列表展示用）")
    private String pickupUserName;
}
