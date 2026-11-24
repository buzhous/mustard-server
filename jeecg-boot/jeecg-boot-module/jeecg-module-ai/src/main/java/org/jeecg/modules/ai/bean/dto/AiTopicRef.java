package org.jeecg.modules.ai.bean.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 用户主题引用（ai 模块自建 DTO；桥接实现映射 tbl_topic 启用模板）
 */
@Data
@Accessors(chain = true)
public class AiTopicRef implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String id;

    private String name;

    /** 主题类型：1基础 2多媒体 3模型 4扩展 */
    private Integer topicType;

    /** 主题启用的组件编号集（fields JSON 解析，仅保留 1-34 已知组件） */
    private List<Integer> elements;
}
