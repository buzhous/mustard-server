package org.jeecg.modules.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.jeecg.modules.app.entity.AiFeedback;

/**
 * AI 反馈样本 Mapper（tbl_ai_feedback）：低命中样本自动入库与样本池查询共用。
 */
public interface AiFeedbackMapper extends BaseMapper<AiFeedback> {
}
