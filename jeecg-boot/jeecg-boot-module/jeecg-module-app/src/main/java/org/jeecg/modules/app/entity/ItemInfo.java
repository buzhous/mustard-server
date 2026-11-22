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
@TableName("tbl_item_info")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(name = "物品表", description = "物品表")
public class ItemInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "id")
    // Long 雪花 ID 超出 JS Number.MAX_SAFE_INTEGER，JSON 出参一律字符串（防前端精度丢失）
    // Jackson 2 注解（Redis 等 Jackson2 序列化场景）+ Jackson 3 全限定注解（Spring Boot 4 HTTP 出参）
    @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
    @tools.jackson.databind.annotation.JsonSerialize(using = tools.jackson.databind.ser.std.ToStringSerializer.class)
    private java.lang.Long id;

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

    @Schema(description = "分类ID")
    private java.lang.String category;

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

    @Schema(description = "组件列表")
    private String fields;

    @Schema(description = "拓展数据")
    private String extendData;

    @Schema(description = "属性列表")
    private String attributes;

    @Schema(description = "标签列表")
    private String tags;

    @Schema(description = "用户Id")
    private String userId;

    @Schema(description = "仓位Id(虚拟仓Id或真实站点Id),未传时服务端自动落默认仓")
    private String stationId;

    @Schema(description = "仓位类型:1虚拟仓,2真实站点仓")
    private Integer stationType;

    // ============ 开放平台出域摘要（非表字段，list 响应装配，设计方案 4.2）============

    @Schema(description = "MIP 模板 schemaId（出域摘要，非表字段）")
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String schemaId;

    @Schema(description = "MIP 内容哈希 contentHash（出域摘要，非表字段）")
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String contentHash;

}
