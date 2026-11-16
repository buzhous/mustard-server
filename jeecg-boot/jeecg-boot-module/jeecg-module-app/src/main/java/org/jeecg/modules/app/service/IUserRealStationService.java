package org.jeecg.modules.app.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.app.bean.vo.station.UserRealStationVO;
import org.jeecg.modules.app.entity.warehouse.UserRealStation;

/**
 * 实物云仓-用户专属默认真实仓服务
 */
public interface IUserRealStationService extends IService<UserRealStation> {

    /**
     * 确保用户拥有默认真实仓（惰性初始化，幂等）
     * 新账号在注册链路创建；存量账号首次查询时兜底创建
     */
    UserRealStation ensureUserRealStation(String userId);

    /**
     * 查询用户默认真实仓（不存在时自动创建）
     */
    UserRealStationVO getDetailByUser(String userId);

    /**
     * 更新默认真实仓：收件人、电话、省市区、详细地址、定位
     */
    UserRealStationVO updateByUser(String userId, UserRealStationVO vo);
}
