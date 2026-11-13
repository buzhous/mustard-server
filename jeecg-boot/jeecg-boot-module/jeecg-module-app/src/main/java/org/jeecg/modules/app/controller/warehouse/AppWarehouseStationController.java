package org.jeecg.modules.app.controller.warehouse;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.app.bean.enums.ExceptionEnum;
import org.jeecg.modules.app.bean.vo.station.MyStationsVO;
import org.jeecg.modules.app.bean.vo.station.UserStationBindVO;
import org.jeecg.modules.app.bean.vo.station.UserVirtualStationVO;
import org.jeecg.modules.app.bean.vo.station.WarehouseStationVO;
import org.jeecg.modules.app.entity.user.AppUser;
import org.jeecg.modules.app.entity.warehouse.UserVirtualStation;
import org.jeecg.modules.app.service.IUserStationBindService;
import org.jeecg.modules.app.service.IUserVirtualStationService;
import org.jeecg.modules.app.service.IWarehouseStationService;
import org.jeecg.modules.app.utils.AppAuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 实物云仓-仓储站点（客户收寄点）
 */
@Slf4j
@RestController
@RequestMapping("/app/station")
@Tag(name = "实物云仓-仓储站点")
public class AppWarehouseStationController {

    @Autowired
    private IWarehouseStationService warehouseStationService;

    @Autowired
    private IUserStationBindService userStationBindService;

    @Autowired
    private IUserVirtualStationService userVirtualStationService;

    @Operation(summary = "平台站点列表（按城市/关键词筛选）")
    @GetMapping("/list")
    public Result<IPage<WarehouseStationVO>> list(
            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(name = "city", required = false) String city,
            @RequestParam(name = "keyword", required = false) String keyword) {
        IPage<WarehouseStationVO> page = warehouseStationService.queryStationPage(pageNo, pageSize, city, keyword);
        return Result.OK(page);
    }

    @Operation(summary = "站点详情")
    @GetMapping("/detail")
    public Result<WarehouseStationVO> detail(@RequestParam(name = "stationId") String stationId) {
        if (ObjectUtil.isEmpty(stationId)) {
            return Result.error(ExceptionEnum.REQUEST_PARAM_ERROR.getMsg());
        }
        return Result.OK(warehouseStationService.queryStationDetail(stationId));
    }

    @Operation(summary = "我的绑定站点列表（主站点在前）")
    @GetMapping("/my")
    public Result<List<UserStationBindVO>> my() {
        AppUser loginUser = AppAuthUtil.getUserInfo();
        if (ObjectUtil.isEmpty(loginUser)) {
            return Result.error(ExceptionEnum.USER_INFO_NOT_EXIST.getMsg());
        }
        return Result.OK(userStationBindService.listMyStations(loginUser.getId()));
    }

    @Operation(summary = "我的仓库点位聚合（虚拟仓固定1个 + 真实绑定站点列表）")
    @GetMapping("/myStations")
    public Result<MyStationsVO> myStations() {
        AppUser loginUser = AppAuthUtil.getUserInfo();
        if (ObjectUtil.isEmpty(loginUser)) {
            return Result.error(ExceptionEnum.USER_INFO_NOT_EXIST.getMsg());
        }
        UserVirtualStation virtual = userVirtualStationService.ensureUserVirtualStation(loginUser.getId());
        UserVirtualStationVO virtualVO = new UserVirtualStationVO()
                .setId(virtual.getId())
                .setStationNo(virtual.getStationNo())
                .setLocationDesc(virtual.getLocationDesc());
        MyStationsVO vo = new MyStationsVO()
                .setVirtual(virtualVO)
                .setReal(userStationBindService.listMyStations(loginUser.getId()));
        return Result.OK(vo);
    }

    @Operation(summary = "更新虚拟仓位置说明")
    @PostMapping("/virtual/locationDesc")
    public Result<String> updateVirtualLocation(@RequestBody Map<String, String> body) {
        AppUser loginUser = AppAuthUtil.getUserInfo();
        if (ObjectUtil.isEmpty(loginUser)) {
            return Result.error(ExceptionEnum.USER_INFO_NOT_EXIST.getMsg());
        }
        String locationDesc = body == null ? null : body.get("locationDesc");
        if (ObjectUtil.isEmpty(locationDesc)) {
            return Result.error(ExceptionEnum.REQUEST_PARAM_ERROR.getMsg());
        }
        userVirtualStationService.updateLocationDesc(loginUser.getId(), locationDesc);
        return Result.OK("更新成功");
    }

    @Operation(summary = "绑定站点")
    @PostMapping("/bind")
    public Result<String> bind(@RequestParam(name = "stationId") String stationId) {
        AppUser loginUser = AppAuthUtil.getUserInfo();
        if (ObjectUtil.isEmpty(loginUser)) {
            return Result.error(ExceptionEnum.USER_INFO_NOT_EXIST.getMsg());
        }
        if (ObjectUtil.isEmpty(stationId)) {
            return Result.error(ExceptionEnum.REQUEST_PARAM_ERROR.getMsg());
        }
        userStationBindService.bindStation(loginUser.getId(), stationId);
        return Result.OK("绑定成功", stationId);
    }

    @Operation(summary = "解绑站点")
    @PostMapping("/unbind")
    public Result<String> unbind(@RequestParam(name = "stationId") String stationId) {
        AppUser loginUser = AppAuthUtil.getUserInfo();
        if (ObjectUtil.isEmpty(loginUser)) {
            return Result.error(ExceptionEnum.USER_INFO_NOT_EXIST.getMsg());
        }
        if (ObjectUtil.isEmpty(stationId)) {
            return Result.error(ExceptionEnum.REQUEST_PARAM_ERROR.getMsg());
        }
        userStationBindService.unbindStation(loginUser.getId(), stationId);
        return Result.OK("解绑成功", stationId);
    }

    @Operation(summary = "设置主站点")
    @PostMapping("/setDefault")
    public Result<String> setDefault(@RequestParam(name = "stationId") String stationId) {
        AppUser loginUser = AppAuthUtil.getUserInfo();
        if (ObjectUtil.isEmpty(loginUser)) {
            return Result.error(ExceptionEnum.USER_INFO_NOT_EXIST.getMsg());
        }
        if (ObjectUtil.isEmpty(stationId)) {
            return Result.error(ExceptionEnum.REQUEST_PARAM_ERROR.getMsg());
        }
        userStationBindService.setDefaultStation(loginUser.getId(), stationId);
        return Result.OK("设置成功", stationId);
    }
}
