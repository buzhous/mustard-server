package org.jeecg.modules.app.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.app.bean.enums.ExceptionEnum;
import org.jeecg.modules.app.bean.exception.AppException;
import org.jeecg.modules.app.bean.vo.category.UserCategoryVO;
import org.jeecg.modules.app.entity.SystemCategory;
import org.jeecg.modules.app.entity.setting.UserCategory;
import org.jeecg.modules.app.mapper.UserCategoryMapper;
import org.jeecg.modules.app.service.ISystemCategoryService;
import org.jeecg.modules.app.service.IUserCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;


@Service
public class UserCategoryServiceImpl extends ServiceImpl<UserCategoryMapper, UserCategory> implements IUserCategoryService {

    /** 系统分类启用状态（1启用 0停用） */
    private static final int SYS_CATEGORY_ENABLED = 1;

    @Autowired
    private ISystemCategoryService systemCategoryService;

    @Override
    public IPage<UserCategoryVO> queryCategories(String userId) {
        LambdaQueryWrapper<UserCategory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserCategory::getUserId, userId);
        Page<UserCategory> page = new Page<UserCategory>(1, 999);
        IPage<UserCategory> pageList = this.page(page, queryWrapper);

        IPage<UserCategoryVO> voPage = new Page<>(page.getPages(), page.getSize());
        // 转换为VO列表
        voPage.setRecords(pageList.getRecords().stream().map(userCategory -> {
            UserCategoryVO vo = new UserCategoryVO();
            BeanUtil.copyProperties(userCategory, vo);
            vo.setUserId(null);
            return vo;
        }).collect(Collectors.toList()));

