package org.jeecg.modules.app.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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
 * 物品流转账本（MIP 协议 5.1 tbl_item_lineage）
 * <p>
 * append-only：只 INSERT，禁止 UPDATE / DELETE。
 * 链上只存 uid 哈希、事件类型、时间、操作者类别；payload 禁止出现任何属性值。
 */
@Data
@TableName("tbl_item_lineage")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(name = "物品流转账本", description = "MIP v1 流转账本，append-only")
public class ItemLineage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "id")
    private Long id;

    @Schema(description = "物品全局唯一标识")
    private String oriId;

    @Schema(description = "物品行Id(tbl_item_info.id,事件关联辅助列,不入哈希)")
    private String itemId;

    @Schema(description = "事件类型:mint/gift_out/gift_in/ruins_drop/ruins_pick/transfer/spawn/destroy/recycle/import")
    private String eventType;

    @Schema(description = "流出方用户哈希")
    private String fromUid;

    @Schema(description = "流入方用户哈希")
    private String toUid;

    @Schema(description = "流出方仓位")
    private String fromStation;

    @Schema(description = "流入方仓位")
    private String toStation;

    @Schema(description = "操作者:user/system/agent:{appKey尾4位}")
    private String operator;

    @Schema(description = "事件摘要JSON,禁止fields内容")
    private String payload;

    @Schema(description = "事件涉及数量(数量拆分场景)")
    private Integer qty;

    @Schema(description = "拆分派生时的父物品oriId(spawn事件)")
    private String parentOri;

    @Schema(description = "幂等键:ori_id+event_type+业务单号,唯一约束防重放")
    private String idempotencyKey;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "事件发生时间")
    private Date occurredAt;

    @Schema(description = "前一事件哈希")
    private String prevHash;

    @Schema(description = "本事件哈希")
    private String hash;
}
