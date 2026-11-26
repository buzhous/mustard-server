package org.jeecg.modules.app.bean.vo.queue;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "物品同步下载请求")
public class ItemSyncDownloadReqVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "原始Id")
    private String oriId;

    @Schema(description = "物品Id")
    private String itemId;

    @Schema(description = "库存Id")
    private String inventoryId;

    @Schema(description = "用户ID（内部）")
    private String userId;

    @Schema(description = "最后同步时间戳（增量同步用，毫秒）")
    private Long lastSyncTime;

    @Schema(description = "分页大小（>0 启用分页，缺省全量返回；服务端上限 500）")
    private Integer pageSize;

    @Schema(description = "翻页游标：上一页最后一条记录 id（配合 lastSyncTime=游标时间续拉，防同毫秒漏页）")
    private Long cursorId;

}
