package org.jeecg.modules.app.bean.vo.queue;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jeecg.modules.app.entity.ItemSync;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "物品增量同步下载响应")
public class ItemSyncDownloadIncrementalRspVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // ==================== 增量同步字段 ====================

    @Schema(description = "服务端时间戳（毫秒），客户端下次同步用此值作为lastSyncTime")
    private Long serverTime;

    @Schema(description = "本次变更的物品总数（needAdd+needUpdate+needDelete）")
    private Integer totalCount;

    @Schema(description = "该用户全部同步记录总数（客户端用于检测水位线越前导致的漏拉，变更=0 但 total 变化时需全量重拉）")
    private Long total;

    @Schema(description = "是否有更多数据")
    private Boolean hasMore;

    @Schema(description = "下一页游标（时间戳）")
    private String cursor;

    @Schema(description = "需要新增的物品列表")
    private List<ItemSync> needAddList = new ArrayList<>();

    @Schema(description = "需要更新的物品列表")
    private List<ItemSync> needUpdateList = new ArrayList<>();

    @Schema(description = "需要删除的物品ID列表")
    private List<String> needDeleteList = new ArrayList<>();

}
