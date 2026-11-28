# 正常链路：审核通过（0 → 1）

| 项 | 内容 |
|---|---|
| 图片 | `02-audit-approve.png` |
| 所属目录 | `evidence/business-flow/` |
| 采集人 | A（前端联调，2026-11-27）；配套说明由 B 据实测补 |
| 采集时间 | 2026-11-27 13:10（Asia/Shanghai） |
| 环境 | Ubuntu 22.04 虚拟机；真实后端 Spring Boot 3.5.16（8080，context `/api`）；MySQL 8.0 库 `ctts_g12`；前端 `g12-web:v1`:80；`VITE_USE_MOCK=false` |
| 关联文档 | `docs/business-flow-record.md §2`；`evidence/business-flow/README.md` |

## 对应页面 / 接口

- 页面：`/audit/16`（审核详情）
- 接口：`PUT /api/applies/16/approve`（auditor01，技术研发部审核员）

## 图上应当看得出

页面顶部提示「已审核通过，接下来请生成核验码」；步骤条走到「审核通过 状态 1」，访客周航、外部供应商、被访人刘洋、技术研发部等信息完整。

## 落库核对

| 表 | 变化 |
|---|---|
| `visitor_apply` | `status: 0 → 1`，`verify_code` 仍为 NULL |
| `apply_process_record` | 新增 `action='APPROVE'` |
| `biz_status_change_log` | 新增 `from_status=0, to_status=1` |

## 观察结论

审核通过分支闭环：approve 只做 0→1，不生成核验码、不跳状态；APPROVE 记录与 `0→1` 审计落库。跨部门审核员访问本单被 `403 DEPT_SCOPE_DENIED` 拦截（见 `03-dept-scope-403.md`）。
