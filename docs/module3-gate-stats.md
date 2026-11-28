# 模块三 · 门岗核验与数据统计（C）联调与部署说明

> 适用：`main` 分支当前版本；对应页面 `frontend/src/views/GateCheck.vue`、`frontend/src/views/StatsDashboard.vue`；
> 对应后端 `RecordController`、`StatsController`（业务逻辑未改动，本文只补充调用与联调口径）。

## 1. 模块范围

| 能力 | 页面 | 后端接口 | 角色 |
|---|---|---|---|
| 门岗核验入场 | `/gate` | `POST /api/records/checkin` | ADMIN、AUDITOR |
| 登记离场 | `/gate` | `POST /api/records/checkout` | ADMIN、AUDITOR |
| 到访记录查询 | `/gate` 列表 | `GET /api/records` | ADMIN、AUDITOR |
| 总览卡片 | `/stats` | `GET /api/stats/overview` | ADMIN、AUDITOR |
| 趋势图 | `/stats` | `GET /api/stats/trend?days=7\|14\|30` | ADMIN、AUDITOR |
| 部门明细 | `/stats` | `GET /api/stats/department` | 仅 ADMIN |

## 2. 状态机口径

```
0 待审核 → 1 已通过 → 2 待到访 → 3 已到访 → 4 已离开
                   ↘ 驳回/取消
```

- 入场：`verifyCode` 为 6 位数字，必须命中「status=2」且未过期的预约单；成功后写 `visitor_record.checkin_time`。
- 离场：按 `applyId` 找「status=3 且未离场」的记录，写 `checkout_time`，`stay_minutes` 由后端按两个时间戳相减得出（前端不参与计算）。
- 部门口径：AUDITOR 只能操作/查看本部门访客，越权由后端拦截返回 403，前端由 axios 拦截器统一提示。

## 3. 联调步骤（三种环境通用）

1）登录拿 token（所有角色同一套接口，密码见种子数据）：

```bash
TOKEN=$(curl -s -X POST http://127.0.0.1:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"auditor01","password":"123456"}' | grep -o '"token":"[^"]*"' | head -1 | cut -d'"' -f4)
```

2）总览与趋势（审核员应为「本部门」口径）：

```bash
curl -s -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8080/api/stats/overview
curl -s -H "Authorization: Bearer $TOKEN" 'http://127.0.0.1:8080/api/stats/trend?days=7'
```

3）入场核验（先用一条 status=2 的预约单的核验码）：

```bash
curl -s -X POST http://127.0.0.1:8080/api/records/checkin \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"verifyCode":"123456","gate":"南门"}'
```

4）离场登记：

```bash
curl -s -X POST http://127.0.0.1:8080/api/records/checkout \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"applyId":101,"gate":"南门"}'
```

5）到访记录分页查询：

```bash
curl -s -H "Authorization: Bearer $TOKEN" 'http://127.0.0.1:8080/api/records?page=1&size=10'
```

> K8s 环境把 `127.0.0.1:8080` 换成集群内 `http://g12-backend:8080`，或在跳板机上保留
> `kubectl port-forward -n ingress-nginx svc/ingress-nginx-controller 8080:80` 后走
> `-H 'Host: g12.course.local' http://127.0.0.1:8080`。

## 4. 部署说明（不改动任何 yaml）

- 本模块**没有新增** Deployment / Service / ConfigMap / Secret，也没有新增环境变量，
  因此 `k8s/01–09` 与 `docs/env-contract.md` 的口径保持不变。
- 前端改动只涉及 `views/GateCheck.vue`、`views/StatsDashboard.vue` 的注释与说明文案，
  构建产物行为不变；镜像仍为 `g12-web:v1`，无需重新封版。
- 后端 `records` / `stats` 路径沿用既有 `/api` 前缀，探针 `/api/actuator/health` 不受影响；
  改后已验证 Pod 就绪、页面可登录、三个接口返回正常。

## 5. 常见问题对照

| 现象 | 排查 |
|---|---|
| `/api/*` 全 502 | `kubectl get endpoints g12-backend -n enterprise-g12` 是否为空（selector 与 Pod label 不符） |
| 审核员看到全局数据 | 后端 `dept_id` 过滤失效或角色映射错误，检查 JWT 中 `role` 与 `deptId` |
| 部门明细 403 | 正常：`/stats/department` 仅 ADMIN 可用 |
| 入场提示「核验码无效」 | 预约单状态不是 2，或核验码已过期 |
