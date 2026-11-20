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
 * 快递100 下单记录（移库寄送）
 */
@Data
@TableName("tbl_kuaidi_order")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(name = "快递100下单记录", description = "快递100下单记录")
public class KuaidiOrder implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "id")
    private String id;

    @Schema(description = "关联tbl_item_storage_in.id")
    private String recordId;

    @Schema(description = "快递公司编码")
    private String kuaidicom;

    @Schema(description = "快递100 taskId")
    private String taskId;

    @Schema(description = "快递单号")
    private String kuaidinum;

    @Schema(description = "取件码")
    private String pickCode;

    @Schema(description = "订单状态:0已下单,1已接单,2已揽收,3运输中,4派送中,5已签收,-1失败")
    private Integer orderStatus;

    @Schema(description = "寄件人姓名")
    private String sendName;

    @Schema(description = "寄件人电话")
    private String sendPhone;

    @Schema(description = "寄件地址")
    private String sendAddr;

    @Schema(description = "收件人姓名")
    private String recName;

    @Schema(description = "收件人电话")
    private String recPhone;

    @Schema(description = "收件地址")
    private String recAddr;

    @Schema(description = "回调原始报文")
    private String callbackRaw;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private Date updateTime;
}
