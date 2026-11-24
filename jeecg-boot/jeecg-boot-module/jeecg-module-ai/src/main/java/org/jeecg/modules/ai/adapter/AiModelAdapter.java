package org.jeecg.modules.ai.adapter;

import java.util.List;

/**
 * AI 模型适配层（ai-integration-plan §5.7 SK2 / §5.9 S3）
 * <p>
 * 统一三方法签名：chat / structured / vision。实现注册即换模型
 * （OPENAI 兼容协议覆盖 DeepSeek/GPT/QWEN；技能层/编排层面向本接口编程，零改动切换）。
 * vision 无 provider 时返回 null，由编排器降级为口述模式（用户话术即物品描述）。
 */
public interface AiModelAdapter {

    /**
     * 单轮结构化决策（temperature=0 + JSON 输出约束）。
     *
     * @param systemPrompt 技能包 system_prompt 模板（已填充上下文变量）
     * @param userContent  用户本轮输入
     * @return 模型原始文本（编排器负责 JSON 解析与失败重试）
     */
    String structured(String systemPrompt, String userContent);

    /**
     * 多轮对话（闲聊/解释类回合，预留）。
     */
    String chat(List<Turn> messages);

    /**
     * 图片客观描述（QWEN-VL 等 vision provider）。
     *
     * @return null 表示当前无可用 vision provider → 编排器口述模式降级
     */
    String vision(List<String> imageUrls, String text);

    /** 对话轮次 */
    record Turn(String role, String content) {
    }
}
