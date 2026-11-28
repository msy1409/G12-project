# 正常链路：门岗入场（2 → 3）

| 项 | 内容 |
|---|---|
| 图片 | `04-gate-checkin.png` |
| 所属目录 | `evidence/business-flow/` |
| 采集人 | A（前端联调，2026-11-27）；配套说明由 B 据实测补 |
| 采集时间 | 2026-11-27 13:10（Asia/Shanghai） |
| 环境 | Ubuntu 22.04 虚拟机；真实后端 Spring Boot 3.5.16（8080，context `/api`）；MySQL 8.0 库 `ctts_g12`；前端 `g12-web:v1`:80；`VITE_USE_MOCK=false` |
| 关联文档 | `docs/business-flow-record.md §4`；`evidence/business-flow/README.md` |

## 对应页面 / 接口

- 页面：`/gate`（门岗核验 → 入场登记）
- 接口：`POST /api/records/checkin`（核验码 `288988`，门岗 东门-01）

## 图上应当看得出

输入核验码、选择东门-01 后点「核验入场」，页面显示「周航 入场成功·2026-11-27 13:10:41」；核验码随即作废，到访记录表出现一条未离场记录。

## 落库核对

| 表 | 变化 |
|---|---|
| `visitor_record` | 新增 1 行，`checkin_time=2026-11-27 13:10:41`，`checkout_time=NULL`，`gate='东门-01'` |
| `visitor_apply` | `status: 2 → 3` |
| `apply_process_record` | 新增 `action='CHECKIN'` |
| `biz_status_change_log` | 新增 `from_status=2, to_status=3` |

## 观察结论

入场登记分支闭环：核验码匹配且部门权限通过后写入到访记录，单据转为已到访（3）；同一码重复入场 `409 DUPLICATE_CHECKIN`、错码 `400 VERIFY_CODE_INVALID`、已离场再入场 `400 STATE_NOT_ALLOWED`。
