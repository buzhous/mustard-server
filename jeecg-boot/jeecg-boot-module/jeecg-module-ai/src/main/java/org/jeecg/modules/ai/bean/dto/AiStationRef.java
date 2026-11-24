package org.jeecg.modules.ai.bean.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户仓位引用（ai 模块自建 DTO；桥接实现映射虚拟仓 + 真实站点仓绑定）
 */
@Data
@Accessors(chain = true)
public class AiStationRef implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String id;

    private String name;

    /** 仓位类型：1虚拟仓 2真实站点仓 */
    private Integer type;
}