        return voPage;
    }

    @Override
    public boolean addUserCategory(UserCategoryVO add) {
        // C3 治理：名称与启用系统分类重名时「以系统分类为准」——不产生平行分类，直接走系统分类关联/实例化
        SystemCategory sys = queryEnabledSystemByName(add.getName());
        if (sys != null) {
            add.setName(sys.getName());
            add.setIcon(sys.getIcon());
            return instantiateFromSystem(add, sys);
        }
        return saveUserCategory(add);
    }

    @Override
    public UserCategoryVO addBySystemCategory(String userId, String systemCategoryId) {
        SystemCategory sys = systemCategoryService.getOne(new LambdaQueryWrapper<SystemCategory>()
                .eq(SystemCategory::getId, systemCategoryId)
                .eq(SystemCategory::getStatus, SYS_CATEGORY_ENABLED)
                .last("limit 1"), false);
        if (sys == null) {
            return null;
        }
        UserCategoryVO vo = new UserCategoryVO();
        vo.setUserId(userId);
        vo.setName(sys.getName());
        vo.setIcon(sys.getIcon());
        instantiateFromSystem(vo, sys);
        return vo;
    }

    /**
     * 系统分类实例化（共享链，"以系统分类为准"）：
     * ① 用户已关联该系统分类 → 幂等返回既有行；② 同名用户分类已存在 → 回填 system_id/isSys 关联；
     * ③ 否则创建新行（name/icon 取系统分类，add.vo 须已填 userId/name/icon）。
     */
    private boolean instantiateFromSystem(UserCategoryVO add, SystemCategory sys) {
        // ① 已关联该系统分类：幂等返回
        UserCategory linked = this.getOne(new LambdaQueryWrapper<UserCategory>()
                .eq(UserCategory::getUserId, add.getUserId())
                .eq(UserCategory::getSystemId, sys.getId())
                .last("limit 1"), false);
        if (linked != null) {
            add.setId(linked.getId());
            return true;
        }
        // ② 同名用户分类已存在：回填来源关联（历史自由分类收敛进系统体系）
        UserCategory sameName = this.getOne(new LambdaQueryWrapper<UserCategory>()
                .eq(UserCategory::getUserId, add.getUserId())
                .eq(UserCategory::getName, sys.getName())
                .last("limit 1"), false);
        if (sameName != null) {
            UserCategory update = new UserCategory();
            update.setId(sameName.getId());
            update.setSystemId(sys.getId());
            update.setIsSys(1);
            update.setUpdateTime(DateUtil.date());
            this.updateById(update);
            add.setId(sameName.getId());
            return true;
        }
        // ③ 按系统分类创建（名称/图标以系统分类为准，调用方已覆盖）
        return saveSystemLinked(add, sys);
    }

    /** 创建带系统来源的新行（③ 分支的落库） */
    private boolean saveSystemLinked(UserCategoryVO add, SystemCategory sys) {
        UserCategory userCategory = new UserCategory();
        userCategory.setUserId(add.getUserId());
        userCategory.setName(sys.getName());
        userCategory.setIcon(StrUtil.blankToDefault(sys.getIcon(), "other"));
        userCategory.setSystemId(sys.getId());
        userCategory.setIsSys(1);
        userCategory.setCreateTime(DateUtil.date());
        userCategory.setUpdateTime(DateUtil.date());
        boolean saved = this.save(userCategory);
        if (saved) {
            add.setId(userCategory.getId());
            add.setSystemId(sys.getId());
            add.setIsSys(1);
        }
        return saved;
    }

    /** 普通创建（无系统分类治理路径）：按 userId+name 查重幂等返回 */
    private boolean saveUserCategory(UserCategoryVO add) {
        UserCategory userCategory = new UserCategory();
        BeanUtil.copyProperties(add, userCategory);
        userCategory.setCreateTime(DateUtil.date());
        userCategory.setUpdateTime(DateUtil.date());

        LambdaQueryWrapper<UserCategory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserCategory::getUserId, userCategory.getUserId());
        queryWrapper.eq(UserCategory::getName, userCategory.getName());
        queryWrapper.last("limit 1");

        UserCategory existingCategory = this.getOne(queryWrapper);
        if (existingCategory != null) {
            add.setId(existingCategory.getId());
            return true;
        }
        // 分类数量不设用户等级限制（V2.3：移除普通用户 5 个上限，VIP 权益不再含此项）；重名幂等返回
        boolean result = this.save(userCategory);
        if (!result) {
            return false;
        }
        add.setId(userCategory.getId());
        return true;
    }

    /** 启用系统分类按名称精确查询（C3 治理入口判断） */
    private SystemCategory queryEnabledSystemByName(String name) {
        if (ObjectUtil.isEmpty(name)) {
            return null;
        }
        return systemCategoryService.getOne(new LambdaQueryWrapper<SystemCategory>()
                .eq(SystemCategory::getName, name.trim())
                .eq(SystemCategory::getStatus, SYS_CATEGORY_ENABLED)
                .last("limit 1"), false);
    }

    @Override
    public boolean updateUserCategory(UserCategoryVO update) {
        if (ObjectUtil.isEmpty(update.getId()) || ObjectUtil.isEmpty(update.getUserId())) {
            return false;
        }
        // C3 治理：改名撞启用系统分类名 → 拒绝（目标行已关联该系统分类时放行，名称本来就是系统名）
        if (ObjectUtil.isNotEmpty(update.getName())) {
            SystemCategory sys = queryEnabledSystemByName(update.getName());
            if (sys != null) {
                UserCategory target = this.getById(update.getId());
                if (target == null || !sys.getId().equals(target.getSystemId())) {
                    throw new AppException(ExceptionEnum.CATEGORY_NAME_CONFLICT);
                }
            }
        }
        LambdaQueryWrapper<UserCategory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserCategory::getUserId, update.getUserId());
        queryWrapper.eq(UserCategory::getId, update.getId());
        queryWrapper.last("limit 1");
        UserCategory existingCategory = this.getOne(queryWrapper);
        if (ObjectUtil.isEmpty(existingCategory)) {
            return false;
        }
        UserCategory userCategory = new UserCategory();
        BeanUtil.copyProperties(update, userCategory);
        userCategory.setUpdateTime(DateUtil.date());
        return this.updateById(userCategory);
    }

    @Override
    public boolean deleteUserCategory(String id, String userId) {
        LambdaQueryWrapper<UserCategory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserCategory::getId, id);
        queryWrapper.eq(UserCategory::getUserId, userId);
        return this.remove(queryWrapper);
    }

}
