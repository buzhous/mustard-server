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
 * OPENAI 兼容协议适配器（DeepSeek deepseek-flash 走此实现；GPT/QWEN-VL/智谱兼容端点仅换配置即用）。
 * 配置源：application-*.yml jeecg.ai-chat.item-filing.*（base-url / api-key / chat-model / thinking / temperature / max-tokens）。
 */
@Slf4j
@Component
public class OpenAiCompatAdapter implements AiModelAdapter {

    @Value("${jeecg.ai-chat.item-filing.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${jeecg.ai-chat.item-filing.api-key:}")
    private String apiKey;

    @Value("${jeecg.ai-chat.item-filing.chat-model:deepseek-flash}")
    private String chatModel;

    @Value("${jeecg.ai-chat.item-filing.temperature:0}")
    private Double temperature;

    @Value("${jeecg.ai-chat.item-filing.max-tokens:8192}")
    private Integer maxTokens;

    /**
     * 思考模式开关（DeepSeek 官方参数 thinking.type：enabled/disabled）。
     * 官方默认 enabled，但思考模式下 temperature/top_p 等参数不生效；
     * 建档结构化解析需要确定性输出 → 默认 disabled，可用配置开启。
     */
    @Value("${jeecg.ai-chat.item-filing.thinking:disabled}")
    private String thinking;

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
        // DeepSeek deepseek-flash 官方支持图像理解（JPEG/PNG/GIF/WebP），content 为 OpenAI 兼容 block 数组：
        // url 支持 http(s) 外链（≤8192 字符、需公网可达）或 base64 data URL；
        // QWEN-VL / GPT-4o 等兼容端点同为该多模态协议，换模型无需改代码。
        // 返回 null → 编排器口述降级；调用失败抛异常 → 编排器捕获后同样降级。
        if (imageUrls == null || imageUrls.isEmpty()) {
            return null;
        }
        JSONArray content = new JSONArray();
        if (text != null && !text.isBlank()) {
            content.add(new JSONObject(true).fluentPut("type", "text").fluentPut("text", text));
        }
        for (String url : imageUrls) {
            if (url == null || url.isBlank()) {
                continue;
            }
            content.add(new JSONObject(true)
                    .fluentPut("type", "image_url")
                    .fluentPut("image_url", new JSONObject(true).fluentPut("url", url)));
        }
        if (content.isEmpty()) {
            return null;
        }
        JSONArray msgs = new JSONArray();
        msgs.add(new JSONObject(true).fluentPut("role", "user").fluentPut("content", content));
        JSONObject body = baseBody(null, null);
        body.put("messages", msgs);
        return post(body);
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
        // DeepSeek 新增参数：思考模式开关（官方默认 enabled；思考模式下 temperature 不生效，建档需确定性输出 → 默认 disabled）
        if (thinking != null && !thinking.isBlank()) {
            body.put("thinking", new JSONObject(true).fluentPut("type", thinking.trim()));
        }
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
