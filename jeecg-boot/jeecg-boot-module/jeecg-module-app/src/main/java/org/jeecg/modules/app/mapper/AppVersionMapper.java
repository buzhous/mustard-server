package org.jeecg.modules.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.jeecg.modules.app.entity.AppVersion;

/**
 * App 版本管理 Mapper：latest 判定与状态流转均在 Service 用 LambdaQuery 完成，无自定义 SQL（双库天然兼容）。
 */
@Mapper
public interface AppVersionMapper extends BaseMapper<AppVersion> {
}
