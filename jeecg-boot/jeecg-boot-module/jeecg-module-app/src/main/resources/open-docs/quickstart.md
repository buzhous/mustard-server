---
title: 快速接入
summary: 登录、创建应用、取得 AppKey、发起第一次调用
keywords: 快速开始, AppKey, 登录, 应用, 接入, 调用
---

## 接入流程

1. **登录开放平台**：与 App 完全同一账号体系（账号 + 密码或短信），登录后全局免登录，可从任意入口跳转开放平台；
2. **创建应用**：控制台 → 应用 → 新建应用，填写名称与描述，选择数据权限（只读 / 读写）；
3. **取得 AppKey**：应用创建成功即自动生成 40 位 AppKey（一个应用即一个密钥），在应用详情页复制；
4. **发起调用**：带 `appkey` 请求头调用 REST 接口，或按 MCP 协议接入 AI Agent。

## 第一次调用（REST）

```bash
# 读取物品列表（appkey 鉴权，无需 Access Token）
curl 'https://api.example.com/api/app/open/item/list?pageNo=1&pageSize=10' \
  -H 'appkey: 你的40位AppKey'
```

## 第一次调用（MCP）

```bash
curl -X POST 'https://api.example.com/api/app/open/mcp/sse' \
  -H 'Content-Type: application/json' \
  -H 'appkey: 你的40位AppKey' \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'
```

## 在线调试

登录后进入控制台 → 接口调试：OpenAPI（REST）与 MCP（JSON-RPC）两个 TARGET 分开调试，AppKey 可从下拉直接选择本账号的应用，写请求自动携带 `X-Idempotency-Key`。

## 注意事项

- 所有 ID（itemId / credentialId 等）出参均为**字符串**（雪花 ID 超出 JS 安全整数），调用时按字符串回填；
- 写请求（创建 / 更新 / 删除 / 赠送）需要应用权限为 `readwrite`，并携带幂等键（同键 24 小时内回放首次响应）；
- 限流：每 AppKey 60 次/分钟（读），10 次/分钟（写），超出返回 429。

更多细节：[开放鉴权](/api/open/docs/authentication.md) · [物品管理 API](/api/open/docs/items.md) · [REST 直连](/api/open/docs/rest-openapi.md) · [MCP 接入](/api/open/docs/mcp.md)
