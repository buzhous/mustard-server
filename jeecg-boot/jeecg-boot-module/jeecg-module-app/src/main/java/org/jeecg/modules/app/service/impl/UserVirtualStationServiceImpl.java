package org.jeecg.modules.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.constant.CommonConstant;
import org.jeecg.modules.app.constant.StationConstant;
import org.jeecg.modules.app.entity.warehouse.UserVirtualStation;
import org.jeecg.modules.app.mapper.UserVirtualStationMapper;
import org.jeecg.modules.app.service.IUserVirtualStationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 实物云仓-用户虚拟仓库点服务实现
 * 保证每个用户固定拥有 1 个虚拟仓库点（惰性初始化，幂等）
 *
 * @author elinx
 */
@Slf4j
@Service
public class UserVirtualStationServiceImpl extends ServiceImpl<UserVirtualStationMapper, UserVirtualStation>
        implements IUserVirtualStationService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVirtualStation ensureUserVirtualStation(String userId) {
        UserVirtualStation exist = queryByUserId(userId);
        if (exist != null) {
            return exist;
        }
        UserVirtualStation station = new UserVirtualStation()
                .setUserId(userId)
                .setStationNo(StationConstant.buildVirtualStationNo(userId))
                .setLocationDesc(StationConstant.DEFAULT_VIRTUAL_LOCATION_DESC);
        station.setCreateTime(new Date());
        station.setUpdateTime(new Date());
        baseMapper.insert(station);
        log.info("初始化用户虚拟仓库点 userId={}, stationNo={}", userId, station.getStationNo());
        return station;
    }

    @Override
    public UserVirtualStation queryByUserId(String userId) {
        return baseMapper.selectOne(new LambdaQueryWrapper<UserVirtualStation>()
                .eq(UserVirtualStation::getUserId, userId)
                .last("limit 1"));
    }

    @Override
    public boolean updateLocationDesc(String userId, String locationDesc) {
        UserVirtualStation station = queryByUserId(userId);
        if (station == null) {
            station = ensureUserVirtualStation(userId);
        }
        station.setLocationDesc(locationDesc);
        station.setUpdateTime(new Date());
        return baseMapper.updateById(station) > 0;
    }

    @Override
    public boolean updateCustomName(String userId, String customName) {
        UserVirtualStation station = queryByUserId(userId);
        if (station == null) {
            station = ensureUserVirtualStation(userId);
        }
        station.setCustomName(cn.hutool.core.util.StrUtil.isEmpty(customName) ? null : customName.trim());
        station.setUpdateTime(new Date());
        return baseMapper.updateById(station) > 0;
    }
}
