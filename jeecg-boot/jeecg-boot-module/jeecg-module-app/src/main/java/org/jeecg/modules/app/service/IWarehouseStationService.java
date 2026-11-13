package org.jeecg.modules.app.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.app.bean.vo.station.WarehouseStationVO;
import org.jeecg.modules.app.entity.warehouse.WarehouseStation;

public interface IWarehouseStationService extends IService<WarehouseStation> {

    /**
     * 分页查询平台开放站点列表
     *
     * @param pageNo   页码
     * @param pageSize 每页数量
     * @param city     城市（可空）
     * @param keyword  关键词，匹配站点名称/地址/覆盖区域（可空）
     */
    IPage<WarehouseStationVO> queryStationPage(int pageNo, int pageSize, String city, String keyword);

    /**
     * 查询站点详情
     */
    WarehouseStationVO queryStationDetail(String stationId);
}
