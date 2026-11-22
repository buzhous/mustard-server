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
 * 开放平台调用审计日志（设计方案 3.4 / E3）
 * append-only 使用习惯；REST 与 MCP 同表归因（channel 区分出口，MCP 的 path 记 tool 名）
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@TableName("tbl_open_audit_log")
@Schema(description = "开放平台调用审计日志")
public class OpenAuditLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "调用方 AppKey")
    private String appKey;

    @Schema(description = "数据归属用户")
    private String boundUid;

    @Schema(description = "出口：rest / mcp")
    private String channel;

    @Schema(description = "接口路径或 MCP tool 名")
    private String path;

    @Schema(description = "read / write")
    private String action;

    @Schema(description = "入参摘要（截断）")
    private String paramsDigest;

    @Schema(description = "返回码")
    private Integer resultCode;

    @Schema(description = "返回条数（list/search）")
    private Integer dataCount;

    @Schema(description = "耗时毫秒")
    private Integer costMs;

    @Schema(description = "调用方IP")
    private String clientIp;

    @Schema(description = "调用时间")
    private Date createTime;
}
