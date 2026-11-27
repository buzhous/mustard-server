---
title: MCP Server 接入
summary: 通过 MCP 协议（2024-11-05）在 Cursor / Claude 等 AI 客户端中读写芥子空间物品
keywords: MCP, SSE, Streamable HTTP, tools, JSON-RPC, AI, Agent, Cursor, Claude, list_items, add_item, gift_item, parse_item_draft, 建档解析
scope: readonly readwrite
---

## 端点

MCP Server 复用直连鉴权（`appkey` 请求头），SSE 与 Streamable HTTP 双端点：

| 端点 | 说明 |
|---|---|
| `GET /api/app/open/mcp/sse` | SSE 长连接，返回 `endpoint` 事件指向 message 地址 |
| `POST /api/app/open/mcp/sse` | Streamable HTTP（JSON-RPC 2.0 单请求） |

`serverInfo` 为 `mustard-mip-mcp v1.0.0`，协议版本 `2024-11-05`。

## 客户端配置

Cursor / Claude 桌面端（或任意支持 MCP 的 Agent），配置文件示例：

```json
{
  "mcpServers": {
    "mustard": {
      "url": "https://api.example.com/api/app/open/mcp/sse",
      "headers": { "appkey": "YOUR_APP_KEY" }
    }
  }
}
```

## 工具清单（13 个）

| Tool | 类型 | 说明 |
|---|---|---|
| `list_items` | 读 | 分页查询物品（轻量摘要，含 contentHash） |
| `get_item` | 读 | 物品 MIP 档案（restricted 字段已服务端过滤） |
| `get_item_lineage` | 读 | 流转账本 + chainValid 哈希链校验 |
| `search_items` | 读 | 关键字搜索（keyword 必填，limit≤50） |
| `get_stats` | 读 | 聚合统计（总数 / 状态 / 分类 / 总价值） |
| `parse_item_draft` | 读 | **建档解析服务（不落库）**：自然语言 → 结构化草稿（draft 卡 + attributes + confidence + manualFill 敏感字段补录提示）；创建由调用方二次确认后经 `add_item` 提交 |
| `add_item` | 写 | 新增物品（attributes/confidence 必填，idempotencyKey 可选） |
| `update_item` | 写 | 编辑物品：itemId + attributes **全量覆盖**，同新增 MIP 写入链，账本记 `update` |
| `delete_item` | 写 | 删除物品（高危不可恢复）：账本记 `recycle`，历史保留可追溯 |
| `gift_item` | 写 | 两阶段赠送：`step=preview` 取 token → `step=execute` 执行 |
| `flow_offer` | 写 | 发起定向流转要约（72h 锁定期，接收方须为 App 用户） |
| `flow_respond` | 写 | 处理要约：action=accept 确认 / reject 拒绝 / cancel 撤回 |
| `flow_list` | 读 | 查询 72h 窗口内待响应要约（dir=sent/received/all） |

工具输出为 `content: [{ type: "text", text: "<JSON>" }]`；业务失败输出 `isError: true`（403 scope 不足 / 429 限流 / 404 不存在），Agent 可直接阅读错误信息。

## 调用示例（Streamable HTTP）

```bash
# initialize
curl -X POST 'https://api.example.com/api/app/open/mcp/sse' \
  -H 'appkey: YOUR_APP_KEY' -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"demo","version":"1"}}}'

# tools/list
curl -X POST 'https://api.example.com/api/app/open/mcp/sse' \
  -H 'appkey: YOUR_APP_KEY' -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/list"}'

# 调用工具（tools/call）
curl -X POST 'https://api.example.com/api/app/open/mcp/sse' \
  -H 'appkey: YOUR_APP_KEY' -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"list_items","arguments":{"pageSize":5}}}'
```

## 权限说明

- `readonly` 凭据可调用全部读类工具（含 parse_item_draft）；调用写工具（add_item / update_item / delete_item / gift_item / flow_offer / flow_respond）将返回 tool error「scope 不足」；
- 建档解析（parse_item_draft）属解析服务不落库：输出 draft/attributes/manualFill，**restricted 字段绝不参与解析**，仅提示手动补录；调用方拿 attributes + confidence 二次确认后调 add_item（confidence≥85 直接入库，<85 转草稿态由用户在 App 确认）；
- 写工具与 REST 写接口共用限流窗口（10 次/分钟），日写入配额默认 5000；解析工具计入读窗口与日配额；
- 每次调用均落审计，用户可在芥子空间 App「开放授权 → 调用记录」中查看。

调试可在控制台 Playground 选择「AppKey（直连）」模式，直接发送 JSON-RPC 单请求验证。
