# 正常链路：生成核验码（1 → 2）

| 项 | 内容 |
|---|---|
| 图片 | `03-verify-code.png` |
| 所属目录 | `evidence/business-flow/` |
| 采集人 | A（前端联调，2026-11-27）；配套说明由 B 据实测补 |
| 采集时间 | 2026-11-27 13:10（Asia/Shanghai） |
| 环境 | Ubuntu 22.04 虚拟机；真实后端 Spring Boot 3.5.16（8080，context `/api`）；MySQL 8.0 库 `ctts_g12`；前端 `g12-web:v1`:80；`VITE_USE_MOCK=false` |
| 关联文档 | `docs/business-flow-record.md §3`；`evidence/business-flow/README.md` |

## 对应页面 / 接口

- 页面：`/audit/16`（核验码弹窗）
- 接口：`PUT /api/applies/16/verify-code`（auditor01）

## 图上应当看得出

弹窗以大字号展示 6 位核验码 `288988`；单据进入待到访态（状态 2），等待门岗核验入场。

## 落库核对

| 表 | 变化 |
|---|---|
| `visitor_apply` | `status: 1 → 2`，`verify_code='288988'` |
| `apply_process_record` | 新增 `action='VERIFY_CODE'`，remark 含核验码 |
| `biz_status_change_log` | 新增 `from_status=1, to_status=2` |

## 观察结论

核验码生成分支闭环：仅状态 1 可生成，6 位码入库并绑定单据；重复调用返回 `400 STATE_NOT_ALLOWED`。
