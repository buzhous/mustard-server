package org.jeecg.modules.app.controller.sys;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.app.bean.vo.storagein.StorageInDetailVO;
import org.jeecg.modules.app.bean.vo.storagein.StorageInListVO;
import org.jeecg.modules.app.entity.ItemStorageIn;
import org.jeecg.modules.app.service.IItemStorageInService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 官方后台：入库单管理（确认官方入库）
 * 路径 /sys/**，走 jeecg 后台 JWT 鉴权
 *
 * @author elinx
 */
@Slf4j
@Tag(name = "官方后台-入库单")
@RestController
@RequestMapping("/sys/storageIn")
public class SysStorageInAdminController {

    @Autowired
    private IItemStorageInService itemStorageInService;

    @Operation(summary = "入库/移库单分页（按单号/状态筛选）")
    @GetMapping("/page")
    public Result<Page<StorageInListVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String depositNo,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer bizType) {
        return Result.OK(itemStorageInService.adminPage(pageNo, pageSize, depositNo, status, bizType));
    }

    @Operation(summary = "确认官方入库（16已邮寄 → 25已入库）")
    @PostMapping("/confirm")
    public Result<String> confirm(@RequestParam String recordId) {
        itemStorageInService.adminConfirmOfficial(recordId);
        return Result.OK("已确认入库");
    }

    @Operation(summary = "单据详情（后台）")
    @GetMapping("/detail")
    public Result<StorageInDetailVO> detail(@RequestParam String recordId) {
        return Result.OK(itemStorageInService.adminDetail(recordId));
    }
}
