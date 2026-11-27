---
title: 错误码
summary: 开放 API 统一响应结构与错误码排查
keywords: 错误码, 错误, 异常, 401, 403, 404, 422, 429, 500, HTTP
---

## 统一响应结构

```json
{
  "code": 200,
  "success": true,
  "message": "操作成功",
  "result": { }
}
```

- `code`：业务码（200 成功）；
- `success`：是否成功；
- `message`：人类可读信息；
- `result`：业务数据（各接口结构见对应文档）。

HTTP 状态码与业务码对齐：业务失败时 HTTP 状态码同步为 `code`（如 403 / 422 / 429）。

## HTTP 状态码

| HTTP | 场景 | 排查建议 |
| --- | --- | --- |
| 400 | 参数缺失 / 格式错误（itemId 与 attributes 为空等） | 检查请求体字段与 JSON 格式 |
| 401 | appkey 缺失、密钥不存在或已禁用 | 确认 `appkey` 请求头与密钥状态（应用详情页） |
| 403 | IP 不在白名单 / scope 不足（只读密钥调写接口）/ 数据越权 | 授权页升级数据权限为读写；核对资源归属 |
| 404 | 资源不存在（物品 / 库存 / 幂等键回放 miss） | 用列表或搜索接口确认 ID（字符串回填） |
| 422 | MIP 校验失败（attributes 未过组件校验，如置信度 < 85） | 查看 message 中缺失的组件（element 1 名称 / 2 图标 / 3 类别必填） |
| 429 | 限流（读 60/min、写 10/min、日配额超限） | 响应头 `X-RateLimit-Reset` 为窗口重置秒级时间戳 |
| 500 | 服务内部错误 | 检查 message；持续复现请联系平台 |

## MCP 工具级错误

MCP 通道业务失败不中断会话，以 JSON-RPC 结果标注：

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "content": [{ "type": "text", "text": "{\"code\":403,\"message\":\"scope 不足\"}" }],
    "isError": true
  }
}
```

- `result.isError: true`：工具执行失败，错误详情在 `content[0].text`（JSON 文本）；
- JSON-RPC 协议级错误（`error` 对象）：请求格式不合法（缺 jsonrpc/method 等）。

## 幂等回放语义

写请求携带 `X-Idempotency-Key`（MCP 为 `arguments.idempotencyKey`）：

- 首次请求：正常执行并缓存响应（24 小时）；
- 同键重放：**直接回放缓存的首次响应**（不重复执行，状态码/响应体与首次完全一致）。
