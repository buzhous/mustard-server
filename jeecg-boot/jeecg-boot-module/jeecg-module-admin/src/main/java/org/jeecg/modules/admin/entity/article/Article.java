package org.jeecg.modules.admin.entity.article;

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
 * 文章（表 tbl_article），content 为富文本 HTML
 */
@Data
@TableName("tbl_article")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "文章")
public class Article implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 状态-发布
     */
    public static final int STATUS_PUBLISHED = 1;

    /**
     * 状态-下架（草稿）
     */
    public static final int STATUS_OFFLINE = 0;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "id")
    private String id;

    @Schema(description = "所属栏目id")
    private String columnId;

    @Schema(description = "所属栏目编码（冗余）")
    private String columnCode;

    @TableField(exist = false)
    @Schema(description = "所属栏目名称（列表联查展示）")
    private String columnName;

    @Schema(description = "文章业务编码（唯一，App 按编码获取）")
    private String articleCode;

    @Schema(description = "文章标题")
    private String title;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "富文本内容（HTML）")
    private String content;

    @Schema(description = "封面图")
    private String coverImage;

    @Schema(description = "作者")
    private String author;

    @Schema(description = "排序")
    private Integer sortNo;

    @Schema(description = "状态 1-发布 0-下架")
    private Integer status;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "发布时间")
    private java.util.Date publishTime;

    @Schema(description = "浏览次数")
    private Integer viewCount;

    @Schema(description = "创建人")
    private String createBy;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private java.util.Date createTime;

    @Schema(description = "更新人")
    private String updateBy;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private java.util.Date updateTime;

    @Schema(description = "删除标志 0-正常 1-已删除")
    private Integer delFlag;
}
