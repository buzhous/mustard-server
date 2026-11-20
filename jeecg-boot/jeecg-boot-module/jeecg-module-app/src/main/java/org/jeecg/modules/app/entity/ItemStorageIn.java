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
 * 物品转入/移库单
 * 转入自建仓：10待确认转移 → 20已入仓（仓主确认）
 * 转入官方仓：15待邮寄 → 16已邮寄 → 25已入库（官方后台确认）
 * 移库（快递寄送）：30待接收方确认 → 31已下单待揽收 → 32运输中 → 33已签收（归属转移）
 */
@Data
@TableName("tbl_item_storage_in")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(name = "物品转入/移库单", description = "物品转入/移库单")
public class ItemStorageIn implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "id")
    private String id;

    @Schema(description = "入库单号(SI+id尾6位)")
    private String depositNo;

    @Schema(description = "发起人用户Id")
    private String userId;

    @Schema(description = "物品Id")
    private String itemId;

    @Schema(description = "物品原始Id")
    private String oriId;

    @Schema(description = "库存Id")
    private String inventoryId;

    @Schema(description = "仓Id(转入目标仓/移库来源仓)")
    private String stationId;

    @Schema(description = "来源类型:1自建仓,2官方仓")
    private Integer source;

    @Schema(description = "业务类型:1转入,2移库")
    private Integer bizType;

    @Schema(description = "状态:10待确认转移/20已入仓/15待邮寄/16已邮寄/25已入库/30待接收确认/31已下单待揽收/32运输中/33已签收/40待确认收货/99已取消")
    private Integer status;

    @Schema(description = "快递单号(官方转入回填)")
    private String expressNo;

    @Schema(description = "数量")
    private Integer quantity;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "移库接收方用户Id")
    private String receiverUserId;

    @Schema(description = "关联赠送单Id")
    private String giftId;

    @Schema(description = "接收方落仓Id")
    private String receiverStationId;

    @Schema(description = "接收方落仓类型")
    private Integer receiverStationType;

    @Schema(description = "确认人用户Id")
    private String confirmUserId;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "确认时间")
    private Date confirmTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private Date updateTime;
}
