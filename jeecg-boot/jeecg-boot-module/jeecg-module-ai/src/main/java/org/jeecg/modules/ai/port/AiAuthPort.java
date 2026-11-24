package org.jeecg.modules.ai.port;

/**
 * 认证端口：ai 模块获取当前登录用户 uid（app 模块桥接实现）
 */
public interface AiAuthPort {

    /**
     * 当前登录用户 uid（未登录/无 x-uid 头返回 null）
     */
    String currentUid();
}
