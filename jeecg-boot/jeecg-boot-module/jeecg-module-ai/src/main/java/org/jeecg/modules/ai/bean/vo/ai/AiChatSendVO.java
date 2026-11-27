package org.jeecg.modules.ai.bean.vo.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import org.jeecg.modules.ai.entity.AiChatMessage;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * AI 建档·发送响应（S3/S4）
 */
@Data
@Accessors(chain = true)
@Schema(description = "AI 建档发送响应")
public class AiChatSendVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "会话状态：active/filed")
    private String sessionStatus;

    @Schema(description = "剩余追问次数（≤3）")
    private Integer askRemaining;

    @Schema(description = "草稿 JSON（含各字段置信度；chat 回合为 null）")
    private com.alibaba.fastjson.JSONObject draft;

    @Schema(description = "整体置信度 0-100")
    private Integer overallConfidence;

    @Schema(description = "本轮落库消息（用户气泡 + AI 气泡/草稿卡/追问）")
    private List<AiChatMessage> messages;

    @Schema(description = "express 直接建档成功标记（true 时 itemIds 可用）")
    private Boolean direct;

    @Schema(description = "express 直接建档成功的物品 itemId 清单（×N，tbl_item_info.id）")
    private List<String> itemIds;

    @Schema(description = "express 直接建档件数（数量 ×N，direct=true 时可用）")
    private Integer quantity;

    @Schema(description = "P7 票据预填：OCR 结构化块 {merchant, purchase_time, amount, summary}（仅 mode=receipt 识别成功返回，其余为 null）")
    private com.alibaba.fastjson.JSONObject receipt;
}
