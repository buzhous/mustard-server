package org.jeecg.modules.app.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * AI 建档会话（ai-integration-plan §5.9 S2）
 * 聊天记录多会话保存：状态点（active=进行中琥珀 / filed=已入库绿）+ 标题 + 摘要 + 草稿快照。
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@TableName("tbl_ai_chat_session")
@Schema(description = "AI 建档会话")
public class AiChatSession implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 会话状态：进行中（琥珀点） */
    public static final String STATUS_ACTIVE = "active";
    /** 会话状态：已入库（绿点） */
    public static final String STATUS_FILED = "filed";

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键")
    private String id;

    @Schema(description = "用户ID")
    private String uid;

    @Schema(description = "标题（自动取首条草稿名）")
    private String title;

    @Schema(description = "状态：active进行中 filed已入库")
    private String status;

    @Schema(description = "最后一条消息摘要（会话面板展示）")
    private String lastSummary;

    @Schema(description = "本会话追问计数（≤3，防循环）")
    private Integer askCount;

    @Schema(description = "当前草稿快照JSON（含各字段置信度）")
    private String draftSnapshot;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "更新时间")
    private Date updateTime;
}
