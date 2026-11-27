package org.jeecg.modules.app.controller.open;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.config.shiro.IgnoreAuth;
import org.jeecg.modules.ai.service.AiAgentService;
import org.jeecg.modules.ai.tooling.ItemTools;
import org.jeecg.modules.app.entity.OpenAppCredential;
import org.jeecg.modules.app.openapi.OpenAuditLogger;
import org.jeecg.modules.app.openapi.OpenRateLimiter;
import org.jeecg.modules.app.openapi.OpenScopeChecker;
import org.jeecg.modules.app.openapi.OpenSignatureFilter;
import org.jeecg.modules.app.service.AppOpenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 芥子开放平台 MCP Server（设计方案 5 章 / B2）
 * <p>
 * 复用 demo 自研 JSON-RPC 模式（协议 2024-11-05，SSE + Streamable HTTP 双端点，零新增依赖）；
 * 鉴权走 OpenSignatureFilter（appkey 请求头），scope/写限流/审计在 dispatch 层按 tool 名判定
 * （路径层无法区分 tools/call 读写意图）。工具输出即 MIP 出域实例，红线在 AppOpenService 内核。
 */
@Slf4j
@RestController
@RequestMapping("/app/open/mcp")
@Tag(name = "Open-MCP Server")
public class AppOpenMcpController {

    private final Map<String, SseEmitter> sseEmitters = new ConcurrentHashMap<>();

    /** MCP 写工具（dispatch 层追加写限流窗口） */
    private static final List<String> WRITE_TOOLS = List.of(
            "add_item", "update_item", "delete_item", "gift_item", "flow_offer", "flow_respond");

