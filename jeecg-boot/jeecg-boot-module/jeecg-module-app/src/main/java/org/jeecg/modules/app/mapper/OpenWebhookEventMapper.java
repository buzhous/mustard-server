package org.jeecg.modules.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.jeecg.modules.app.entity.OpenWebhookEvent;

/**
 * 开放平台 Webhook 投递事件 Mapper（B4）
 */
@Mapper
public interface OpenWebhookEventMapper extends BaseMapper<OpenWebhookEvent> {
}
