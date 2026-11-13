package org.jeecg.modules.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.admin.bean.vo.station.UserStationBindVO;
import org.jeecg.modules.admin.entity.warehouse.UserStationBind;

import java.util.List;

public interface IUserStationBindAdminService extends IService<UserStationBind> {

    /**
     * 查询当前用户已绑定的站点列表（含站点信息，主站点在前）
     */
    List<UserStationBindVO> listMyStations(String userId);

    /**
     * 绑定站点（校验站点开放、重复绑定、绑定数量上限；首个绑定自动设为主站点）
     */
    boolean bindStation(String userId, String stationId);

    /**
     * 解绑站点（解绑主站点后自动将最早绑定的剩余站点设为主站点）
     */
    boolean unbindStation(String userId, String stationId);

    /**
     * 设置主站点（须为已绑定站点）
     */
    boolean setDefaultStation(String userId, String stationId);
}
