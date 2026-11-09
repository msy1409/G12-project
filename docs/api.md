# G12 访客预约平台 · 接口文档（api.md）

> 负责人：B（后端）　调用方：A（前端）　契约状态：**已冻结**
> 版本：v1.0　共 **27 个业务接口** + 1 个健康检查接口
> 改动规则：字段名不许随口改。要改 = 先改本文档 → 再改代码 → 再通知 A 重拉。否则 A 的页面全部作废。

---

## 1. 通用约定

### 1.1 Base URL

| 环境 | 地址 |
|---|---|
| 本地 SpringBoot | `http://localhost:8080/api` |
| Compose | `http://localhost:8080/api`（nginx 反代 `/api` → `backend:8080/api`） |
| K8s | `http://g12.course.local/api`（Ingress → `g12-frontend` → `g12-backend:80`） |

后端 `application.yml` 已配 `server.servlet.context-path: /api`，所以代码里 `@RequestMapping("/auth")` 实际对外是 `/api/auth`。**前端 axios 写 `baseURL: '/api'`，页面里只写 `/auth/login`。**

### 1.2 统一响应结构

```json
{
  "code": 200,
  "message": "ok",
  "data": {}
}
```

- `code`：`200` 成功，其余为错误码（见 1.4）。**HTTP 状态码同步返回**，前端两处都能判断。
- `data`：成功时为业务数据，失败时为 `null`。

### 1.3 鉴权

```
Authorization: Bearer <jwt>
```

- JWT payload：`{ userId, username, role, deptId, exp }`，有效期 2 小时。
- 缺 token / 格式错 / 过期 → `401`，前端 axios 拦截器清 `localStorage.token` 并跳 `/login`。
- 除 `POST /auth/login` 和 `GET /actuator/health` 外，**所有接口都要 token**。

### 1.4 错误码

| code | 含义 | 典型场景 |
|---|---|---|
| 200 | 成功 | — |
| 400 | 参数或业务错误 | 状态流转非法、必填缺失、核验码错误 |
| 401 | 未认证 | 无 token、token 过期、密码错、账号停用 |
| 403 | 无权限 | 员工调审核接口、审核员碰别的部门 |
| 404 | 资源不存在 | 预约单 id 不存在 |
| 409 | 冲突 | 用户名重复、部门名重复、重复入场 |
| 500 | 系统错误 | 未捕获异常 |

具体业务码放在 `message` 里，取值固定：

`STATE_NOT_ALLOWED`、`INVALID_CREDENTIALS`、`USER_DISABLED`、`TOKEN_EXPIRED`、`FORBIDDEN`、`DEPT_SCOPE_DENIED`、`NOT_FOUND`、`USERNAME_EXISTS`、`DEPT_NAME_EXISTS`、`DEPT_IN_USE`、`DUPLICATE_CHECKIN`、`NOT_CHECKED_IN`、`VERIFY_CODE_INVALID`。

### 1.5 分页

请求：`?page=1&size=10`（page 从 1 开始，size 默认 10，最大 100）

响应：

```json
{ "code": 200, "message": "ok",
  "data": { "list": [], "total": 42, "page": 1, "size": 10 } }
```

### 1.6 权限标记说明

`[公开]` 免登录 · `[登录]` 任何角色 · `[EMPLOYEE]` / `[AUDITOR]` / `[ADMIN]` 指定角色 · `[AUDITOR,ADMIN]` 两者之一。
**AUDITOR 的所有数据操作都自动附加 `dept_id = 当前用户 deptId` 过滤，跨部门请求返回 403 `DEPT_SCOPE_DENIED`。**

---

## 2. 认证 `auth`（3 个）

### 2.1 POST `/auth/login`　[公开]

请求：

```json
{ "username": "zhangsan", "password": "123456" }
```

响应：

```json
{ "code": 200, "message": "ok",
  "data": {
    "token": "eyJhbGciOi...",
    "user": { "id": 3, "username": "zhangsan", "realName": "张三",
              "role": "EMPLOYEE", "deptId": 1, "deptName": "研发部" }
  } }
```

