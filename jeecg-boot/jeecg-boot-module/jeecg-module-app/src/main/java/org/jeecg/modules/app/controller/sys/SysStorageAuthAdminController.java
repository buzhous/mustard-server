package org.jeecg.modules.app.controller.sys;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.app.bean.enums.ExceptionEnum;
import org.jeecg.modules.app.bean.exception.AppException;
import org.jeecg.modules.app.bean.vo.storage.UserStorageVO;
import org.jeecg.modules.app.constant.StorageInConstant;
import org.jeecg.modules.app.entity.warehouse.UserStorage;
import org.jeecg.modules.app.mapper.UserStorageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

/**
 * 官方后台：自建仓平台认证审核
 * 路径 /sys/**，走 jeecg 后台 JWT 鉴权
 *
 * @author elinx
 */
@Slf4j
@Tag(name = "官方后台-自建仓认证审核")
@RestController
@RequestMapping("/sys/storageAuth")
public class SysStorageAuthAdminController {

    @Autowired
    private UserStorageMapper userStorageMapper;

    @Operation(summary = "认证审核列表（分页，默认待审核）")
    @GetMapping("/page")
    public Result<Page<UserStorageVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer authStatus) {
        Page<UserStorage> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<UserStorage> wrapper = new LambdaQueryWrapper<>();
        Integer status = authStatus == null ? StorageInConstant.AUTH_PENDING : authStatus;
        wrapper.eq(UserStorage::getAuthStatus, status);
        wrapper.orderByDesc(UserStorage::getAuthApplyTime);
        Page<UserStorage> result = userStorageMapper.selectPage(page, wrapper);
        Page<UserStorageVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream()
                .map(s -> cn.hutool.core.bean.BeanUtil.copyProperties(s, UserStorageVO.class))
                .toList());
        return Result.OK(voPage);
    }

    @Operation(summary = "认证审核（通过/驳回）")
    @PostMapping("/audit")
    public Result<String> audit(@RequestParam String storageId,
                                @RequestParam boolean approved,
                                @RequestParam(required = false) String reason) {
        UserStorage storage = userStorageMapper.selectById(storageId);
        if (storage == null) {
            throw new AppException(ExceptionEnum.STORAGE_NOT_EXIST);
        }
        if (!approved && (reason == null || reason.isBlank())) {
            throw new AppException(ExceptionEnum.REQUEST_PARAM_ERROR);
        }
        UserStorage update = new UserStorage()
                .setId(storageId)
                .setAuthStatus(approved ? StorageInConstant.AUTH_APPROVED : StorageInConstant.AUTH_REJECTED)
                .setAuthReason(approved ? null : reason)
                .setAuthTime(approved ? new Date() : null);
        userStorageMapper.updateById(update);
        return Result.OK(approved ? "已通过认证" : "已驳回");
    }
}
