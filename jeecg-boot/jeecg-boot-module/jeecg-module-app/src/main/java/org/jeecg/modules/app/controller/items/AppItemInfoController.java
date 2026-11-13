package org.jeecg.modules.app.controller.items;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.app.bean.enums.ExceptionEnum;
import org.jeecg.modules.app.bean.vo.item.ItemInfoQueryVO;
import org.jeecg.modules.app.entity.ItemInfo;
import org.jeecg.modules.app.entity.user.AppUser;
import org.jeecg.modules.app.service.IItemInfoService;
import org.jeecg.modules.app.utils.AppAuthUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * App 物品查询接口（结构化查询，区别于 /app/queue/itemSync 增量同步）
 */
@Slf4j
@RestController
@RequestMapping("/app/item")
@Tag(name = "App-物品查询")
public class AppItemInfoController {

    private final IItemInfoService itemInfoService;

    public AppItemInfoController(IItemInfoService itemInfoService) {
        this.itemInfoService = itemInfoService;
    }

    @Operation(summary = "物品分页查询（关键字/仓位/状态/排序）")
    @GetMapping("/list")
    public Result<Page<ItemInfo>> list(
            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "stationId", required = false) String stationId,
            @RequestParam(name = "stationType", required = false) Integer stationType,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "orderBy", required = false) String orderBy,
            @RequestParam(name = "ascending", required = false, defaultValue = "false") Boolean ascending) {
        AppUser loginUser = AppAuthUtil.getUserInfo();
        if (ObjectUtil.isEmpty(loginUser)) {
            return Result.error(ExceptionEnum.USER_INFO_NOT_EXIST.getMsg());
        }
        ItemInfoQueryVO queryVO = new ItemInfoQueryVO()
                .setUserId(loginUser.getId())
                .setKeyword(keyword)
                .setStationId(stationId)
                .setStationType(stationType)
                .setStatus(status)
                .setOrderBy(orderBy)
                .setAscending(ascending)
                .setPageNo(pageNo)
                .setPageSize(Math.min(pageSize, 100));
        return Result.OK(itemInfoService.listByCondition(queryVO));
    }

    @Operation(summary = "物品详情")
    @GetMapping("/detail")
    public Result<ItemInfo> detail(@RequestParam(name = "itemId") String itemId) {
        AppUser loginUser = AppAuthUtil.getUserInfo();
        if (ObjectUtil.isEmpty(loginUser)) {
            return Result.error(ExceptionEnum.USER_INFO_NOT_EXIST.getMsg());
        }
        if (ObjectUtil.isEmpty(itemId)) {
            return Result.error(ExceptionEnum.REQUEST_PARAM_ERROR.getMsg());
        }
        ItemInfo itemInfo = itemInfoService.queryItemInfo(itemId);
        if (ObjectUtil.isEmpty(itemInfo) || !loginUser.getId().equals(itemInfo.getUserId())) {
            return Result.error(ExceptionEnum.ITEM_NOT_EXIST.getMsg());
        }
        return Result.OK(itemInfo);
    }
}