错误：`401 INVALID_CREDENTIALS`（用户名或密码错）、`401 USER_DISABLED`（status=0）。

### 2.2 GET `/auth/me`　[登录]

返回当前登录人信息（前端刷新页面后恢复用户态用）。

```json
{ "code": 200, "message": "ok",
  "data": { "id": 3, "username": "zhangsan", "realName": "张三", "phone": "13800000003",
            "role": "EMPLOYEE", "deptId": 1, "deptName": "研发部", "status": 1 } }
```

错误：`401 TOKEN_EXPIRED`。

### 2.3 POST `/auth/logout`　[登录]

JWT 无状态，服务端不做黑名单，仅返回成功让前端统一清 token。

```json
{ "code": 200, "message": "ok", "data": null }
```

---

## 3. 用户管理 `users`（5 个，全 `[ADMIN]`）

### 3.1 GET `/users`　[ADMIN]

查询参数：`keyword`（用户名/姓名模糊）、`deptId`、`role`、`status`、`page`、`size`

```json
{ "code": 200, "message": "ok",
  "data": { "list": [
      { "id": 3, "username": "zhangsan", "realName": "张三", "phone": "13800000003",
        "deptId": 1, "deptName": "研发部", "role": "EMPLOYEE", "status": 1,
        "createTime": "2026-11-01 09:12:00" }
    ], "total": 12, "page": 1, "size": 10 } }
```

> 响应里**永远不含 `password` 字段**。

### 3.2 POST `/users`　[ADMIN]

```json
{ "username": "lisi", "password": "123456", "realName": "李四",
  "phone": "13800000010", "deptId": 2, "role": "AUDITOR" }
```

- `username` 唯一，`password` 明文传入、服务端 BCrypt 加密后存。
- `role` 只能是三个枚举值，否则 400。

响应：`{ "code": 200, "data": { "id": 13 } }`　错误：`409 USERNAME_EXISTS`。

### 3.3 PUT `/users/{id}`　[ADMIN]

只改基本信息，**不在此接口改密码**。

```json
{ "realName": "李四四", "phone": "13800000011", "deptId": 2, "role": "AUDITOR" }
```

响应：`{ "code": 200, "data": null }`　错误：`404 NOT_FOUND`。

### 3.4 PUT `/users/{id}/status`　[ADMIN]

```json
{ "status": 0 }
```

`1` 启用 / `0` 停用。停用后该用户立即无法登录，已签发 token 在拦截器复查时失效。

响应：`{ "code": 200, "data": null }`

### 3.5 DELETE `/users/{id}`　[ADMIN]

**实现为 `status=0`，不是物理删除**（外键与审计要求）。

```json
{ "code": 200, "message": "ok", "data": null }
```

> 不允许管理员停用自己：返回 `400 STATE_NOT_ALLOWED`。

---

## 4. 部门管理 `departments`（4 个）

### 4.1 GET `/departments`　[登录]

所有角色可读——预约表单、用户表单、筛选器都要用它填下拉框。

```json
{ "code": 200, "data": [
    { "id": 1, "deptCode": "D001", "deptName": "研发部", "managerName": "王经理",
      "phone": "010-88880001", "status": 1 }
  ] }
```

> 该接口不分页，返回全部（含停用部门，前端自行按 `status` 过滤）。`?status=1` 可只取启用的。

### 4.2 POST `/departments`　[ADMIN]

```json
{ "deptCode": "D011", "deptName": "法务部", "managerName": "赵经理", "phone": "010-88880011" }
```

错误：`409 DEPT_NAME_EXISTS`（`dept_code` 或 `dept_name` 重复都返回它）。

### 4.3 PUT `/departments/{id}`　[ADMIN]

```json
{ "deptName": "法务合规部", "managerName": "赵经理", "phone": "010-88880011" }
```

### 4.4 DELETE `/departments/{id}`　[ADMIN]

实现为 `status=0`。

