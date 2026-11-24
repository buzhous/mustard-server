package org.jeecg.modules.app.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.app.bean.vo.category.UserCategoryVO;
import org.jeecg.modules.app.entity.setting.UserCategory;


public interface IUserCategoryService extends IService<UserCategory> {

    IPage<UserCategoryVO> queryCategories(String userId);

    boolean addUserCategory(UserCategoryVO addVO);

    /**
     * 添加系统分类到用户分类（C3 治理，"以系统分类为准"）：
     * ① 用户已关联该系统分类 → 幂等返回既有行；② 同名用户分类已存在 → 回填 system_id/isSys 关联；
     * ③ 否则按系统分类创建新行（name/icon 取系统分类）。
     *
     * @param systemCategoryId 系统分类 id（须为启用状态）
     * @return 落到的用户分类 VO（含 id/name/icon/systemId）；系统分类不存在/停用返回 null
     */
    UserCategoryVO addBySystemCategory(String userId, String systemCategoryId);

    boolean updateUserCategory(UserCategoryVO update);

    boolean deleteUserCategory(String id, String userId);
}
