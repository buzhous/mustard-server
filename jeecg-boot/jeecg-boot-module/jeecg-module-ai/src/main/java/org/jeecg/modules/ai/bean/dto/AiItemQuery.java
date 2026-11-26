package org.jeecg.modules.ai.bean.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 物品条件查询请求（ai 模块自建 DTO，字段为编排器实际读写的 ItemInfoQueryVO 字段子集）
 */
@Data
@Accessors(chain = true)
public class AiItemQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String userId;

    private String keyword;

    private List<String> categoryList;

    private String tag;

    private Integer level;

    private Integer status;

    /** 价格区间下限（元，含） */
    private BigDecimal priceMin;

    /** 价格区间上限（元，含） */
    private BigDecimal priceMax;

    /** 建档时间区间起点（含） */
    private Date createTimeStart;

    /** 建档时间区间终点（含） */
    private Date createTimeEnd;

    /** 仓位 id 组查询（station 名称解析后的 id 集合） */
    private List<String> stationIds;

    /** 排序字段：created/name/price/quantity/status */
    private String orderBy;

    /** 是否升序，默认降序 */
    private Boolean ascending;

    private Integer pageNo;

    private Integer pageSize;

    public AiItemQuery() {
    }

    /** 拷贝构造：渐进放宽重试时基于原查询微调（丢 keyword/categoryList） */
    public AiItemQuery(AiItemQuery other) {
        this.userId = other.userId;
        this.keyword = other.keyword;
        this.categoryList = other.categoryList;
        this.tag = other.tag;
        this.level = other.level;
        this.status = other.status;
        this.priceMin = other.priceMin;
        this.priceMax = other.priceMax;
        this.createTimeStart = other.createTimeStart;
        this.createTimeEnd = other.createTimeEnd;
        this.stationIds = other.stationIds;
        this.orderBy = other.orderBy;
        this.ascending = other.ascending;
        this.pageNo = other.pageNo;
        this.pageSize = other.pageSize;
    }
}
