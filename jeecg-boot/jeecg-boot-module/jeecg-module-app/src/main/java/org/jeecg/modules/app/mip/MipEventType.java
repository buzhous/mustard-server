package org.jeecg.modules.app.mip;

/**
 * MIP v1 流转账本事件字典（MIP 协议 5.2 + 10.3 缺陷①，共 10 种）
 * <p>
 * restore（回收站恢复）不记账：custody 未变。
 * destroy（回收站彻底删除）为账本终止事件，其后该 ori_id 不允许新事件；
 * 同 ori_id 重新建档视为新链（新 mint）。
 */
public enum MipEventType {

    /** 建档（链首事件，协议上线后新建档） */
    MINT("mint", "建档"),
    /** 编辑更新（开放接口/MCP 写路径，链内事件） */
    UPDATE("update", "编辑更新"),
    /** 赠送发出 */
    GIFT_OUT("gift_out", "赠送发出"),
    /** 赠送领取 */
    GIFT_IN("gift_in", "赠送领取"),
    /** 丢入殷墟 */
    RUINS_DROP("ruins_drop", "丢入殷墟"),
    /** 殷墟拾取 */
    RUINS_PICK("ruins_pick", "殷墟拾取"),
    /** 定向流转（D1，无资金结算） */
    TRANSFER("transfer", "定向流转"),
    /** 流转发出（两阶段要约被确认时与 flow_in 成对入链：发起方放行视角） */
    FLOW_OUT("flow_out", "流转发出"),
    /** 流转接收（两阶段要约被确认时与 flow_out 成对入链：接收方领取视角） */
    FLOW_IN("flow_in", "流转接收"),
    /** 数量拆分派生新档（跨持有者拆分时接收方新 ori_id） */
    SPAWN("spawn", "拆分派生"),
    /** 回收站彻底删除（账本终止事件） */
    DESTROY("destroy", "彻底删除"),
    /** 进入回收站 */
    RECYCLE("recycle", "进入回收站"),
    /** 外部锚点（存量起始 / 档案导入，operator=system） */
    IMPORT("import", "导入锚点");

    private final String code;
    private final String label;

    MipEventType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }
}
