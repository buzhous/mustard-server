package org.jeecg.modules.app.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.app.entity.UserAmount;
import org.jeecg.modules.app.entity.user.AppUser;


public interface IUserAmountService extends IService<UserAmount> {

    UserAmount queryByUserId(String userId);

    boolean createUserAmount(AppUser appUser);

    /**
     * 原子增加金币（积分）并记录积分流水，返回变动后的最新余额，失败返回 null
     */
    Integer addGoldCoin(String userId, int points, int flowType, String remark, String refId);

}
