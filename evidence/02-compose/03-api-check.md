# 03 接口自查记录（Compose 环境，不依赖前端）

## 1. 环境

| 项 | 值 |
| --- | --- |
| 宿主机 | VirtualBox 虚拟机 `docker`（Ubuntu 22.04.5 LTS，Docker 29.8.1） |
| 编排 | `docker compose up -d --no-build mysql backend`（frontend 未启动） |
| 后端镜像 | `g12-api:v2`（构建于 2026-11-13 17:36:30） |
| 后端容器 IP | `172.20.0.3`，端口 8080，context-path `/api` |
| 数据库 | `g12-mysql`（healthy），库 `ctts_g12` |
| 执行时间 | 2026-11-13 17:43 CST |
| 脚本 | `api_check.sh`，`BASE=http://172.20.0.3:8080/api` |

## 2. 结论

```
TOTAL=29  OK=29  FAIL=0
```

`docs/api.md` 冻结的 27 个业务接口全部覆盖，另加 3 次建单和 1 次终态复查，共 29 条断言，全部 HTTP 200 且 `code=200`。

## 3. 逐条记录

| # | 方法与路径 | HTTP | 结论 |
| --- | --- | --- | --- |
| 01 | POST /auth/login（admin） | 200 | OK |
| 02 | POST /auth/login（auditor01） | 200 | OK |
| 03 | POST /auth/login（emp01） | 200 | OK |
| 04 | GET /auth/me | 200 | OK |
| 05 | POST /auth/logout | 200 | OK |
| 06 | GET /users?page=1&size=5 | 200 | OK |
| 07 | POST /users | 200 | OK |
| 08 | PUT /users/14 | 200 | OK |
| 09 | PUT /users/14/status | 200 | OK |
| 10 | DELETE /users/14 | 200 | OK |
| 11 | GET /departments | 200 | OK |
| 12 | POST /departments | 200 | OK |
| 13 | PUT /departments/12 | 200 | OK |
| 14 | DELETE /departments/12 | 200 | OK |
| 15 | POST /applies ×3（新建 18/19/20） | 200 | OK |
| 15 | GET /applies/mine?page=1&size=5 | 200 | OK |
| 16 | GET /applies/19 | 200 | OK |
| 17 | PUT /applies/18/cancel | 200 | OK |
| 18 | GET /applies/pending?page=1&size=5 | 200 | OK |
| 19 | PUT /applies/19/approve（0→1） | 200 | OK |
| 20 | PUT /applies/20/reject（0→9） | 200 | OK |
| 21 | PUT /applies/19/verify-code（1→2，返回 6 位码） | 200 | OK |
| 22 | GET /applies?page=1&size=5 | 200 | OK |
| 23 | POST /records/checkin（2→3） | 200 | OK |
| 24 | POST /records/checkout（3→4，含 stayMinutes） | 200 | OK |
| 25 | GET /records?page=1&size=5 | 200 | OK |
| 26 | GET /stats/overview | 200 | OK |
| 27 | GET /stats/trend?days=7 | 200 | OK |
| 28 | GET /stats/department | 200 | OK |
| 29 | GET /applies/19（终态复查） | 200 | OK |

## 4. 本次自查发现并修复的两个契约违背

第一轮在容器里跑出 `OK=14 FAIL=15`，其中 11 个是测试脚本取值错误，但暴露出两处**后端与 `docs/api.md` 不一致**的真实问题：

### 4.1 `AuditController` 5 个接口路径与契约不符（严重）

- 原代码：`@RequestMapping("/audit")` + `@GetMapping("/list")`，实际路径是 `/audit/pending`、`/audit/list` 等。
- 契约要求：`GET /applies/pending`、`PUT /applies/{id}/approve`、`PUT /applies/{id}/reject`、`PUT /applies/{id}/verify-code`、`GET /applies`。
- 后果：`GET /applies/pending` 会落到 `ApplyController` 的 `@GetMapping("/applies/{id}")` 上，后者类级注解是 `@RequireRole({EMPLOYEE, ADMIN})`，于是 **AUDITOR 审核员的待审列表被直接打成 403 FORBIDDEN**（实测 auditor01 返回 `{"code":403,"message":"FORBIDDEN"}`）。前端按契约调用必然拿不到数据。
- 修复：类级改为 `@RequestMapping("/applies")`，列表方法 `@GetMapping("/list")` 改为 `@GetMapping`。

### 4.2 `UserController` 缺少 `PUT /users/{id}/status`

- 契约 `api.md` 3.4 要求有停用/启用接口，代码只有 `PUT /users/{id}` 与 `DELETE /users/{id}`，另外多了一个契约里没有的 `GET /users/{id}`。
- 修复：`UserService` 增加 `changeStatus(Long id, Integer status)`（校验 `status ∈ {0,1}`、禁止停用自己、用户不存在抛 404），`UserController` 增加 `@PutMapping("/{id}/status")`。

两处修复后重新 `mvn -B -q compile` 通过，并重出镜像（见第 6 节）。

## 5. 已知问题（未在本次范围内修改）

1. 空路径变量（如 `PUT /users//status`）返回 500 `INTERNAL_ERROR`，而不是 400/404。属健壮性问题，不影响正常调用。
2. 容器内冷启动较慢：`WebApplicationContext` 初始化耗时约 40 秒（本机约 3 秒），首次探测会出现 `health: starting`，`compose.yaml` 已将 `start_period` 放宽到 90s、`timeout` 放宽到 10s。
3. 本次自查向库中写入了测试数据（用户 id=14、部门 id=12、申请 18/19/20 及其处理记录与状态审计）。**正式演示前需重灌 `database/schema.sql` + `database/seed.sql` 恢复干净种子数据。**

## 6. 镜像重出

因 4.1、4.2 属于代码变更，两个镜像已重建并重新导出：

| 镜像 | 构建时间 | 说明 |
| --- | --- | --- |
| `g12-api:v1` | 2026-11-13 17:32:44 | 不含 `apiVersion` 字段赋值（用于 A7 基线演示） |
| `g12-api:v2` | 2026-11-13 17:36:30 | 含 `apiVersion` 字段赋值（发布变更演示） |

字节码校验：`AuthService.class` 中 `setApiVersion` 常量，v1 = False、v2 = True。