- 该部门下还有 `status=1` 的用户 → `400 DEPT_IN_USE`
- 该部门下还有未完结预约（status in 0,1,2,3）→ `400 DEPT_IN_USE`

---

## 5. 预约 `applies`（4 个）

### 5.1 POST `/applies`　[EMPLOYEE]

员工替访客提交预约。

```json
{ "visitorName": "陈七", "visitorPhone": "13900000007", "visitorCompany": "外部供应商",
  "deptId": 1, "hostName": "张三", "visitReason": "设备维修",
  "visitTime": "2026-11-12 09:30:00", "leaveTime": "2026-11-12 11:00:00" }
```

- `deptId` 默认取当前用户部门，可改（跨部门拜访）。
- `create_by` = 当前用户 id，`status` 初始 0。
- 服务端生成 `apply_no`，并写一条 `CREATE` 处理记录 + 一条 `0←∅` 状态审计（`from_status` 用 `-1` 表示新建）。

响应：

```json
{ "code": 200, "data": { "id": 101, "applyNo": "AP20261112000001", "status": 0 } }
```

错误：`400`（`visitTime` 早于当前时间）。

### 5.2 GET `/applies/mine`　[EMPLOYEE]

查询参数：`status`（可选）、`page`、`size`

只返回 `create_by = 当前用户` 的单。

```json
{ "code": 200, "data": {
    "list": [ { "id": 101, "applyNo": "AP20261112000001", "visitorName": "陈七",
                "visitorPhone": "13900000007", "deptName": "研发部", "hostName": "张三",
                "visitTime": "2026-11-12 09:30:00", "status": 0,
                "createTime": "2026-11-09 10:00:00" } ],
    "total": 1, "page": 1, "size": 10 } }
```

### 5.3 GET `/applies/{id}`　[登录]

详情，含核验码（仅状态 ≥2 时返回）、处理记录、状态变更历史。

```json
{ "code": 200, "data": {
    "id": 101, "applyNo": "AP20261112000001", "visitorName": "陈七",
    "visitorPhone": "13900000007", "visitorCompany": "外部供应商",
    "deptId": 1, "deptName": "研发部", "hostName": "张三", "visitReason": "设备维修",
    "visitTime": "2026-11-12 09:30:00", "leaveTime": "2026-11-12 11:00:00",
    "verifyCode": "483920", "status": 2,
    "processList": [ { "action": "CREATE", "operatorName": "张三",
                       "operatorRole": "EMPLOYEE", "remark": null,
                       "createTime": "2026-11-09 10:00:00" } ],
    "statusLogList": [ { "fromStatus": -1, "toStatus": 0, "action": "CREATE",
                         "operatorName": "张三", "createTime": "2026-11-09 10:00:00" } ] } }
```

权限：EMPLOYEE 只能看自己创建的；AUDITOR 只能看本部门的；ADMIN 全部。越权 `403`。

### 5.4 PUT `/applies/{id}/cancel`　[EMPLOYEE, ADMIN]

员工取消自己未开始的预约。

- 允许取消的状态：0、1、2
- 状态 3（已到访）、4（已离开）不可取消 → `400 STATE_NOT_ALLOWED`
- 流转到 9，`action=CANCEL`，写审计 + 处理记录

请求体：`{ "reason": "行程取消" }`（可选）

响应：`{ "code": 200, "data": { "status": 9 } }`

---

## 6. 审核 `audit`（5 个）

### 6.1 GET `/applies/pending`　[AUDITOR, ADMIN]

待审核列表（`status=0`）。AUDITOR 自动限定本部门。

查询参数：`keyword`（访客名/单号）、`page`、`size`

```json
{ "code": 200, "data": {
    "list": [ { "id": 101, "applyNo": "AP20261112000001", "visitorName": "陈七",
                "visitorCompany": "外部供应商", "deptName": "研发部", "hostName": "张三",
                "visitReason": "设备维修", "visitTime": "2026-11-12 09:30:00",
                "createTime": "2026-11-09 10:00:00" } ],
    "total": 1, "page": 1, "size": 10 } }
```

