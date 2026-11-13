package org.jeecg.modules.app.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.app.bean.enums.ExceptionEnum;
import org.jeecg.modules.app.bean.exception.AppException;
import org.jeecg.modules.app.bean.vo.station.UserStationBindVO;
import org.jeecg.modules.app.bean.vo.station.WarehouseStationVO;
import org.jeecg.modules.app.constant.StationConstant;
import org.jeecg.modules.app.entity.warehouse.UserStationBind;
import org.jeecg.modules.app.entity.warehouse.WarehouseStation;
import org.jeecg.modules.app.mapper.UserStationBindMapper;
import org.jeecg.modules.app.service.IUserStationBindService;
import org.jeecg.modules.app.service.IWarehouseStationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserStationBindServiceImpl extends ServiceImpl<UserStationBindMapper, UserStationBind> implements IUserStationBindService {

    @Autowired
    private IWarehouseStationService warehouseStationService;

    @Override
    public List<UserStationBindVO> listMyStations(String userId) {
        List<UserStationBind> binds = listBinds(userId);
        if (binds.isEmpty()) {
            return List.of();
        }
        Map<String, WarehouseStation> stationMap = warehouseStationService.listByIds(
                        binds.stream().map(UserStationBind::getStationId).collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(WarehouseStation::getId, Function.identity()));

        return binds.stream()
                .filter(bind -> stationMap.containsKey(bind.getStationId()))
                .map(bind -> {
                    UserStationBindVO vo = new UserStationBindVO();
                    BeanUtil.copyProperties(bind, vo);
                    WarehouseStationVO stationVO = new WarehouseStationVO();
                    BeanUtil.copyProperties(stationMap.get(bind.getStationId()), stationVO);
                    int total = stationMap.get(bind.getStationId()).getTotalCapacity() == null ? 0 : stationMap.get(bind.getStationId()).getTotalCapacity();
                    int used = stationMap.get(bind.getStationId()).getUsedCapacity() == null ? 0 : stationMap.get(bind.getStationId()).getUsedCapacity();
                    stationVO.setAvailableCapacity(Math.max(total - used, 0));
                    vo.setStation(stationVO);
                    return vo;
                })
                // 主站点在前，其余按绑定时间正序
                .sorted(Comparator
                        .comparing((UserStationBindVO vo) -> ObjectUtil.defaultIfNull(vo.getIsDefault(), 0) == StationConstant.IS_DEFAULT_YES ? 0 : 1)
                        .thenComparing(vo -> vo.getBindTime() == null ? 0 : vo.getBindTime().getTime()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindStation(String userId, String stationId) {
        WarehouseStation station = warehouseStationService.getById(stationId);
        if (ObjectUtil.isEmpty(station)) {
            throw new AppException(ExceptionEnum.STATION_NOT_EXIST);
        }
        if (ObjectUtil.defaultIfNull(station.getStatus(), StationConstant.STATION_STATUS_DISABLED) != StationConstant.STATION_STATUS_OPEN) {
            throw new AppException(ExceptionEnum.STATION_NOT_AVAILABLE);
        }
        if (ObjectUtil.isNotEmpty(getBind(userId, stationId))) {
            throw new AppException(ExceptionEnum.STATION_ALREADY_BOUND);
        }
        long boundCount = listBinds(userId).size();
        if (boundCount >= StationConstant.MAX_BIND_COUNT_NORMAL) {
            throw new AppException(ExceptionEnum.STATION_BIND_LIMIT_EXCEEDED,
                    "当前等级最多可绑定" + StationConstant.MAX_BIND_COUNT_NORMAL + "个仓储站点");
        }

        Date now = DateUtil.date();
        UserStationBind bind = new UserStationBind()
                .setUserId(userId)
                .setStationId(stationId)
                .setIsDefault(boundCount == 0 ? StationConstant.IS_DEFAULT_YES : StationConstant.IS_DEFAULT_NO)
                .setStatus(StationConstant.BIND_STATUS_BOUND)
                .setBindTime(now);
        bind.setCreateTime(now);
        bind.setUpdateTime(now);
        return this.save(bind);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unbindStation(String userId, String stationId) {
        UserStationBind bind = getBind(userId, stationId);
        if (ObjectUtil.isEmpty(bind)) {
            throw new AppException(ExceptionEnum.STATION_NOT_BOUND);
        }

        boolean wasDefault = ObjectUtil.defaultIfNull(bind.getIsDefault(), StationConstant.IS_DEFAULT_NO) == StationConstant.IS_DEFAULT_YES;
        Date now = DateUtil.date();
        bind.setStatus(StationConstant.BIND_STATUS_UNBOUND);
        bind.setIsDefault(StationConstant.IS_DEFAULT_NO);
        bind.setUnbindTime(now);
        bind.setUpdateTime(now);
        boolean result = this.updateById(bind);

        // 解绑的是主站点时，将最早绑定的剩余站点升级为主站点
        if (result && wasDefault) {
            promoteEarliestAsDefault(userId);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setDefaultStation(String userId, String stationId) {
        UserStationBind bind = getBind(userId, stationId);
        if (ObjectUtil.isEmpty(bind)) {
            throw new AppException(ExceptionEnum.STATION_NOT_BOUND);
        }
        if (ObjectUtil.defaultIfNull(bind.getIsDefault(), StationConstant.IS_DEFAULT_NO) == StationConstant.IS_DEFAULT_YES) {
            return true;
        }
        // 先清空原主站点
        UserStationBind clear = new UserStationBind()
                .setIsDefault(StationConstant.IS_DEFAULT_NO)
                .setUpdateTime(DateUtil.date());
        this.update(clear, new LambdaQueryWrapper<UserStationBind>()
                .eq(UserStationBind::getUserId, userId)
                .eq(UserStationBind::getStatus, StationConstant.BIND_STATUS_BOUND)
                .eq(UserStationBind::getIsDefault, StationConstant.IS_DEFAULT_YES));

        bind.setIsDefault(StationConstant.IS_DEFAULT_YES);
        bind.setUpdateTime(DateUtil.date());
        return this.updateById(bind);
    }

    private List<UserStationBind> listBinds(String userId) {
        return this.list(new LambdaQueryWrapper<UserStationBind>()
                .eq(UserStationBind::getUserId, userId)
                .eq(UserStationBind::getStatus, StationConstant.BIND_STATUS_BOUND)
                .orderByAsc(UserStationBind::getBindTime));
    }

    private UserStationBind getBind(String userId, String stationId) {
        return this.getOne(new LambdaQueryWrapper<UserStationBind>()
                .eq(UserStationBind::getUserId, userId)
                .eq(UserStationBind::getStationId, stationId)
                .eq(UserStationBind::getStatus, StationConstant.BIND_STATUS_BOUND)
                .last("limit 1"));
    }

    private void promoteEarliestAsDefault(String userId) {
        List<UserStationBind> remains = listBinds(userId);
        if (remains.isEmpty()) {
            return;
        }
        UserStationBind earliest = remains.get(0);
        earliest.setIsDefault(StationConstant.IS_DEFAULT_YES);
        earliest.setUpdateTime(DateUtil.date());
        this.updateById(earliest);
    }
}
