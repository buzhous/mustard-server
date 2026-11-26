package org.jeecg.modules.ai.bean.vo.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import org.jeecg.modules.ai.entity.AiChatMessage;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * AI 建档·确认创建响应（S5）：N 件 = N 个云端 itemId，过程条目与回执带 ×N 标注
 */
@Data
@Accessors(chain = true)
@Schema(description = "AI 建档创建响应")
public class AiChatCreateVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "请求创建件数")
    private Integer total;

    @Schema(description = "成功件数")
    private Integer count;

    @Schema(description = "成功物品 itemId 列表（tbl_item_info.id，App 端按 itemId 映射本地记录跳转）")
    private List<String> itemIds;

    @Schema(description = "首个失败原因（全部失败时返回）")
    private String error;

    @Schema(description = "本轮落库消息（过程条目 + 回执）")
    private List<AiChatMessage> messages;
}
