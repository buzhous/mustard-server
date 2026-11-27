package org.jeecg.modules.app.controller.open;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.config.shiro.IgnoreAuth;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 开放平台文档 API（P6：llms.txt / OpenAPI 规范 / 结构化索引 / 章节检索，设计方案 5.5）。
 * <p>
 * 公开访问（@IgnoreAuth + 非 /app/ 前缀不经 AppAuthFilter），发布正本在 classpath
 * `open-docs/`（与 mustard-frontend 内容目录同步维护）。AI 可检索：llms.txt 给地图、
 * index.json 给章节、openapi.json 给契约、search 给片段。
 */
@Slf4j
@RestController
@RequestMapping("/open/docs")
@Tag(name = "Open-Docs")
public class OpenDocsController {

    /** 发布 Markdown 白名单（resources/open-docs/） */
    private static final List<String> DOC_SLUGS = List.of(
            "quickstart", "authentication", "items", "rest-openapi", "mcp", "errors");

    /** 资源缓存（启动后首次读取驻留） */
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    @IgnoreAuth
    @Operation(summary = "LLM 入口索引（llmstxt.org 约定）")
    @GetMapping(value = "/llms.txt", produces = "text/plain;charset=UTF-8")
    public String llms() {
        return resource("open-docs/llms.txt");
    }

    @IgnoreAuth
    @Operation(summary = "结构化文档索引")
    @GetMapping(value = "/index.json", produces = "application/json;charset=UTF-8")
    public String index() {
        return resource("open-docs/index.json");
    }

    @IgnoreAuth
    @Operation(summary = "OpenAPI 3.1 规范")
    @GetMapping(value = "/openapi.json", produces = "application/json;charset=UTF-8")
    public String openapi() {
        return resource("open-docs/openapi.json");
    }

    @IgnoreAuth
    @Operation(summary = "单篇 Markdown 原文（白名单）")
    @GetMapping("/{name}.md")
    public ResponseEntity<String> doc(@PathVariable String name) {
        if (!DOC_SLUGS.contains(name)) {
            return notFound("文档不存在：" + name);
        }
        String md = resource("open-docs/" + name + ".md");
        if (md == null) {
            return notFound("文档不存在：" + name);
        }
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/markdown;charset=UTF-8")).body(md);
    }

    /**
     * 文档检索（设计方案 8.3）：按 `## ` 章节切分，doc 标题/章节名/正文命中加权
     * （标题 3 / 章节 2 / 正文逐次 1），返回章节级片段与相关度。
     */
    @IgnoreAuth
    @Operation(summary = "文档关键词检索（章节级片段）")
    @GetMapping("/search")
    public Result<List<Map<String, Object>>> search(@RequestParam String q,
                                                    @RequestParam(required = false) Integer limit) {
        if (StrUtil.isBlank(q)) {
            return Result.error(400, "参数 q 必填");
        }
        int cap = limit == null || limit <= 0 ? 5 : Math.min(limit, 20);
        String query = q.trim();
        List<Map<String, Object>> hits = new ArrayList<>();
        for (Map<String, String> section : sections()) {
            int score = 0;
            String docTitle = section.get("title");
            String name = section.get("section");
            String text = section.get("text");
            if (docTitle != null && docTitle.contains(query)) {
                score += 3;
            }
            if (name != null && name.contains(query)) {
                score += 2;
            }
            int idx = 0;
            int occur = 0;
            while (occur < 5 && (idx = text.indexOf(query, idx)) >= 0) {
                score += 1;
                idx += query.length();
                occur++;
            }
            if (score <= 0) {
                continue;
            }
            Map<String, Object> hit = new LinkedHashMap<>();
            hit.put("doc", section.get("doc"));
            hit.put("title", docTitle);
            hit.put("section", name);
            hit.put("snippet", snippet(text, query));
            hit.put("score", score);
            hits.add(hit);
        }
        hits.sort((a, b) -> Integer.compare((int) b.get("score"), (int) a.get("score")));
        return Result.OK(hits.size() > cap ? new ArrayList<>(hits.subList(0, cap)) : hits);
    }

    // ================================ 私有工具 ================================

    /** classpath 资源读取（缓存驻留；缺失返回 null） */
    private String resource(String path) {
        return cache.computeIfAbsent(path, p -> {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(p)) {
                if (in == null) {
                    log.warn("[open-docs] 资源缺失: {}", p);
                    return null;
                }
                return IoUtil.read(in, StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.error("[open-docs] 资源读取失败: {}", p, e);
                return null;
            }
        });
    }

    private ResponseEntity<String> notFound(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(JSON.toJSONString(Result.error(404, message)));
    }

    /** 全部文档章节缓存：{doc, title, section, text}（frontmatter 跳过，## 切分） */
    private List<Map<String, String>> sections() {
        List<Map<String, String>> out = new ArrayList<>();
        for (String slug : DOC_SLUGS) {
            String md = resource("open-docs/" + slug + ".md");
            if (md == null) {
                continue;
            }
            String title = slug;
            String body = md;
            if (md.startsWith("---")) {
                int end = md.indexOf("---", 3);
                if (end > 0) {
                    String head = md.substring(3, end);
                    for (String line : head.split("\n")) {
                        if (line.startsWith("title:")) {
                            title = line.substring(6).trim();
                        }
                    }
                    body = md.substring(end + 3);
                }
            }
            String current = "（开篇）";
            StringBuilder text = new StringBuilder();
            for (String line : body.split("\n")) {
                if (line.startsWith("## ")) {
                    collect(out, slug, title, current, text);
                    current = line.substring(3).trim();
                    text = new StringBuilder();
                } else {
                    text.append(line).append('\n');
                }
            }
            collect(out, slug, title, current, text);
        }
        return out;
    }

    private void collect(List<Map<String, String>> out, String doc, String title,
                         String section, StringBuilder text) {
        String t = text.toString().trim();
        if (t.isEmpty()) {
            return;
        }
        Map<String, String> m = new LinkedHashMap<>();
        m.put("doc", doc);
        m.put("title", title);
        m.put("section", section);
        m.put("text", t);
        out.add(m);
    }

    /** 片段：首个命中位置向前 40 向后 120 截取（无命中取开头） */
    private String snippet(String text, String query) {
        String plain = text.replaceAll("\n+", " ").trim();
        int idx = plain.indexOf(query);
        if (idx < 0) {
            return plain.length() > 160 ? plain.substring(0, 160) + "…" : plain;
        }
        int from = Math.max(0, idx - 40);
        int to = Math.min(plain.length(), idx + query.length() + 120);
        return (from > 0 ? "…" : "") + plain.substring(from, to) + (to < plain.length() ? "…" : "");
    }
}
