# 正常链路：离场登记（3 → 4）

| 项 | 内容 |
|---|---|
| 图片 | `05-gate-checkout.png` |
| 所属目录 | `evidence/business-flow/` |
| 采集人 | A（前端联调，2026-11-27）；配套说明由 B 据实测补 |
| 采集时间 | 2026-11-27 13:10（Asia/Shanghai） |
| 环境 | Ubuntu 22.04 虚拟机；真实后端 Spring Boot 3.5.16（8080，context `/api`）；MySQL 8.0 库 `ctts_g12`；前端 `g12-web:v1`:80；`VITE_USE_MOCK=false` |
| 关联文档 | `docs/business-flow-record.md §5`；`evidence/business-flow/README.md` |

## 对应页面 / 接口

- 页面：`/gate`（门岗核验 → 离场登记）
- 接口：`POST /api/records/checkout`（预约 ID 16）

## 图上应当看得出

在离场登记输入预约 ID 后点「登记离场」，页面显示离场时间 `2026-11-27 13:10:47` 与停留时长；到访记录补齐离场时间，单据进入终态「已离开」。

## 落库核对

| 表 | 变化 |
|---|---|
| `visitor_record` | 更新 `checkout_time=2026-11-27 13:10:47`、`checkout_by`、`gate` |
| `visitor_apply` | `status: 3 → 4`（终态） |
| `apply_process_record` | 新增 `action='CHECKOUT'` |
| `biz_status_change_log` | 新增 `from_status=3, to_status=4` |

## 观察结论

离场登记分支闭环：仅在场（3）且存在未离场记录的单据可离场，单据转为已离开（4）终态；未入场直接离场 `400 NOT_CHECKED_IN`、已离场再离场 `400 STATE_NOT_ALLOWED`。本次入场离场同分钟完成，停留 0 分钟为真实结果，未改库。
