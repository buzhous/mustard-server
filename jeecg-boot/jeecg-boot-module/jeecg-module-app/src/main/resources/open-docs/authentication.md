---
title: API 鉴权
summary: 开放平台鉴权方式：Access Token（JWT）与 AppKey 直连、Scope 权限、限流与幂等
keywords: 鉴权, Access Token, JWT, AppKey, appkey 请求头, Scope, 限流, 幂等, 密钥, 启用, 禁用, 删除
---

## 鉴权方式（两种）

开放平台能力由同一账号体系提供两种鉴权，**不存在独立的 mk_ 网关密钥**：

### 1. Access Token（JWT，管理接口）

登录后签发的 JWT（与 App 端登录同一账号），用于应用/密钥管理接口（`/api/app/authorization/*`）：

```
X-Access-Token: <登录返回的 accessToken>
```

- 获取：登录接口 `/api/app/login/signin`（与 App 同一账号密码）返回 `accessToken`；
- 开发平台控制台本身即用此方式调用（登录即生效）。

### 2. AppKey（直连开放接口）

开放授权页创建的 40 位 AppKey（hex），用于物品开放接口与 MCP（`/api/app/open/*`）：

```
appkey: cee1826887aa193e8650d9a3fad81f49c5939685
```

- AppKey 直接标识应用与绑定用户，**无需再传 token**；「token 为空」类报错说明路径不在开放接口白名单内（开放接口均以 `/api/app/open/` 开头）；
- 写接口必须携带幂等键：`X-Idempotency-Key: <业务幂等键>`（24h 内重复请求回放首次响应）。

## Scope 权限

每个 AppKey 有 `readonly` / `readwrite` 两级：

| scope | 能力 |
| --- | --- |
| readonly | 只读接口：list / detail / search / stats / lineage，MCP 只读工具 |
| readwrite | 追加写接口：item/add、item/update、item/delete、gift 三步流，MCP 全部写工具 |

在授权页「数据权限」开关切换（写入测试请确认密钥已开通读写）。

## 限流

| 维度 | 上限 | 说明 |
| --- | --- | --- |
| 每分钟总请求 | 60 | 超限返回 429，携带 `X-RateLimit-Reset` 重置秒数 |
| 每分钟写请求 | 10 | add/update/delete/gift 共享窗口 |
| 每日配额 | 5000 | 默认值，可按凭据配置 |

## 密钥管理

授权页支持：创建 / 重命名 / 启用 / 禁用 / 删除 / 数据权限切换 / 调用记录查询，全部走 Access Token 鉴权的管理接口；对应控制台「我的应用」页面。
