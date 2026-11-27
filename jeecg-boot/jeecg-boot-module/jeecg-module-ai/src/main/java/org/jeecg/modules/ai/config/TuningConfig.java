package org.jeecg.modules.ai.config;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * AI 调优配置（P4）：提示词在线编辑 / 模型参数 / 检索开关与意图缓存参数。
 * 版本历史同表多版本：active=1 当前生效，active=0 历史；enabled=0 整 key 停用（回退内置技能包/默认值）。
 */
@Data
@TableName("tbl_ai_tuning_config")
@Accessors(chain = true)
public class TuningConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String configKey;

    private String configValue;

    private String valueType;

    private Integer version;

    /** DB 配置是否生效：1 生效，0 停用（回退内置） */
    private Integer enabled;

    /** 是否当前生效版本：1 是，0 历史 */
    private Integer active;

    private String remark;

    private String updateBy;

    private Date updateTime;
}
