package org.jeecg.modules.app.bean.vo.item;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 物品条件查询请求对象（App 端 / 开放平台共用）
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "物品条件查询请求对象")
public class ItemInfoQueryVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID（服务端根据登录用户/开放平台授权填充）")
    private String userId;

    @Schema(description = "关键字（名称/描述模糊匹配）")
    private String keyword;

    @Schema(description = "仓位ID")
    private String stationId;

    @Schema(description = "仓位类型：1虚拟仓 2真实站点仓")
    private Integer stationType;

    @Schema(description = "物品状态：0临时 1正常 2使用中")
    private Integer status;

    @Schema(description = "分类ID（精确匹配）")
    private String category;

    @Schema(description = "多分类ID列表（AI 组查询：一个泛称映射多个分类时使用，≤10 个）")
    private List<String> categoryList;

    @Schema(description = "等级")
    private Integer level;

    @Schema(description = "标签名（fields JSON 模糊匹配）")
    private String tag;

    @Schema(description = "排序字段：created/name/price/quantity/status")
    private String orderBy;

    @Schema(description = "是否升序，默认降序")
    private Boolean ascending;

    @Schema(description = "页码", defaultValue = "1")
    private Integer pageNo = 1;

    @Schema(description = "页大小", defaultValue = "10")
    private Integer pageSize = 10;
}
