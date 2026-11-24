package org.jeecg.modules.app.bean.vo.category;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 系统分类 VO（App 端系统分类列表/添加关联，C3 分类治理）
 */
@Data
@Accessors(chain = true)
@Schema(description = "系统分类")
public class SystemCategoryVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "系统分类 id")
    private String id;

    @Schema(description = "分类名称")
    private String name;

    @Schema(description = "图标（iconpark 名称）")
    private String icon;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "分类边界说明")
    private String remark;

    @Schema(description = "当前用户是否已关联（true 时列表不再展示添加按钮）")
    private Boolean added;
}
