package org.jeecg.modules.app.entity.article;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文章栏目（表 tbl_article_column，App 只读）
 */
@Data
@TableName("tbl_article_column")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "文章栏目")
public class ArticleColumn implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 状态-启用
     */
    public static final int STATUS_ENABLED = 1;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "id")
    private String id;

    @Schema(description = "栏目名称")
    private String columnName;

    @Schema(description = "栏目编码（唯一）")
    private String columnCode;

    @Schema(description = "排序（越小越靠前）")
    private Integer sortNo;

    @Schema(description = "状态 1-启用 0-停用")
    private Integer status;
}
