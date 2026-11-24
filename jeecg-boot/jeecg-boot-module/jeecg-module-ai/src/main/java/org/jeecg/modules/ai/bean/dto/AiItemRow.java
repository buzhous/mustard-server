package org.jeecg.modules.ai.bean.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 物品行（ai 模块自建 DTO，字段为编排器实际读取的 ItemInfo 字段子集；id 为 Long 雪花 ID）
 */
@Data
@Accessors(chain = true)
public class AiItemRow implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String oriId;

    private String name;

    private String category;

    private String icon;

    private Integer quantity;

    private Integer status;

    /** 等级（1-10） */
    private Integer level;

    private Date createTime;

    /** 价格（元） */
    private BigDecimal price;

    /** 仓位 id（虚拟仓或真实站点仓） */
    private String stationId;

    /** 仓位类型：1虚拟仓 2真实站点仓 */
    private Integer stationType;
}
