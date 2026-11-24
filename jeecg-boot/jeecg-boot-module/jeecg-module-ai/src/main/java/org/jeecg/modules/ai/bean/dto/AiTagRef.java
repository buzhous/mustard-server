package org.jeecg.modules.ai.bean.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户标签引用（ai 模块自建 DTO；桥接实现映射 tbl_user_tag）
 */
@Data
@Accessors(chain = true)
public class AiTagRef implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String id;

    private String name;
}
