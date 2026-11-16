package org.jeecg.modules.app.controller.storage;

import cn.hutool.core.util.ObjectUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.app.bean.exception.AppException;
import org.jeecg.modules.app.bean.vo.storage.StorageCreateReqVO;
import org.jeecg.modules.app.bean.vo.storage.StorageDeleteReqVO;
import org.jeecg.modules.app.bean.vo.storage.StorageRenameReqVO;
import org.jeecg.modules.app.bean.vo.storage.UserStorageVO;
import org.jeecg.modules.app.entity.user.AppUser;
import org.jeecg.modules.app.entity.warehouse.UserStorage;
import org.jeecg.modules.app.service.IUserStorageService;
import org.jeecg.modules.app.utils.AppAuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户仓库（系统虚拟仓 + 真实个人仓）
 * 普通用户可创建 1 个真实个人仓，VIP 可创建 3 个
 */
@Tag(name = "用户仓库")
@Slf4j
@RestController
@RequestMapping("/app/storage")
public class AppStorageController {

    @Autowired
    private IUserStorageService userStorageService;

    @Operation(summary = "我的仓库列表(含系统虚拟仓)")
    @GetMapping(value = "/list")
    public Result<List<UserStorageVO>> list() {
        AppUser loginUser = AppAuthUtil.getUserInfo();
        if (ObjectUtil.isEmpty(loginUser)) {
            return Result.error("用户信息错误！");
        }
        return Result.OK(userStorageService.listUserStorages(loginUser.getId()));
    }

    @Operation(summary = "创建真实个人仓(非VIP上限1个,VIP上限3个)")
    @PostMapping(value = "/create")
    public Result<UserStorageVO> create(@RequestBody StorageCreateReqVO reqVO) {
        AppUser loginUser = AppAuthUtil.getUserInfo();
        if (ObjectUtil.isEmpty(loginUser)) {
            return Result.error("用户信息错误！");
        }
        if (reqVO == null || ObjectUtil.isEmpty(reqVO.getName())) {
            return Result.error("请求参数错误");
        }
        try {
            UserStorage storage = userStorageService.createStorage(loginUser.getId(), reqVO.getName());
            return Result.OK(new UserStorageVO()
                    .setId(storage.getId())
                    .setName(storage.getName())
                    .setStorageType(storage.getStorageType())
                    .setIsDefault(storage.getIsDefault())
                    .setCreateTime(storage.getCreateTime()));
        } catch (AppException e) {
            return Result.error(e.getMsg());
        }
    }

    @Operation(summary = "重命名仓库(仅真实仓)")
    @PostMapping(value = "/rename")
    public Result<String> rename(@RequestBody StorageRenameReqVO reqVO) {
        AppUser loginUser = AppAuthUtil.getUserInfo();
        if (ObjectUtil.isEmpty(loginUser)) {
            return Result.error("用户信息错误！");
        }
        if (reqVO == null || ObjectUtil.isEmpty(reqVO.getId()) || ObjectUtil.isEmpty(reqVO.getName())) {
            return Result.error("请求参数错误");
        }
        try {
            userStorageService.renameStorage(loginUser.getId(), reqVO.getId(), reqVO.getName());
            return Result.OK("重命名成功");
        } catch (AppException e) {
            return Result.error(e.getMsg());
        }
    }

    @Operation(summary = "删除仓库(软删,仅真实仓)")
    @PostMapping(value = "/delete")
    public Result<String> delete(@RequestBody StorageDeleteReqVO reqVO) {
        AppUser loginUser = AppAuthUtil.getUserInfo();
        if (ObjectUtil.isEmpty(loginUser)) {
            return Result.error("用户信息错误！");
        }
        if (reqVO == null || ObjectUtil.isEmpty(reqVO.getId())) {
            return Result.error("请求参数错误");
        }
        try {
            userStorageService.deleteStorage(loginUser.getId(), reqVO.getId());
            return Result.OK("删除成功");
        } catch (AppException e) {
            return Result.error(e.getMsg());
        }
    }

}
