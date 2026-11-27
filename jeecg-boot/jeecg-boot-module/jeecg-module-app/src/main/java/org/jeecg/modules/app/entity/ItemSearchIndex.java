package org.jeecg.modules.app.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 物品检索索引（可重建快照，非权威数据）
 * <p>
 * ngram 全文检索快照：由 ItemSearchIndexService.rebuild 在物品写路径同事务维护，
 * admin 工作台可异步全量重建。受限（restricted）组件值不进索引。
 */
@Data
@TableName("tbl_item_search_index")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(name = "物品检索索引", description = "物品全文检索快照(可重建)")
public class ItemSearchIndex implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    @Schema(description = "物品Id(tbl_item_info.id)")
    private String itemId;

    @Schema(description = "所属用户")
    private String uid;

    @Schema(description = "归一化名称(trim/全角转半角)")
    private String normName;

    @Schema(description = "归一化分类")
    private String categoryNorm;

    @Schema(description = "归一化标签(空格分隔)")
    private String tagNorms;

    @Schema(description = "非受限组件属性文本")
    private String attrText;

    @Schema(description = "全文检索拼接文本(含别名折叠)")
    private String searchBlob;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "快照更新时间")
    private Date updatedTime;
}