### 6.2 PUT `/applies/{id}/approve`　[AUDITOR, ADMIN]

**只做 0→1**，不生成核验码、不跳状态。

请求体：`{ "remark": "同意" }`（可选）

响应：`{ "code": 200, "data": { "status": 1 } }`

错误：`400 STATE_NOT_ALLOWED`（当前不是 0）、`403 DEPT_SCOPE_DENIED`（跨部门）。

### 6.3 PUT `/applies/{id}/reject`　[AUDITOR, ADMIN]

0→9，`action=REJECT`。

```json
{ "reason": "来访事由不明确" }
```

`reason` 必填，最长 255。响应：`{ "code": 200, "data": { "status": 9 } }`

### 6.4 PUT `/applies/{id}/verify-code`　[AUDITOR, ADMIN]

**1→2**，生成 6 位数字核验码。

无请求体。

```json
{ "code": 200, "data": { "status": 2, "verifyCode": "483920" } }
```

- 6 位码用 `SecureRandom` 生成，查库去重，最多重试 3 次。
- 该接口只在 1 状态下可用，重复调用 → `400 STATE_NOT_ALLOWED`。

### 6.5 GET `/applies`　[AUDITOR, ADMIN]

审核记录查询（全量，不限状态）。

查询参数：`status`、`deptId`（仅 ADMIN 有效）、`keyword`（访客名/手机号/单号）、`startDate`、`endDate`、`page`、`size`

响应结构同 6.1，额外带 `status` 和 `verifyCode` 字段。

---

## 7. 到访登记 `records`（3 个）

> 门岗由 AUDITOR 轮值或 ADMIN 操作。AUDITOR 只能核验本部门的单。

### 7.1 POST `/records/checkin`　[AUDITOR, ADMIN]

访客到场，凭核验码登记入场。

```json
{ "verifyCode": "483920", "gate": "东门-01" }
```

服务端校验：`status=2` → 码匹配 → 部门权限 → `INSERT visitor_record(checkin_time=now)` → `transit(2→3, action=CHECKIN)`。

```json
{ "code": 200, "data": { "recordId": 55, "applyId": 101, "visitorName": "陈七",
                          "checkinTime": "2026-11-12 09:28:00", "status": 3 } }
```

错误：`400 VERIFY_CODE_INVALID`（码错或已用）、`400 STATE_NOT_ALLOWED`（不是 2）、`409 DUPLICATE_CHECKIN`。

### 7.2 POST `/records/checkout`　[AUDITOR, ADMIN]

```json
{ "applyId": 101, "gate": "东门-01" }
```

校验：`status=3` 且存在 `checkout_time IS NULL` 的记录 → 更新 `checkout_time=now` → `transit(3→4, action=CHECKOUT)`。

响应：

```json
{ "code": 200, "data": { "applyId": 101, "checkoutTime": "2026-11-12 11:05:00",
                          "stayMinutes": 97, "status": 4 } }
```

错误：`400 NOT_CHECKED_IN`、`400 STATE_NOT_ALLOWED`。

### 7.3 GET `/records`　[AUDITOR, ADMIN]

到访记录查询。参数：`date`（yyyy-MM-dd，按 `checkin_time` 过滤）、`deptId`、`keyword`、`page`、`size`

```json
{ "code": 200, "data": {
    "list": [ { "id": 55, "applyId": 101, "applyNo": "AP20261112000001",
                "visitorName": "陈七", "deptName": "研发部", "gate": "东门-01",
                "checkinTime": "2026-11-12 09:28:00", "checkoutTime": "2026-11-12 11:05:00",
                "stayMinutes": 97 } ],
    "total": 1, "page": 1, "size": 10 } }
```

---

## 8. 统计 `stats`（3 个）

### 8.1 GET `/stats/overview`　[AUDITOR, ADMIN]

顶部卡片用的汇总数。AUDITOR 返回本部门口径，ADMIN 返回全局。

无参数。

```json
{ "code": 200, "data": {
    "todayApplyCount": 5,
    "pendingCount": 3,
    "todayCheckinCount": 2,
    "monthCheckinCount": 38,
    "avgStayMinutes": 62.5
  } }
```

