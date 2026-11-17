package org.jeecg.modules.app.entity.article;

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
 * 文章（表 tbl_article，App 只读）
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

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "id")
    private String id;

    @Schema(description = "所属栏目id")
    private String columnId;

    @Schema(description = "所属栏目编码")
    private String columnCode;

    @Schema(description = "文章业务编码（唯一）")
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
}
