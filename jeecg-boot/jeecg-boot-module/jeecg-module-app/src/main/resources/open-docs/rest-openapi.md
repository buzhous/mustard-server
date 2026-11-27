---
title: 物品开放 REST API（直连）
summary: /app/open/* 直连端点：物品查询、统计、账本、受控写入与赠送三步流
keywords: 直连, appkey, REST, 物品, 列表, 统计, 账本, 创建, 赠送, 幂等, 限流, contentHash
scope: readonly readwrite
---

## 与网关通路的区别

开放接口提供两条通路：本页为**直连接口**（`/api/app/open/*`，appkey 请求头鉴权，无需签名）；「物品管理 API」页描述的是 jeecg 网关通路（`/open/v1/*`）。两条通路能力一致时可任选；新能力（MCP、账本校验、赠送三步流）优先在直连接口提供。

## 鉴权与请求头

| Header | 必填 | 说明 |
|---|---|---|
| `appkey` | 是 | 40 位随机哈希，仅 HTTPS 传输，无需签名 |
| `X-Idempotency-Key` | 写接口必填 | 幂等键，24h 内重复请求回放首次响应 |
| `Content-Type` | 写接口必填 | `application/json` |

数据范围仅限凭据绑定的用户物品。scope 默认 `readonly`，写接口需在 App 授权页开通「读写」。

## 只读接口（readonly 即可）

| 接口 | 说明 |
|---|---|
| `GET /api/app/open/item/list` | 物品分页（pageNo/pageSize/keyword/orderBy），记录含 `contentHash`/`schemaId` 摘要 |
| `GET /api/app/open/item/detail?itemId=` | 物品 MIP 出域实例（header/identity/attributes/custody/lineage_digest/integrity） |
| `GET /api/app/open/item/search?keyword=&limit=&fields=` | 关键字搜索，limit≤50，fields 按 element 过滤 |
| `GET /api/app/open/item/stats` | 聚合统计 total/byStatus/byCategory/totalValue |
| `GET /api/app/open/item/lineage?itemId=` | 流转账本事件 + `chainValid` 完整性校验 |

## 写接口（需 readwrite）

### 物品入库：POST /api/app/open/item/add

```bash
curl -X POST 'https://api.example.com/api/app/open/item/add' \
  -H 'appkey: YOUR_APP_KEY' \
  -H 'X-Idempotency-Key: pg-add-001' \
  -H 'Content-Type: application/json' \
  -d '{
    "attributes": [
      {"element": 1, "value": "物品名称"},
      {"element": 2, "value": "https://example.com/icon.png"},
      {"element": 3, "value": "1"}
    ],
    "confidence": 95,
    "source": "ai-assistant"
  }'
```

- 必填组件：1 名称 / 2 图标（合法 URL）/ 3 类别；
- `confidence ≥ 85` 直接入库（message=normal），否则进入草稿确认态（message=draft，App 端弹确认）；
- 服务端单点计算 `sensitivity`，私有/受限字段出域前整条剥离并重算 `content_hash`；
- 校验失败返回 422 与 errors 数组；
- 成功后同步创建库存记录，MIP 账本记 `mint`（operator=`agent:{appKey尾4位}`）。

### 物品编辑：POST /api/app/open/item/update

```bash
curl -X POST 'https://api.example.com/api/app/open/item/update' \
  -H 'appkey: YOUR_APP_KEY' \
  -H 'X-Idempotency-Key: pg-update-001' \
  -H 'Content-Type: application/json' \
  -d '{
    "itemId": "物品ID（必须是该账号名下物品）",
    "attributes": [
      {"element": 1, "value": "新名称"},
      {"element": 2, "value": "https://example.com/icon.png"},
      {"element": 3, "value": "1"}
    ],
    "confidence": 95
  }'
```

- `attributes` **全量覆盖**（未传的组件将被移除）；`itemId` 归属校验（404 不在凭据数据范围）；
- 与新增同一 MIP 写入链：enrich → identity 同步 → 422 全量校验；
- `confidence ≥ 85` 恢复正常态，`< 85` 转草稿，不传保持原状态；
- 账本追加 `update` 事件（自持，from=to=用户，operator=`agent:*`）；幂等键 24h 回放首次响应。

### 物品删除：POST /api/app/open/item/delete

```bash
curl -X POST 'https://api.example.com/api/app/open/item/delete' \
  -H 'appkey: YOUR_APP_KEY' \
  -H 'X-Idempotency-Key: pg-delete-001' \
  -H 'Content-Type: application/json' \
  -d '{"itemId": "物品ID"}'
```

- **高危不可恢复**：删除物品记录与库存记录，返回 `{itemId, oriId, deleted:true}`；
- 账本追加 `recycle` 事件（历史保留，账本链可追溯该物品全生命周期）；
- 幂等键 24h 回放首次响应（重复删除返回首次的 deleted:true）。

### 赠送三步流（不可撤销）

| 步骤 | 接口 | 说明 |
|---|---|---|
| 1 预览 | `POST /api/app/open/gift/preview` `{"itemId":"..."}` | 返回一次性 `giftToken`（5 分钟有效，绑定 appKey）+ 物品摘要 + warning |
| （用户确认） | 第三方需完整展示预览结果 | 禁止跳过确认直接执行 |
| 2 执行 | `POST /api/app/open/gift/send` `{"giftToken":"...","toUserId":"..."}` | 创建 PENDING 赠送单、扣减赠出方库存、账本记 `gift_out`；接收方在 App 端领取 |
| 3 幂等保证 | giftToken 一次性 | 复用返回 404 |

## 限流与审计

| 维度 | 阈值 | 超限响应 |
|---|---|---|
| 每分钟总调用 | 60 次/凭据 | 429，响应头 `X-RateLimit-Reset` |
| 每分钟写调用 | 10 次/凭据（与 MCP 写工具共用） | 429 |
| 日写入配额 | credential.daily_quota（默认 5000） | 429 |

每次调用异步落库 `tbl_open_audit_log`（channel=rest/mcp、action=read/write、result_code、cost_ms），用户可在 App「开放授权 → 调用记录」查看。

## 错误码

| code | HTTP | 场景 |
|---|---|---|
| 401 | 401 | 缺少 / 无效 / 停用的 appkey |
| 403 | 403 | IP 白名单拒绝；scope 不足（写接口需 readwrite） |
| 404 | 404 | 物品不存在或非本凭据范围；giftToken 无效/过期/复用 |
| 422 | 422 | MIP 校验失败，result.errors 数组 |
| 429 | 429 | 触发限流 |
