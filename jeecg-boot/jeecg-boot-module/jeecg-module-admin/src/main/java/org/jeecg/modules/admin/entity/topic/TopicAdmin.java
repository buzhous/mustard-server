package org.jeecg.modules.admin.entity.topic;

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
import java.util.Date;

/**
 * 添加物品主题（后台管理，映射 tbl_topic，与 App 端 org.jeecg.modules.app.entity.Topic 共表）
 */
@Data
@TableName("tbl_topic")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(name = "tbl_topic后台管理对象", description = "添加物品主题管理")
public class TopicAdmin implements Serializable {

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

    @Schema(description = "主题编码")
    private String topicCode;

    @Schema(description = "主题名称")
    private String topicName;

    @Schema(description = "子标题")
    private String topicSubtitle;

    @Schema(description = "主题类型(1基础型 2多媒体型 3模型型 4扩展型,保存时由后端根据fields自动推导)")
    private Integer topicType;

    @Schema(description = "主题ICON")
    private String topicIcon;

    @Schema(description = "背景图")
    private String bgImage;

    @Schema(description = "主题描述")
    private String topicDesc;

    @Schema(description = "主题分类")
    private String topicCate;

    @Schema(description = "主题状态(0启用 1停用)")
    private Integer topicStatus;

    @Schema(description = "物品分类")
    private String itemCate;

    @Schema(description = "是否热门")
    private Integer isHot;

    @Schema(description = "主题价格")
    private Integer price;

    @Schema(description = "是否收费")
    private Integer isCharge;

    @Schema(description = "主题字段配置JSON")
    private String fields;

    @Schema(description = "MIP v1 Profile文档JSON（保存时由服务端自动生成）")
    private String mipProfile;
}
