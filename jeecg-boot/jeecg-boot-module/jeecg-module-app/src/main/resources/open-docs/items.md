---
title: 物品管理 API
summary: 物品开放接口（直连）：增删改查、统计、账本，MIP 出域契约
keywords: 物品, 直连, appkey, MIP, 账本, 增删改查, 统计, 开放接口
---

## 概述

物品开放能力统一由**直连接口**提供（`/api/app/open/*`，appkey 请求头鉴权）。本页为速查；完整请求/响应契约见 [开放 REST API](/docs/rest-openapi)。

> 早期文档中的网关通路 `/open/v1/*` 已废弃，从未上线——如请求该路径会收到「token 为空」类 JWT 拦截错误。

## 接口速查（均需 `appkey` 请求头）

| 方法 | 路径 | scope | 说明 |
| --- | --- | --- | --- |
| GET | `/app/open/item/list` | readonly | 分页列表（含 schemaId/contentHash） |
| GET | `/app/open/item/detail?itemId=` | readonly | MIP 出域档案 |
| GET | `/app/open/item/search?keyword=&limit=` | readonly | 关键字搜索（≤50） |
| GET | `/app/open/item/stats` | readonly | 数量/价值/分类聚合 |
| GET | `/app/open/item/lineage?itemId=` | readonly | MIP 转账账本（哈希链校验） |
| POST | `/app/open/item/add` | readwrite | 新增（MIP 写链 + 账本 mint + 建库存） |
| POST | `/app/open/item/update` | readwrite | 编辑（全量覆盖 attributes，账本 update） |
| POST | `/app/open/item/delete` | readwrite | 删除（高危，账本 recycle） |
| POST | `/app/open/gift/preview` → `/app/open/gift/send` | readwrite | 赠送三步流（token 一次性） |

## 通用约束

- 写接口必须带 `X-Idempotency-Key`（24h 回放）；
- attributes 为 MIP 组件数组 `[{element, value}]`，置信度 ≥85 正常入库，<85 转草稿，校验失败 422；
- 响应经 MIP 红线过滤：private/restricted 字段整条剥离，含 `content_hash` 与 `integrity.filtered` 标记。

MCP 接入（9 个工具）见 [MCP Server 接入](/docs/mcp)。
