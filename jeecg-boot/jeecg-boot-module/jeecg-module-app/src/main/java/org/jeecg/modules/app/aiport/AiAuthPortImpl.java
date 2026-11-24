package org.jeecg.modules.app.aiport;

import org.jeecg.modules.ai.port.AiAuthPort;
import org.jeecg.modules.app.entity.user.AppUser;
import org.jeecg.modules.app.utils.AppAuthUtil;

/**
 * AiAuthPort 桥接实现：App 登录态取 uid（未登录/无 x-uid 头返回 null）
 */
public class AiAuthPortImpl implements AiAuthPort {

    @Override
    public String currentUid() {
        AppUser loginUser = AppAuthUtil.getUserInfo();
        return loginUser == null ? null : loginUser.getId();
    }
}
