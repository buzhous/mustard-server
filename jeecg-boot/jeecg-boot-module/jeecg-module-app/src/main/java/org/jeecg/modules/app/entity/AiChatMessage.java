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
 * AI 建档消息（ai-integration-plan §5.9 S2）
 * 消息流元素：用户气泡 / AI 气泡 / 过程条目 / 草稿卡 / 回执，全部随会话持久化（切换/重开完整恢复）。
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@TableName("tbl_ai_chat_message")
@Schema(description = "AI 建档消息")
public class AiChatMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 消息角色：用户 */
    public static final String ROLE_USER = "user";
    /** 消息角色：AI */
    public static final String ROLE_ASSISTANT = "assistant";

    /** 消息类型：普通文本气泡 */
    public static final String TYPE_TEXT = "text";
    /** 消息类型：草稿卡（draft_snapshot 必填） */
    public static final String TYPE_DRAFT = "draft";
    /** 消息类型：追问气泡（content=问题文本） */
    public static final String TYPE_CLARIFY = "clarify";
    /** 消息类型：回执卡（process_entries 必填） */
    public static final String TYPE_RECEIPT = "receipt";
    /** 消息类型：过程条目横条（process_entries 必填） */
    public static final String TYPE_PROCESS = "process";

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键")
    private String id;

    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "用户ID")
    private String uid;

    @Schema(description = "角色：user/assistant")
    private String role;

    @Schema(description = "类型：text/draft/clarify/receipt/process")
    private String msgType;

    @Schema(description = "文本内容")
    private String content;

    @Schema(description = "图片fileKey JSON数组（≤3张）")
    private String images;

    @Schema(description = "过程条目JSON数组（创建×N/同步等）")
    private String processEntries;

    @Schema(description = "草稿快照JSON（含各字段置信度）")
    private String draftSnapshot;

    @Schema(description = "整体置信度 0-100")
    private Integer confidence;

    @Schema(description = "创建时间")
    private Date createTime;
}
