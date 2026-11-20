package org.jeecg.modules.app.bean.vo.storagein;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 转入/移库单列表项
 */
@Data
@Accessors(chain = true)
@Schema(description = "转入/移库单列表项")
public class StorageInListVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "单Id")
    private String id;

    @Schema(description = "入库单号")
    private String depositNo;

    @Schema(description = "业务类型:1转入,2移库")
    private Integer bizType;

    @Schema(description = "来源类型:1自建仓,2官方仓")
    private Integer source;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "状态文案")
    private String statusDesc;

    @Schema(description = "物品Id")
    private String itemId;

    @Schema(description = "物品名称")
    private String itemName;

    @Schema(description = "物品图标")
    private String itemIcon;

    @Schema(description = "数量")
    private Integer quantity;

    @Schema(description = "仓Id(转入目标仓/移库来源仓)")
    private String stationId;

    @Schema(description = "仓库名称")
    private String stationName;

    @Schema(description = "发起人用户Id")
    private String userId;

    @Schema(description = "发起人昵称")
    private String userNickname;

    @Schema(description = "接收人用户Id")
    private String receiverUserId;

    @Schema(description = "接收人昵称")
    private String receiverNickname;

    @Schema(description = "快递单号(官方转入回填)")
    private String expressNo;

    @Schema(description = "快递订单状态:0已下单,1已接单,2已揽收,3运输中,4派送中,5已签收,-1失败")
    private Integer kuaidiStatus;

    @Schema(description = "快递单号(快递100)")
    private String kuaidinum;

    @Schema(description = "取件码")
    private String pickCode;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private Date updateTime;
}
