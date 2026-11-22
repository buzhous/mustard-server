package org.jeecg.modules.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.jeecg.modules.app.entity.OpenAuditLog;

/**
 * 开放平台调用审计 Mapper
 */
@Mapper
public interface OpenAuditLogMapper extends BaseMapper<OpenAuditLog> {
}