- `avgStayMinutes`：`AVG(TIMESTAMPDIFF(MINUTE, checkin_time, checkout_time))`，无数据返回 0。
- `pendingCount`：`status=0` 的条数——这是给审核员看的核心指标。

### 8.2 GET `/stats/trend?days=7`　[AUDITOR, ADMIN]

近 N 天按日期趋势，用于折线图。

```json
{ "code": 200, "data": [
    { "date": "2026-11-03", "applyCount": 4, "checkinCount": 3 },
    { "date": "2026-11-04", "applyCount": 6, "checkinCount": 5 }
  ] }
```

- 返回连续 N 天，没有数据的那天补 0（服务端补，不要前端补）。

### 8.3 GET `/stats/department`　[ADMIN]

按部门统计，用于柱状图。

```json
{ "code": 200, "data": [
    { "deptId": 1, "deptName": "研发部", "applyCount": 12, "checkinCount": 9 },
    { "deptId": 2, "deptName": "市场部", "applyCount": 8,  "checkinCount": 6 }
  ] }
```

---

## 9. 健康检查（基础设施，不占总接口数）

### GET `/actuator/health`　[公开]

```json
{ "status": "UP", "components": { "db": { "status": "UP" }, "diskSpace": { "status": "UP" } } }
```

- K8s 探针打的就是这个地址：`/api/actuator/health`（带 context-path）。
- Compose healthcheck 用 `curl -f http://localhost:8080/api/actuator/health`。
- **拦截器必须放行 `/actuator/health`（相对 context-path），否则探针 401、Pod 永远不 Ready。**

---

## 10. 页面 ↔ 接口对照表（A 建页面的依据）

| 页面 | 路由 | 角色 | 调用的接口 |
|---|---|---|---|
| Login | `/login` | 公开 | 1.1 login、1.2 me |
| ApplyNew | `/apply/new` | EMPLOYEE | 5.1 新建、4.1 部门下拉 |
| MyApplies | `/apply/mine` | EMPLOYEE | 5.2 我的、5.3 详情、5.4 取消 |
| AuditList | `/audit/list` | AUDITOR/ADMIN | 6.1 待审核、6.2 approve、6.3 reject |
| AuditDetail | `/audit/:id` | AUDITOR/ADMIN | 5.3 详情、6.2、6.3、6.4 verify-code、6.5 记录 |
| GateCheck | `/gate` | AUDITOR/ADMIN | 7.1 checkin、7.2 checkout、7.3 records |
| AdminUsers | `/admin/users` | ADMIN | 3.1–3.5、4.1 部门下拉 |
| AdminDepts | `/admin/depts` | ADMIN | 4.1–4.4 |
| StatsDashboard | `/stats` | AUDITOR/ADMIN | 8.1 overview、8.2 trend、8.3 department |

> 页面只是保底对照。**页面结构最终以本文档的资源分组为准**，资源分组就是菜单的天然边界。

---

## 11. 状态流转 × 接口 速查

| 接口 | 动作 | 状态变化 | 写审计 | 写处理记录 |
|---|---|---|---|---|
| POST `/applies` | CREATE | ∅→0 | ✅ | ✅ |
| PUT `/applies/{id}/approve` | APPROVE | 0→1 | ✅ | ✅ |
| PUT `/applies/{id}/reject` | REJECT | 0→9 | ✅ | ✅ |
| PUT `/applies/{id}/verify-code` | VERIFY_CODE | 1→2 | ✅ | ✅ |
| POST `/records/checkin` | CHECKIN | 2→3 | ✅ | ✅ |
| POST `/records/checkout` | CHECKOUT | 3→4 | ✅ | ✅ |
| PUT `/applies/{id}/cancel` | CANCEL | 0/1/2→9 | ✅ | ✅ |

五条主链路状态：`0 待审核 → 1 审核通过 → 2 待到访 → 3 已到访 → 4 已离开`，每一步都停在库里，可以逐条演示。
