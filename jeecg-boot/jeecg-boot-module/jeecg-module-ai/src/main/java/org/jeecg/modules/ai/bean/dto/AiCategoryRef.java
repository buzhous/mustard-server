package org.jeecg.modules.ai.bean.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户分类引用（ai 模块自建 DTO，字段为编排器实际读取的 UserCategory 字段：id/name）
 */
@Data
@Accessors(chain = true)
public class AiCategoryRef implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String id;

    private String name;
}
