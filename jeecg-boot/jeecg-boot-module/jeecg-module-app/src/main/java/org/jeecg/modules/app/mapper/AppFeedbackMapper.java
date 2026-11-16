package org.jeecg.modules.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.jeecg.modules.app.entity.feedback.Feedback;

/**
 * 用户反馈 Mapper（App 提交侧）
 */
@Mapper
public interface AppFeedbackMapper extends BaseMapper<Feedback> {
}
