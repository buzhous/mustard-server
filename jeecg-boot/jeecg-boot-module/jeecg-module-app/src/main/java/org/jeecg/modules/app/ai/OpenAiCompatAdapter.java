package org.jeecg.modules.app.ai;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.util.List;

/**
 * OPENAI 兼容协议适配器（DeepSeek 走此实现；GPT/QWEN/智谱兼容端点仅换配置即用）。
 * 配置源：application-*.yml jeecg.ai.*（base-url / api-key / chat-model / temperature / max-tokens）。
 */
@Slf4j
@Component
public class OpenAiCompatAdapter implements AiModelAdapter {

    @Value("${jeecg.ai-chat.item-filing.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${jeecg.ai-chat.item-filing.api-key:}")
    private String apiKey;

    @Value("${jeecg.ai-chat.item-filing.chat-model:deepseek-chat}")
    private String chatModel;

    @Value("${jeecg.ai-chat.item-filing.temperature:0}")
    private Double temperature;

    @Value("${jeecg.ai-chat.item-filing.max-tokens:8192}")
    private Integer maxTokens;

    @Override
    public String structured(String systemPrompt, String userContent) {
        JSONObject body = baseBody(systemPrompt, userContent);
        // JSON 模式（DeepSeek/主流兼容端点支持），失败重试由编排器负责
        body.put("response_format", new JSONObject(true).fluentPut("type", "json_object"));
        return post(body);
    }

    @Override
    public String chat(List<Turn> messages) {
        JSONArray msgs = new JSONArray();
        for (Turn t : messages) {
            msgs.add(new JSONObject(true).fluentPut("role", t.role()).fluentPut("content", t.content()));
        }
        JSONObject body = baseBody(null, null);
        body.put("messages", msgs);
        return post(body);
    }

    @Override
    public String vision(List<String> imageUrls, String text) {
        // vision provider 注册位：QWEN-VL Key 就绪后在此实现（多模态 message content 数组协议）。
        // 当前未提供 Key → 返回 null，编排器降级口述模式（tool_schema.json vision_describe.fallback）。
        return null;
    }

    // ================================ 私有工具 ================================

    private JSONObject baseBody(String systemPrompt, String userContent) {
        JSONArray msgs = new JSONArray();
        if (systemPrompt != null) {
            msgs.add(new JSONObject(true).fluentPut("role", "system").fluentPut("content", systemPrompt));
        }
        if (userContent != null) {
            msgs.add(new JSONObject(true).fluentPut("role", "user").fluentPut("content", userContent));
        }
        JSONObject body = new JSONObject(true);
        body.put("model", chatModel);
        body.put("messages", msgs);
        body.put("temperature", temperature == null ? 0 : temperature);
        body.put("max_tokens", maxTokens == null ? 8192 : maxTokens);
        body.put("stream", false);
        return body;
    }

    private String post(JSONObject body) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("AI 未配置（jeecg.ai.api-key 为空）");
        }
        String url = baseUrl.replaceAll("/+$", "") + "/chat/completions";
        String rsp = cn.hutool.http.HttpRequest.post(url)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(body.toJSONString())
                .timeout(60000)
                .execute()
                .body();
        JSONObject json = JSON.parseObject(rsp);
        if (json == null || json.getJSONObject("error") != null) {
            throw new IllegalStateException("AI 调用失败: "
                    + (json == null ? "空响应" : json.getJSONObject("error") == null ? rsp : json.getJSONObject("error").getString("message")));
        }
        if (json.getIntValue("code") == HttpURLConnection.HTTP_INTERNAL_ERROR) {
            throw new IllegalStateException("AI 服务端错误");
        }
        JSONArray choices = json.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new IllegalStateException("AI 响应缺少 choices");
        }
        return choices.getJSONObject(0).getJSONObject("message").getString("content");
    }
}
