package org.jeecg.modules.ai.bean.enums;

/**
 * AI 模块错误码（镜像自 app 模块 ExceptionEnum 中 AI 建档实际用到的子集，msg 文案逐字一致）
 */
public enum AiErrorEnum {

    // 登录/用户 10100-10199
    USER_INFO_NOT_EXIST(10108, "用户信息不存在"),
    // 通用业务错误 11000-12000
    REQUEST_PARAM_ERROR(11002, "请求参数错误"),
    ;

    private int code;
    private String msg;

    AiErrorEnum() {
    }

    AiErrorEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

}
