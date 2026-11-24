package org.jeecg.modules.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.jeecg.modules.app.entity.SystemCategory;

/**
 * 系统物品分类 Mapper（C3 分类治理）
 */
@Mapper
public interface SystemCategoryMapper extends BaseMapper<SystemCategory> {
}
