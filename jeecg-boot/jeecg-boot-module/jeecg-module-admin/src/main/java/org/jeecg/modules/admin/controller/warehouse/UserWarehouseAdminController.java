package org.jeecg.modules.admin.controller.warehouse;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.modules.admin.bean.vo.warehouse.UserWarehouseDetailVO;
import org.jeecg.modules.admin.constant.StationConstant;
import org.jeecg.modules.admin.entity.warehouse.UserStorage;
import org.jeecg.modules.admin.entity.warehouse.UserVirtualStation;
import org.jeecg.modules.admin.entity.warehouse.WarehouseStation;
import org.jeecg.modules.admin.mapper.UserStorageAdminMapper;
import org.jeecg.modules.admin.mapper.UserVirtualStationAdminMapper;
import org.jeecg.modules.admin.mapper.UserWarehouseMapper;
import org.jeecg.modules.admin.service.IUserStationBindAdminService;
import org.jeecg.modules.admin.service.IWarehouseStationAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户仓库查看 后台管理（只读：虚拟仓 + 自建真实仓 + 官方绑定仓）
 */
@Slf4j
@RestController
@RequestMapping("/warehouse/userStorage")
@Tag(name = "用户仓库查看")
public class UserWarehouseAdminController {

    @Autowired
    private UserStorageAdminMapper userStorageMapper;

    @Autowired
    private UserVirtualStationAdminMapper userVirtualStationMapper;

    @Autowired
    private UserWarehouseMapper userWarehouseMapper;

    @Autowired
    private IWarehouseStationAdminService warehouseStationService;

    @Autowired
    private IUserStationBindAdminService userStationBindService;

    @Operation(summary = "自建真实仓分页列表（userKey支持手机号/用户名/用户Id）")
    @GetMapping(value = "/page")
    public Result<?> page(UserStorage userStorage,
                          @RequestParam(name = "userKey", required = false) String userKey,
                          @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                          @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                          HttpServletRequest req) {
        if (StrUtil.isNotBlank(userKey)) {
            Map<String, Object> user = userWarehouseMapper.selectUserInfoByKey(userKey.trim());
            if (user == null) {
                return Result.error("未找到用户：" + userKey);
            }
            userStorage.setUserId(String.valueOf(user.get("id")));
        }
        QueryWrapper<UserStorage> queryWrapper = QueryGenerator.initQueryWrapper(userStorage, req.getParameterMap());
        queryWrapper.orderByDesc("create_time");
        Page<UserStorage> page = new Page<>(pageNo, pageSize);
        IPage<UserStorage> pageList = userStorageMapper.selectPage(page, queryWrapper);
        fillUserInfo(pageList.getRecords());
        return Result.ok(pageList);
    }

    @Operation(summary = "用户仓库详情（虚拟仓 + 自建真实仓 + 官方绑定仓，userKey支持手机号/用户名/用户Id）")
    @GetMapping(value = "/detail")
    public Result<?> detail(@RequestParam(name = "userKey") String userKey) {
        if (StrUtil.isBlank(userKey)) {
            return Result.error("请输入手机号/用户名/用户Id");
        }
        Map<String, Object> user = userWarehouseMapper.selectUserInfoByKey(userKey.trim());
        if (user == null) {
            return Result.error("未找到用户：" + userKey);
        }
        String userId = String.valueOf(user.get("id"));

        UserWarehouseDetailVO vo = new UserWarehouseDetailVO();
        UserWarehouseDetailVO.UserInfo userInfo = new UserWarehouseDetailVO.UserInfo();
        userInfo.setId(userId);
        userInfo.setPhone(user.get("phone") == null ? "" : String.valueOf(user.get("phone")));
        userInfo.setNickname(user.get("nickname") == null ? "" : String.valueOf(user.get("nickname")));
        vo.setUser(userInfo);

        UserVirtualStation virtual = userVirtualStationMapper.selectOne(new LambdaQueryWrapper<UserVirtualStation>()
                .eq(UserVirtualStation::getUserId, userId)
                .last("limit 1"));
        vo.setVirtual(virtual);

        vo.setStorages(userStorageMapper.selectList(new LambdaQueryWrapper<UserStorage>()
                .eq(UserStorage::getUserId, userId)
                .orderByAsc(UserStorage::getCreateTime)));

        // 官方绑定仓（含已解绑记录，后台可查看全量状态）
        List<org.jeecg.modules.admin.entity.warehouse.UserStationBind> binds = userStationBindService.list(
                new LambdaQueryWrapper<org.jeecg.modules.admin.entity.warehouse.UserStationBind>()
                        .eq(org.jeecg.modules.admin.entity.warehouse.UserStationBind::getUserId, userId)
                        .orderByDesc(org.jeecg.modules.admin.entity.warehouse.UserStationBind::getBindTime));
        Map<String, WarehouseStation> stationMap = binds.isEmpty()
                ? Map.of()
                : warehouseStationService.listByIds(binds.stream()
                        .map(org.jeecg.modules.admin.entity.warehouse.UserStationBind::getStationId)
                        .collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(WarehouseStation::getId, Function.identity(), (a, b) -> a));
        List<UserWarehouseDetailVO.BindInfo> bindInfos = new ArrayList<>();
        for (org.jeecg.modules.admin.entity.warehouse.UserStationBind bind : binds) {
            UserWarehouseDetailVO.BindInfo info = new UserWarehouseDetailVO.BindInfo();
            info.setId(bind.getId());
            info.setStationId(bind.getStationId());
            info.setCustomName(bind.getCustomName());
            info.setIsDefault(bind.getIsDefault());
            info.setStatus(bind.getStatus());
            info.setBindTime(bind.getBindTime());
            WarehouseStation station = stationMap.get(bind.getStationId());
            if (station != null) {
                info.setStationName(station.getStationName());
                info.setStationCode(station.getStationCode());
            }
            bindInfos.add(info);
        }
        vo.setBinds(bindInfos);
        return Result.ok(vo);
    }

    private void fillUserInfo(List<UserStorage> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<String> userIds = records.stream()
                .map(UserStorage::getUserId)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return;
        }
        Map<String, Map<String, Object>> userMap = userWarehouseMapper.selectUserInfos(userIds).stream()
                .collect(Collectors.toMap(m -> String.valueOf(m.get("id")), m -> m, (a, b) -> a));
        records.forEach(r -> {
            Map<String, Object> user = userMap.get(r.getUserId());
            if (user != null) {
                r.setUserPhone(user.get("phone") == null ? "" : String.valueOf(user.get("phone")));
                r.setNickname(user.get("nickname") == null ? "" : String.valueOf(user.get("nickname")));
            }
        });
    }
}
