package org.jeecg.modules.app.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.app.bean.vo.category.SystemCategoryVO;
import org.jeecg.modules.app.entity.SystemCategory;
import org.jeecg.modules.app.entity.setting.UserCategory;
import org.jeecg.modules.app.mapper.SystemCategoryMapper;
import org.jeecg.modules.app.mapper.UserCategoryMapper;
import org.jeecg.modules.app.service.ISystemCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 系统物品分类服务实现（C3 分类治理）：
 * 种子数据由 flyway V3.9.7_1 初始化；运营治理（增停改）走管理端接口。
 * App 端「列表带已关联标记」在此组装（系统分类 + 当前用户 system_id 集合）。
 */
@Service
public class SystemCategoryServiceImpl extends ServiceImpl<SystemCategoryMapper, SystemCategory>
        implements ISystemCategoryService {

    /** 系统分类启用状态（1启用 0停用） */
    private static final int SYS_CATEGORY_ENABLED = 1;

    @Autowired
    private UserCategoryMapper userCategoryMapper;

    /**
     * App 端系统分类列表（启用态，sort 升序），带当前用户「已关联」标记：
     * added = 该用户 tbl_user_category 中存在 system_id 指向该系统分类的行
     */
    @Override
    public List<SystemCategoryVO> listForUser(String userId) {
        List<SystemCategory> sysList = this.list(new LambdaQueryWrapper<SystemCategory>()
                .eq(SystemCategory::getStatus, SYS_CATEGORY_ENABLED)
                .orderByAsc(SystemCategory::getSort));
        Set<String> linked = sysList.isEmpty() ? Set.of()
                : userCategoryMapper.selectList(new LambdaQueryWrapper<UserCategory>()
                        .eq(UserCategory::getUserId, userId)
                        .isNotNull(UserCategory::getSystemId)).stream()
                .map(UserCategory::getSystemId)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
        return sysList.stream().map(s -> new SystemCategoryVO()
                .setId(s.getId())
                .setName(s.getName())
                .setIcon(s.getIcon())
                .setSort(s.getSort())
                .setRemark(s.getRemark())
                .setAdded(linked.contains(s.getId()))).collect(Collectors.toList());
    }
}
