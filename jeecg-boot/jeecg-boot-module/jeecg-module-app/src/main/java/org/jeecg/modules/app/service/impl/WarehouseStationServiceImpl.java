package org.jeecg.modules.app.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.app.bean.enums.ExceptionEnum;
import org.jeecg.modules.app.bean.exception.AppException;
import org.jeecg.modules.app.bean.vo.station.WarehouseStationVO;
import org.jeecg.modules.app.constant.StationConstant;
import org.jeecg.modules.app.entity.warehouse.WarehouseStation;
import org.jeecg.modules.app.mapper.WarehouseStationMapper;
import org.jeecg.modules.app.service.IWarehouseStationService;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class WarehouseStationServiceImpl extends ServiceImpl<WarehouseStationMapper, WarehouseStation> implements IWarehouseStationService {

    @Override
    public IPage<WarehouseStationVO> queryStationPage(int pageNo, int pageSize, String city, String keyword) {
        LambdaQueryWrapper<WarehouseStation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WarehouseStation::getStatus, StationConstant.STATION_STATUS_OPEN);
        if (StrUtil.isNotBlank(city)) {
            queryWrapper.and(wrapper -> wrapper.eq(WarehouseStation::getCity, city)
                    .or().like(WarehouseStation::getCoverArea, city));
        }
        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.and(wrapper -> wrapper.like(WarehouseStation::getStationName, keyword)
                    .or().like(WarehouseStation::getAddress, keyword)
                    .or().like(WarehouseStation::getCoverArea, keyword));
        }
        queryWrapper.orderByAsc(WarehouseStation::getSort)
                .orderByDesc(WarehouseStation::getCreateTime);

        IPage<WarehouseStation> pageList = this.page(new Page<>(pageNo, pageSize), queryWrapper);
        Page<WarehouseStationVO> voPage = new Page<>(pageList.getCurrent(), pageList.getSize(), pageList.getTotal());
        voPage.setRecords(pageList.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public WarehouseStationVO queryStationDetail(String stationId) {
        WarehouseStation station = this.getById(stationId);
        if (station == null) {
            throw new AppException(ExceptionEnum.STATION_NOT_EXIST);
        }
        return toVO(station);
    }

    private WarehouseStationVO toVO(WarehouseStation station) {
        WarehouseStationVO vo = new WarehouseStationVO();
        BeanUtil.copyProperties(station, vo);
        int total = station.getTotalCapacity() == null ? 0 : station.getTotalCapacity();
        int used = station.getUsedCapacity() == null ? 0 : station.getUsedCapacity();
        vo.setAvailableCapacity(Math.max(total - used, 0));
        return vo;
    }
}
