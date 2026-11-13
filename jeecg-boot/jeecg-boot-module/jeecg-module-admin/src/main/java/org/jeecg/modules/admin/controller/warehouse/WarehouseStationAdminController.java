package org.jeecg.modules.admin.controller.warehouse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.modules.admin.constant.StationConstant;
import org.jeecg.modules.admin.entity.warehouse.UserStationBind;
import org.jeecg.modules.admin.entity.warehouse.WarehouseStation;
import org.jeecg.modules.admin.service.IUserStationBindAdminService;
import org.jeecg.modules.admin.service.IWarehouseStationAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 实物云仓-仓储站点 后台管理
 */
@Slf4j
@RestController
@RequestMapping("/warehouse/station")
@Tag(name = "实物云仓-仓储站点管理")
public class WarehouseStationAdminController extends JeecgController<WarehouseStation, IWarehouseStationAdminService> {

    @Autowired
    private IWarehouseStationAdminService warehouseStationService;

    @Autowired
    private IUserStationBindAdminService userStationBindService;

    @Operation(summary = "站点分页列表查询")
    @GetMapping(value = "/list")
    public Result<?> queryPageList(WarehouseStation station,
                                   @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                   @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                   HttpServletRequest req) {
        QueryWrapper<WarehouseStation> queryWrapper = QueryGenerator.initQueryWrapper(station, req.getParameterMap());
        Page<WarehouseStation> page = new Page<>(pageNo, pageSize);
        IPage<WarehouseStation> pageList = warehouseStationService.page(page, queryWrapper);
        return Result.ok(pageList);
    }

    @Operation(summary = "站点详情")
    @GetMapping(value = "/queryById")
    public Result<?> queryById(@RequestParam(name = "id") String id) {
        WarehouseStation station = warehouseStationService.getById(id);
        if (station == null) {
            return Result.error("未找到对应数据");
        }
        return Result.ok(station);
    }

    @Operation(summary = "新增站点")
    @PostMapping(value = "/add")
    public Result<?> add(@RequestBody WarehouseStation station) {
        if (checkCodeExist(station.getStationCode(), null)) {
            return Result.error("站点编码已存在：" + station.getStationCode());
        }
        Date now = new Date();
        station.setId(null);
        station.setCreateTime(now);
        station.setUpdateTime(now);
        if (station.getStatus() == null) {
            station.setStatus(StationConstant.STATION_STATUS_OPEN);
        }
        warehouseStationService.save(station);
        return Result.ok("添加成功！");
    }

    @Operation(summary = "编辑站点")
    @PutMapping(value = "/edit")
    public Result<?> edit(@RequestBody WarehouseStation station) {
        if (station.getId() == null) {
            return Result.error("缺少主键id");
        }
        WarehouseStation exist = warehouseStationService.getById(station.getId());
        if (exist == null) {
            return Result.error("未找到对应数据");
        }
        if (checkCodeExist(station.getStationCode(), station.getId())) {
            return Result.error("站点编码已存在：" + station.getStationCode());
        }
        // 容量调整不允许小于已用容量
        int used = exist.getUsedCapacity() == null ? 0 : exist.getUsedCapacity();
        if (station.getTotalCapacity() != null && station.getTotalCapacity() < used) {
            return Result.error("库存总容量不可小于已用容量（" + used + "件）");
        }
        station.setUpdateTime(new Date());
        warehouseStationService.updateById(station);
        return Result.ok("更新成功！");
    }

    @Operation(summary = "通过id删除站点（存在用户绑定时不可删）")
    @DeleteMapping(value = "/delete")
    public Result<?> delete(@RequestParam(name = "id") String id) {
        if (checkBindExist(Arrays.asList(id))) {
            return Result.error("该站点存在用户绑定，不可删除，请先下架处理");
        }
        warehouseStationService.removeById(id);
        return Result.ok("删除成功!");
    }

    @Operation(summary = "批量删除站点（存在用户绑定时不可删）")
    @DeleteMapping(value = "/deleteBatch")
    public Result<?> deleteBatch(@RequestParam(name = "ids") String ids) {
        List<String> idList = Arrays.asList(ids.split(","));
        if (checkBindExist(idList)) {
            return Result.error("选中站点存在用户绑定，不可删除，请先下架处理");
        }
        warehouseStationService.removeByIds(idList);
        return Result.ok("批量删除成功!");
    }

    @Operation(summary = "站点列表（不分页，供下拉选择）")
    @GetMapping(value = "/selectAll")
    public Result<?> selectAll() {
        List<WarehouseStation> list = warehouseStationService.list(
                new LambdaQueryWrapper<WarehouseStation>()
                        .eq(WarehouseStation::getStatus, StationConstant.STATION_STATUS_OPEN)
                        .orderByAsc(WarehouseStation::getSort));
        return Result.ok(list);
    }

    @Operation(summary = "导出excel")
    @RequestMapping(value = "/exportXls")
    public org.springframework.web.servlet.ModelAndView exportXls(HttpServletRequest request, WarehouseStation station) {
        return super.exportXls(request, station, WarehouseStation.class, "仓储站点");
    }

    @Operation(summary = "excel导入")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, WarehouseStation.class);
    }

    private boolean checkCodeExist(String stationCode, String excludeId) {
        if (stationCode == null || stationCode.isBlank()) {
            return false;
        }
        LambdaQueryWrapper<WarehouseStation> wrapper = new LambdaQueryWrapper<WarehouseStation>()
                .eq(WarehouseStation::getStationCode, stationCode);
        if (excludeId != null) {
            wrapper.ne(WarehouseStation::getId, excludeId);
        }
        return warehouseStationService.count(wrapper) > 0;
    }

    private boolean checkBindExist(List<String> stationIds) {
        long binds = userStationBindService.count(new LambdaQueryWrapper<UserStationBind>()
                .in(UserStationBind::getStationId, stationIds)
                .eq(UserStationBind::getStatus, StationConstant.BIND_STATUS_BOUND));
        return binds > 0;
    }
}
