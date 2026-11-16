package org.jeecg.modules.app.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.app.entity.feedback.Feedback;
import org.jeecg.modules.app.mapper.AppFeedbackMapper;
import org.jeecg.modules.app.service.IAppFeedbackService;
import org.springframework.stereotype.Service;

/**
 * 用户反馈 服务实现（App 提交侧）
 */
@Service
public class AppFeedbackServiceImpl extends ServiceImpl<AppFeedbackMapper, Feedback>
        implements IAppFeedbackService {
}
