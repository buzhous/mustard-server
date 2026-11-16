package org.jeecg.modules.app.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.app.bean.enums.ExceptionEnum;
import org.jeecg.modules.app.bean.exception.AppException;
import org.jeecg.modules.app.bean.vo.station.UserRealStationVO;
import org.jeecg.modules.app.constant.StationConstant;
import org.jeecg.modules.app.entity.warehouse.UserRealStation;
import org.jeecg.modules.app.mapper.UserRealStationMapper;
import org.jeecg.modules.app.service.IUserRealStationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 实物云仓-用户专属默认真实仓服务实现
 * 每个账号固定拥有 1 个默认真实仓（惰性初始化，幂等），
 * 支持修改收件人、电话、收件地址与定位（省市区根据定位回填）
 */
@Slf4j
@Service
public class UserRealStationServiceImpl extends ServiceImpl<UserRealStationMapper, UserRealStation>
        implements IUserRealStationService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserRealStation ensureUserRealStation(String userId) {
        UserRealStation exist = queryByUserId(userId);
        if (exist != null) {
            return exist;
        }
        UserRealStation station = new UserRealStation()
                .setUserId(userId)
                .setStationNo(StationConstant.buildRealStationNo(userId))
                .setReceiverName("")
                .setPhone("")
                .setAddress("")
                .setStatus(StationConstant.STATION_STATUS_OPEN);
        station.setCreateTime(new Date());
        station.setUpdateTime(new Date());
        baseMapper.insert(station);
        log.info("初始化用户默认真实仓 userId={}, stationNo={}", userId, station.getStationNo());
        return station;
    }

    @Override
    public UserRealStationVO getDetailByUser(String userId) {
        UserRealStation station = ensureUserRealStation(userId);
        return toVO(station);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserRealStationVO updateByUser(String userId, UserRealStationVO vo) {
        if (vo == null) {
            throw new AppException(ExceptionEnum.REQUEST_PARAM_ERROR);
        }
        UserRealStation station = ensureUserRealStation(userId);
        if (StrUtil.isNotEmpty(vo.getReceiverName())) {
            station.setReceiverName(vo.getReceiverName());
        }
        if (StrUtil.isNotEmpty(vo.getPhone())) {
            station.setPhone(vo.getPhone());
        }
        if (vo.getProvince() != null) {
            station.setProvince(StrUtil.isEmpty(vo.getProvince()) ? "" : vo.getProvince());
        }
        if (vo.getCity() != null) {
            station.setCity(StrUtil.isEmpty(vo.getCity()) ? "" : vo.getCity());
        }
        if (vo.getDistrict() != null) {
            station.setDistrict(StrUtil.isEmpty(vo.getDistrict()) ? "" : vo.getDistrict());
        }
        if (vo.getAddress() != null) {
            station.setAddress(StrUtil.isEmpty(vo.getAddress()) ? "" : vo.getAddress());
        }
        if (vo.getLongitude() != null) {
            station.setLongitude(BigDecimal.valueOf(vo.getLongitude().doubleValue()));
        }
        if (vo.getLatitude() != null) {
            station.setLatitude(BigDecimal.valueOf(vo.getLatitude().doubleValue()));
        }
        station.setUpdateTime(new Date());
        baseMapper.updateById(station);
        return toVO(station);
    }

    private UserRealStation queryByUserId(String userId) {
        return baseMapper.selectOne(new LambdaQueryWrapper<UserRealStation>()
                .eq(UserRealStation::getUserId, userId)
                .last("limit 1"));
    }

    private UserRealStationVO toVO(UserRealStation station) {
        if (ObjectUtil.isEmpty(station)) {
            return null;
        }
        return BeanUtil.copyProperties(station, UserRealStationVO.class);
    }
}
