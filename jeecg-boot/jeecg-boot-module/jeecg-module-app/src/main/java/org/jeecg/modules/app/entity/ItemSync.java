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
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName(value = "tbl_item_sync", autoResultMap = true)
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "物品同步")
public class ItemSync implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "id")
    private Long id;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "修改时间")
    private Date updateTime;

    @Schema(description = "原始Id")
    private String oriId;

    @Schema(description = "物品Id")
    private String itemId;

    @Schema(description = "持有Id")
    private String inventoryId;

    @Schema(description = "分类ID")
    private java.lang.String categoryId;

    @Schema(description = "主题名称")
    private String name;

    @Schema(description = "主题类型")
    private String icon;

    @Schema(description = "数量")
    private Integer quantity;

    @Schema(description = "主题描述")
    private Integer level;

    @Schema(description = "价格")
    private BigDecimal price;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态")
    private Integer status;

    /**
     * 仓位ID（仅用于上传/下载链路透传，不落库）
     */
    @Schema(description = "仓位ID")
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String stationId;

    /**
     * 仓位类型：1虚拟仓 2真实站点仓（仅用于链路透传，不落库）
     */
    @Schema(description = "仓位类型：1虚拟仓 2真实站点仓")
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private Integer stationType;

    /**
     * 在途/待确认状态：0-无单据；>0 为转入/移库单活动状态值（仅下载链路透传）
     */
    @Schema(description = "在途/待确认状态")
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private Integer storageInStatus;

    /**
     * 在途/待确认单号（仅下载链路透传）
     */
    @Schema(description = "在途/待确认单号")
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String storageInNo;

    @Schema(description = "组件列表")
    private String fields;

    @Schema(description = "拓展数据")
    private String extendData;

    @Schema(description = "属性列表")
    private String attributes;

    @Schema(description = "标签列表")
    private String tags;

    @Schema(description = "版本")
    private Integer version;

    @Schema(description = "同步状态")
    private Integer syncStatus;

    @Schema(description = "用户Id")
    private String userId;

}
