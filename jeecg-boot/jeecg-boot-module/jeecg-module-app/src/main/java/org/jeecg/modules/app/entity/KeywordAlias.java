package org.jeecg.modules.app.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 检索别名词表（只读）：App/AI 检索侧仅读取启用词行做别名折叠与扩展，
 * 词表维护（增删改）由 P4 后台页负责。
 */
@Data
@TableName("tbl_keyword_alias")
@Accessors(chain = true)
@Schema(name = "检索别名词", description = "同义词/上下位词表(只读)")
public class KeywordAlias implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "别名(用户输入词)")
    private String alias;

    @Schema(description = "标准词")
    private String standard;

    @Schema(description = "词型:1同义词,2上下位")
    private Integer aliasType;

    @Schema(description = "是否启用:1启用,0停用")
    private Integer enabled;
}
