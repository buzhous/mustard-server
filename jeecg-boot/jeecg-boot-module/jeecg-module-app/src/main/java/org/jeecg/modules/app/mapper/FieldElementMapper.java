package org.jeecg.modules.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.jeecg.modules.app.entity.FieldElement;

/**
 * 字段字典 Mapper（三端共用唯一字段数据源）
 */
@Mapper
public interface FieldElementMapper extends BaseMapper<FieldElement> {
}
