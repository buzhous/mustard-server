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

/**
 * 用户独立回收站（与殷墟隔离）：删除的物品进入本人回收站，保留 30 天，
 * 期间可恢复或彻底删除；丢弃/转入公海仍走殷墟，两者互不相通
 */
@Data
@TableName("tbl_item_recycle")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "用户独立回收站")
public class ItemRecycle implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "id")
    private java.lang.Long id;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间(删除时间)")
    private java.util.Date createTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "修改时间")
    private java.util.Date updateTime;

    @Schema(description = "原始Id")
    private java.lang.String oriId;

    @Schema(description = "物品Id")
    private java.lang.String itemId;

    @Schema(description = "分类ID")
    private java.lang.String categoryId;

    @Schema(description = "物品名称")
    private java.lang.String name;

    @Schema(description = "图标")
    private java.lang.String icon;

    @Schema(description = "删除数量")
    private java.lang.Integer quantity;

    @Schema(description = "等级")
    private java.lang.Integer level;

    @Schema(description = "价格")
    private java.math.BigDecimal price;

    @Schema(description = "描述")
    private java.lang.String description;

    @Schema(description = "排序")
    private java.lang.Integer sort;

    @Schema(description = "用户Id")
    private java.lang.String userId;

    @Schema(description = "状态: 0在回收站, 2已恢复")
    private java.lang.Integer status;

    @Schema(description = "仓位类型快照: 0未指定,1虚拟仓,2真实站点仓")
    private java.lang.Integer stationType;

    @Schema(description = "仓位Id快照")
    private java.lang.String stationId;

    @Schema(description = "组件列表")
    private java.lang.String fields;

    @Schema(description = "拓展数据")
    private java.lang.String extendData;

    @Schema(description = "属性列表")
    private java.lang.String attributes;

    @Schema(description = "标签列表")
    private java.lang.String tags;

}
