package org.jeecg.modules.app.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.app.bean.vo.category.SystemCategoryVO;
import org.jeecg.modules.app.entity.SystemCategory;

import java.util.List;

/**
 * 系统物品分类服务（C3 分类治理）：App 端列表/关联 + 管理端治理（运营增停改）
 */
public interface ISystemCategoryService extends IService<SystemCategory> {

    /**
     * App 端系统分类列表（启用态，sort 升序），带当前用户「已关联」标记（added）
     */
    List<SystemCategoryVO> listForUser(String userId);
}
