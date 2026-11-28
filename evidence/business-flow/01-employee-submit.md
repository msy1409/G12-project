# 正常链路：员工提交预约（∅ → 0）

| 项 | 内容 |
|---|---|
| 图片 | `01-employee-submit.png` |
| 所属目录 | `evidence/business-flow/` |
| 采集人 | A（前端联调，2026-11-27）；配套说明由 B 据实测补 |
| 采集时间 | 2026-11-27 13:10（Asia/Shanghai） |
| 环境 | Ubuntu 22.04 虚拟机；真实后端 Spring Boot 3.5.16（8080，context `/api`）；MySQL 8.0 库 `ctts_g12`；前端 `g12-web:v1`:80；`VITE_USE_MOCK=false` |
| 关联文档 | `docs/business-flow-record.md §1`；`evidence/business-flow/README.md` |

## 对应页面 / 接口

- 页面：`/apply/new`（我的预约 → 详情抽屉）
- 接口：`POST /api/applies`（emp01，技术研发部 dept4）

## 图上应当看得出

提交成功后详情抽屉显示预约单号 `AP20261127000016`、访客周航、状态 0（待审核），并能看到 CREATE 处理记录。

## 落库核对

| 表 | 变化 |
|---|---|
| `visitor_apply` | 新增 1 行，`status=0`，`verify_code=NULL`，`create_by=5`（emp01） |
| `apply_process_record` | 新增 `action='CREATE'` |
| `biz_status_change_log` | 新增 `from_status=-1, to_status=0` |

## 观察结论

员工替访客提交预约闭环：单据从无到有进入待审核态（0），CREATE 处理记录与 `-1→0` 状态审计同步落库。