    private final List<Map<String, Object>> TOOLS = List.of(
            tool("list_items", "分页查询用户的物品列表（轻量摘要，含 schemaId/contentHash）", Map.of(
                    "pageNo", Map.of("type", "integer", "description", "页码，默认 1"),
                    "pageSize", Map.of("type", "integer", "description", "页大小，默认 10，上限 100"),
                    "keyword", Map.of("type", "string", "description", "关键字（名称/描述模糊匹配）"),
                    "stationId", Map.of("type", "string", "description", "仓位ID"),
                    "stationType", Map.of("type", "integer", "description", "仓位类型：1虚拟仓 2真实站点仓"),
                    "status", Map.of("type", "integer", "description", "物品状态：0临时 1正常 2使用中"),
                    "orderBy", Map.of("type", "string", "description", "排序字段：created/name/price/quantity/status"),
                    "ascending", Map.of("type", "boolean", "description", "是否升序，默认降序")),
                    List.of()),
            tool("get_item", "返回物品 MIP 档案。sensitivity=restricted 的字段已由服务端过滤，请勿尝试获取证件号、精确定位等受限信息。contentHash 可用于向用户证明档案完整性。", Map.of(
                    "itemId", Map.of("type", "string", "description", "物品ID")),
                    List.of("itemId")),
            tool("get_item_lineage", "返回物品流转账本事件（含逐事件哈希与 chainValid 完整性校验）", Map.of(
                    "itemId", Map.of("type", "string", "description", "物品ID"),
                    "limit", Map.of("type", "integer", "description", "事件截断条数（可选）")),
                    List.of("itemId")),
            tool("search_items", "按关键字搜索物品（轻量摘要，keyword 必填）", Map.of(
                    "keyword", Map.of("type", "string", "description", "搜索关键字"),
                    "limit", Map.of("type", "integer", "description", "返回上限，默认 20，最大 50"),
                    "fields", Map.of("type", "string", "description", "逗号分隔的 attributes element 列表，如 name,price 对应 element 编号")),
                    List.of("keyword")),
            tool("get_stats", "物品聚合统计（总数/按状态/按分类/总价值，仅聚合值无明细）", Map.of(), List.of()),
            tool("parse_item_draft", "自然语言建档解析服务（不落库）。将用户对物品的描述解析为结构化草稿：draft 草稿卡 + attributes 可提交数组 + confidence 置信度 + manualFill 敏感字段手动补录提示。restricted 字段（证件号/密码/精确位置等）绝不参与解析。创建动作：attributes 与 confidence 经二次确认后调用 add_item（>=85 直接入库，<85 转草稿态由用户在 App 确认）。", Map.of(
                    "text", Map.of("type", "string", "description", "物品的自然语言描述（必填）"),
                    "image_url", Map.of("type", "string", "description", "可选：物品图片 URL（http/https），辅助识别")),
                    List.of("text")),
            tool("add_item", "为用户新增物品。attributes 必填组件：1 名称 / 2 图标(合法URL) / 3 类别；sensitivity 由服务端单点计算，请求方传入无效。confidence>=85 直接入库，否则草稿确认态（App 端会弹确认）。", Map.of(
                    "attributes", Map.of(
                            "type", "array",
                            "description", "物品属性数组 [{element,value}]",
                            "items", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                            "element", Map.of("type", "integer", "description", "组件编号，如 1=名称 2=图标 3=类别 7=数量"),
                                            "value", Map.of("description", "组件值，类型随组件而定")),
                                    "required", List.of("element", "value"))),
                    "confidence", Map.of("type", "number", "description", "解析置信度 0-100，≥85 直接入库，否则草稿"),
                    "source", Map.of("type", "string", "description", "来源标识，如 ai-assistant"),
                    "idempotencyKey", Map.of("type", "string", "description", "幂等键，重复调用返回首次结果")),
                    List.of("attributes", "confidence")),
            tool("update_item", "编辑已存在的物品（全量覆盖 attributes）。归属校验后走与新增相同的 MIP 写入链（enrich/identity 同步/422 校验），账本追加 update 事件。", Map.of(
                    "itemId", Map.of("type", "string", "description", "要编辑的物品ID（必须是该账号名下的物品）"),
                    "attributes", Map.of(
                            "type", "array",
                            "description", "物品属性数组 [{element,value}]，全量覆盖",
                            "items", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                            "element", Map.of("type", "integer", "description", "组件编号，如 1=名称 2=图标 3=类别 7=数量"),
                                            "value", Map.of("description", "组件值，类型随组件而定")),
                                    "required", List.of("element", "value"))),
                    "confidence", Map.of("type", "number", "description", "解析置信度 0-100，≥85 恢复正常态，<85 转草稿；不传保持原状态"),
                    "idempotencyKey", Map.of("type", "string", "description", "幂等键，重复调用返回首次结果")),
                    List.of("itemId", "attributes")),
            tool("delete_item", "删除物品（高危，不可恢复）。删除物品与库存记录，账本追加 recycle 事件（历史保留可追溯）。建议先向用户确认。", Map.of(
                    "itemId", Map.of("type", "string", "description", "要删除的物品ID"),
                    "idempotencyKey", Map.of("type", "string", "description", "幂等键，重复调用返回首次结果")),
                    List.of("itemId")),
            tool("gift_item", "赠送物品（不可撤销）。必须先 step=preview 获取 giftToken，并将预览结果完整展示给用户确认后，才可 step=execute。禁止跳过 preview 直接执行。", Map.of(
                    "step", Map.of("type", "string", "description", "preview / execute", "enum", List.of("preview", "execute")),
                    "itemId", Map.of("type", "string", "description", "step=preview 时必填：物品ID"),
                    "giftToken", Map.of("type", "string", "description", "step=execute 时必填：preview 返回的 giftToken"),
                    "toUserId", Map.of("type", "string", "description", "step=execute 时必填：接收方用户ID")),
                    List.of("step")),
            tool("flow_offer", "发起定向流转要约（两阶段，无资金结算）。发起后该物品锁定 72 小时（锁定期不可赠送/编辑/删除/重复发起），接收方必须是平台 App 用户（按用户ID定位）。72h 未响应自动失效并解锁。", Map.of(
                    "itemId", Map.of("type", "string", "description", "要流转的物品ID（当前账号名下）"),
                    "toUserId", Map.of("type", "string", "description", "接收方 App 用户ID（账号ID，不能是自己）"),
                    "idempotencyKey", Map.of("type", "string", "description", "幂等键，重复调用返回首次结果")),
                    List.of("itemId", "toUserId")),
            tool("flow_respond", "处理流转要约：action=accept 确认 / reject 拒绝（两者仅当要约接收方是当前凭据绑定用户本人时可用，不支持代第三方接收）/ cancel 撤回自己发出的要约。accept 后物品归属立即转移并落账本 flow_out/flow_in 事件。", Map.of(
                    "action", Map.of("type", "string", "description", "accept 确认 / reject 拒绝 / cancel 撤回", "enum", List.of("accept", "reject", "cancel")),
                    "flowToken", Map.of("type", "string", "description", "要约令牌（flow_offer 返回或 flow_list 查得）"),
                    "idempotencyKey", Map.of("type", "string", "description", "幂等键，重复调用返回首次结果")),
                    List.of("action", "flowToken")),
            tool("flow_list", "查询待处理的流转要约（dir=sent 发出 / received 收到 / all 全部，默认 all；仅 72h 窗口内待响应要约，已完成的流转以 get_item_lineage 的 flow_out/flow_in 事件为准）", Map.of(
                    "dir", Map.of("type", "string", "description", "方向过滤：sent / received / all（默认 all）", "enum", List.of("sent", "received", "all"))),
                    List.of()));

    @Autowired
    private AppOpenService appOpenService;

    @Autowired
    private AiAgentService agentService;

    @Autowired
    private ItemTools itemTools;

    @Autowired
    private org.jeecg.modules.app.service.ItemFlowService itemFlowService;

    @Autowired
    private OpenRateLimiter rateLimiter;

    @Autowired
    private OpenAuditLogger auditLogger;

    @IgnoreAuth
    @Operation(summary = "MCP SSE 连接端点")
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sse(HttpServletRequest request) {
        // 鉴权已在 OpenSignatureFilter 完成（appkey 请求头）；未通过不会到达此处
        String clientId = UUID.randomUUID().toString();
        OpenAppCredential credential = credential(request);
        log.info("[open-mcp] 新客户端 SSE 连接: {} appKey={}", clientId,
                credential == null ? "-" : credential.getAppKey());
        SseEmitter emitter = new SseEmitter(0L);
        sseEmitters.put(clientId, emitter);
        emitter.onCompletion(() -> sseEmitters.remove(clientId));
        emitter.onTimeout(() -> sseEmitters.remove(clientId));
        emitter.onError(e -> sseEmitters.remove(clientId));
        try {
            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            String messageEndpoint = baseUrl + request.getContextPath() + "/app/open/mcp/message?sessionId=" + clientId;
            emitter.send(SseEmitter.event().name("endpoint").data(messageEndpoint));
        } catch (IOException e) {
            log.error("[open-mcp] 发送 endpoint 事件失败", e);
        }
        return emitter;
    }

    @IgnoreAuth
    @Operation(summary = "MCP Streamable HTTP 端点")
    @PostMapping(value = "/sse")
    public void ssePost(HttpServletRequest request, @RequestBody String body, HttpServletResponse response) throws IOException {
        handleJsonRpcRequest(request, body, response);
    }

    @IgnoreAuth
    @Operation(summary = "MCP 消息处理端点（SSE 通路）")
    @PostMapping(value = "/message")
    public void handleMessage(HttpServletRequest request,
                              @RequestParam(required = false) String sessionId,
                              @RequestBody String body,
                              HttpServletResponse response) throws IOException {
        log.info("[open-mcp] 收到消息 sessionId={} body={}", sessionId, body);
        handleJsonRpcRequest(request, body, response);
    }

    // ================================ JSON-RPC 处理 ================================

    private void handleJsonRpcRequest(HttpServletRequest request, String body, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter writer = response.getWriter();
        try {
            JSONObject rpc = JSON.parseObject(body);
            String method = rpc.getString("method");
            Object id = rpc.get("id");
            JSONObject params = rpc.getJSONObject("params");
            if (id == null) {
                // 通知类消息不需要响应
                writer.write("{}");
                writer.flush();
                return;
            }
            Map<String, Object> jsonRpcResponse = new LinkedHashMap<>();
            jsonRpcResponse.put("jsonrpc", "2.0");
            jsonRpcResponse.put("id", id);
            try {
                jsonRpcResponse.put("result", switch (method) {
                    case "initialize" -> handleInitialize();
                    case "initialized", "notifications/initialized" -> Map.of();
                    case "ping" -> Map.of();
                    case "notifications/cancelled" -> Map.of();
                    case "tools/list" -> Map.of("tools", TOOLS);
                    case "tools/call" -> handleToolsCall(request, params);
                    default -> {
                        if (method != null && method.startsWith("notifications/")) {
                            yield Map.of();
                        }
                        yield errorResult(-32601, "method not found: " + method);
                    }
                });
            } catch (Exception e) {
                log.error("[open-mcp] 处理请求失败 method={}", method, e);
                jsonRpcResponse.put("result", errorResult(-32603, e.getMessage() == null ? "internal error" : e.getMessage()));
            }
            writer.write(JSON.toJSONString(jsonRpcResponse));
        } catch (Exception e) {
            log.error("[open-mcp] 解析请求失败", e);
            writer.write("{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32700,\"message\":\"Parse error\"}}");
        }
        writer.flush();
    }

    private Map<String, Object> handleInitialize() {
        return Map.of(
                "protocolVersion", "2024-11-05",
                "capabilities", Map.of("tools", Map.of()),
                "serverInfo", Map.of("name", "mustard-mip-mcp", "version", "1.0.0"));
    }

    /**
     * tools/call dispatch：scope（tool 名判定）→ 写工具限流 → AppOpenService 内核 → MCP content
     * 业务失败按 MCP 规范输出 tool result isError:true（Agent 可读，设计方案 5.4）
     */
    private Map<String, Object> handleToolsCall(HttpServletRequest request, JSONObject params) {
        OpenAppCredential credential = credential(request);
        String toolName = params == null ? null : params.getString("name");
        JSONObject arguments = params == null || params.getJSONObject("arguments") == null
                ? new JSONObject() : params.getJSONObject("arguments");
        if (toolName == null) {
            return errorResult(-32602, "invalid params: name is required");
        }
        boolean write = WRITE_TOOLS.contains(toolName);
        long start = System.currentTimeMillis();
        // scope 校验（dispatch 层按 tool 名判定）
        String scopeError = OpenScopeChecker.checkTool(toolName, credential.getScope());
        if (scopeError != null) {
            return auditAndToolResult(request, credential, toolName, write, arguments, 403, 0,
                    toolError(toolName, scopeError));
        }
        // 写工具追加写限流窗口（读窗口已在 Filter 层计入）
        if (write) {
            String limitError = rateLimiter.tryAcquire(credential.getAppKey(), true, credential.getDailyQuota());
            if (limitError != null) {
                return auditAndToolResult(request, credential, toolName, true, arguments, 429,
                        (int) (System.currentTimeMillis() - start),
                        toolError(toolName, limitError));
            }
        }
        try {
            String json = switch (toolName) {
                case "list_items" -> JSON.toJSONString(appOpenService.pageItems(credential.getBoundUid(),
                        new org.jeecg.modules.app.bean.vo.item.ItemInfoQueryVO()
                                .setPageNo(arguments.getInteger("pageNo") == null ? 1 : arguments.getInteger("pageNo"))
                                .setPageSize(arguments.getInteger("pageSize") == null ? 10 : arguments.getInteger("pageSize"))
                                .setKeyword(arguments.getString("keyword"))
                                .setStationId(arguments.getString("stationId"))
                                .setStationType(arguments.getInteger("stationType"))
                                .setStatus(arguments.getInteger("status"))
                                .setOrderBy(arguments.getString("orderBy"))
                                .setAscending(arguments.getBoolean("ascending"))));
                case "get_item" -> require(arguments, "itemId", itemId -> {
                    JSONObject instance = appOpenService.itemDetail(credential.getBoundUid(), itemId);
                    return instance == null ? null : JSON.toJSONString(instance);
                });
                case "get_item_lineage" -> require(arguments, "itemId", itemId -> {
                    JSONObject lineage = appOpenService.lineage(credential.getBoundUid(), itemId);
                    if (lineage == null) {
                        return null;
                    }
                    Integer limit = arguments.getInteger("limit");
                    if (limit != null && limit > 0 && lineage.getJSONArray("events").size() > limit) {
                        lineage.put("events", lineage.getJSONArray("events").subList(0, limit));
                    }
                    return JSON.toJSONString(lineage);
                });
                case "search_items" -> {
                    String keyword = arguments.getString("keyword");
                    if (StrUtil.isEmpty(keyword)) {
                        yield null;
                    }
                    JSONObject out = new JSONObject(true);
                    out.put("keyword", keyword);
                    out.put("items", appOpenService.searchItems(credential.getBoundUid(), keyword,
                            arguments.getInteger("limit"), arguments.getString("fields")));
                    yield JSON.toJSONString(out);
                }
                case "get_stats" -> JSON.toJSONString(appOpenService.stats(credential.getBoundUid()));
                case "parse_item_draft" -> {
                    String text = arguments.getString("text");
                    if (StrUtil.isEmpty(text)) {
                        yield null;
                    }
                    String imageUrl = arguments.getString("image_url");
                    if (StrUtil.isNotEmpty(imageUrl) && !imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
                        yield null;
                    }
                    yield JSON.toJSONString(parseDraftResult(credential.getBoundUid(), text, imageUrl));
                }
                case "add_item" -> {
                    Object idem = arguments.get("idempotencyKey");
                    String idempotencyKey = idem == null ? UUID.randomUUID().toString() : String.valueOf(idem);
                    Result<JSONObject> result = appOpenService.addItem(credential, arguments, idempotencyKey);
                    yield toolBusinessJson(result);
                }
                case "update_item" -> {
                    Object idem = arguments.get("idempotencyKey");
                    String idempotencyKey = idem == null ? UUID.randomUUID().toString() : String.valueOf(idem);
                    Result<JSONObject> result = appOpenService.updateItem(credential, arguments, idempotencyKey);
                    yield toolBusinessJson(result);
                }
                case "delete_item" -> {
                    Object idem = arguments.get("idempotencyKey");
                    String idempotencyKey = idem == null ? UUID.randomUUID().toString() : String.valueOf(idem);
                    Result<JSONObject> result = appOpenService.deleteItem(credential, arguments, idempotencyKey);
                    yield toolBusinessJson(result);
                }
                case "gift_item" -> {
                    String step = arguments.getString("step");
                    if ("preview".equals(step)) {
                        String itemId = arguments.getString("itemId");
                        if (StrUtil.isEmpty(itemId)) {
                            yield null;
                        }
                        yield toolBusinessJson(appOpenService.giftPreview(credential, itemId));
                    } else if ("execute".equals(step)) {
                        yield toolBusinessJson(appOpenService.giftSend(credential,
                                arguments.getString("giftToken"), arguments.getString("toUserId")));
                    } else {
                        yield null;
                    }
                }
                case "flow_offer" -> {
                    Object idem = arguments.get("idempotencyKey");
                    String idempotencyKey = idem == null ? UUID.randomUUID().toString() : String.valueOf(idem);
                    yield toolBusinessJson(itemFlowService.offer(credential, arguments, idempotencyKey));
                }
                case "flow_respond" -> {
                    String action = arguments.getString("action");
                    String flowToken = arguments.getString("flowToken");
                    if (StrUtil.isEmpty(action) || StrUtil.isEmpty(flowToken)) {
                        yield null;
                    }
                    Object idem = arguments.get("idempotencyKey");
                    String idempotencyKey = idem == null ? UUID.randomUUID().toString() : String.valueOf(idem);
                    Result<JSONObject> result = switch (action) {
                        case "accept" -> itemFlowService.accept(credential, flowToken, idempotencyKey);
                        case "reject" -> itemFlowService.reject(credential, flowToken, idempotencyKey);
                        case "cancel" -> itemFlowService.cancel(credential, flowToken, idempotencyKey);
                        default -> null;
                    };
                    yield result == null ? null : toolBusinessJson(result);
                }
                case "flow_list" -> JSON.toJSONString(
                        itemFlowService.list(credential.getBoundUid(), arguments.getString("dir")));
                default -> null;
            };
            if (json == null) {
                return auditAndToolResult(request, credential, toolName, write, arguments, 400,
                        (int) (System.currentTimeMillis() - start), errorResult(-32602, "invalid params"));
            }
            return auditAndToolResult(request, credential, toolName, write, arguments, 200,
                    (int) (System.currentTimeMillis() - start),
                    Map.of("content", List.of(Map.of("type", "text", "text", json)), "isError", false));
        } catch (Exception e) {
            log.error("[open-mcp] 工具执行失败 tool={}", toolName, e);
            return auditAndToolResult(request, credential, toolName, write, arguments, 500,
                    (int) (System.currentTimeMillis() - start),
                    toolError(toolName, e.getMessage() == null ? "internal error" : e.getMessage()));
        }
    }

    // ================================ 私有工具 ================================

    /**
     * parse_item_draft 结果组装（P6 建档解析服务）：draft 有可用 attributes 时 parsed=true，
     * 输出 {draft, attributes, confidence, manualFill, next_step}；否则 parsed=false + reply。
     * attributes 经 ItemTools.buildAttributes（restricted/未知 element 兜底丢弃）——
     * 外部 agent 拿到的可提交数组不含敏感元素（红线随工具封装继承）。
     */
    private JSONObject parseDraftResult(String uid, String text, String imageUrl) {
        JSONObject decision = agentService.parseDraft(uid, text,
                StrUtil.isEmpty(imageUrl) ? null : List.of(imageUrl));
        JSONObject out = new JSONObject(true);
        JSONObject draft = decision.getJSONObject("draft");
        JSONArray attributes = draft == null ? null : JSON.parseArray(itemTools.buildAttributes(draft));
        if (attributes == null || attributes.isEmpty()) {
            out.put("parsed", false);
            out.put("reply", StrUtil.blankToDefault(decision.getString("reply"),
                    "未能从描述中解析出可建档的物品信息，请补充物品名称、类别与特征"));
            return out;
        }
        out.put("parsed", true);
        out.put("draft", draft);
        out.put("attributes", attributes);
        out.put("confidence", decision.getIntValue("confidence"));
        JSONArray manual = draft.getJSONArray("manualFill");
        if (manual != null && !manual.isEmpty()) {
            // 克隆断开与 draft.manualFill 的共享引用，避免 fastjson2 输出 $ref 路径（外部 Agent 不可读）
            out.put("manualFill", JSON.parseArray(manual.toJSONString()));
        }
        out.put("next_step", "将 attributes 与 confidence 二次确认后调用 add_item 提交（confidence>=85 直接入库，<85 转草稿态由用户在 App 确认）");
        return out;
    }

    private Map<String, Object> auditAndToolResult(HttpServletRequest request, OpenAppCredential credential,
                                                   String toolName, boolean write, JSONObject arguments,
                                                   int resultCode, int costMs, Map<String, Object> toolResult) {
        auditLogger.log(credential.getAppKey(), credential.getBoundUid(), "mcp", toolName,
                write ? "write" : "read", digestOf(arguments), resultCode, null,
                costMs, clientIp(request));
        return toolResult;
    }

    private Map<String, Object> toolError(String toolName, String message) {
        return Map.of(
                "content", List.of(Map.of("type", "text",
                        "text", "{\"tool\":\"" + toolName + "\",\"error\":\"" + (message == null ? "" : message.replace("\"", "'")) + "\"}")),
                "isError", true);
    }

    /** Result → tool 文本（业务失败输出 isError:true + 业务 message，设计方案 5.4） */
    private String toolBusinessJson(Result<?> result) {
        if (result == null) {
            return null;
        }
        return JSON.toJSONString(result);
    }

    private String digestOf(JSONObject arguments) {
        if (arguments == null) {
            return null;
        }
        String s = JSON.toJSONString(arguments);
        return s.length() > 500 ? s.substring(0, 500) : s;
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(ip) && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return StrUtil.isBlank(ip) ? request.getRemoteAddr() : ip;
    }

    private OpenAppCredential credential(HttpServletRequest request) {
        return (OpenAppCredential) request.getAttribute(OpenSignatureFilter.ATTR_CREDENTIAL);
    }

    private interface ArgumentGetter<T, R> {
        R get(T value);
    }

    private <R> R require(JSONObject arguments, String key, ArgumentGetter<String, R> fn) {
        String value = arguments.getString(key);
        if (StrUtil.isEmpty(value)) {
            return null;
        }
        return fn.get(value);
    }

    private Map<String, Object> tool(String name, String description, Map<String, Object> properties, List<String> required) {
        Map<String, Object> inputSchema = new LinkedHashMap<>();
        inputSchema.put("type", "object");
        inputSchema.put("properties", properties);
        if (!required.isEmpty()) {
            inputSchema.put("required", required);
        }
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("name", name);
        t.put("description", description);
        t.put("inputSchema", inputSchema);
        return t;
    }

    private Map<String, Object> errorResult(int code, String message) {
        return Map.of(
                "content", List.of(Map.of("type", "text", "text", message == null ? "" : message)),
                "isError", true,
                "errorCode", code);
    }
}
