# 业务流程走查证据（business-flow）

采集时间：2026-11-27 13:10（Asia/Shanghai）
环境：Ubuntu 22.04 虚拟机；真实后端 Spring Boot 3.5.16（8080，context `/api`）；
MySQL 8.0（host 网络，库 `ctts_g12`）；前端镜像 `g12-web:v1` 跑在 80 端口；
`VITE_USE_MOCK=false`，请求经 nginx `/api` 反代到真实后端。

测试数据：

| 项 | 值 |
|---|---|
| 预约 ID | 16 |
| 预约单号 | `AP20261127000016` |
| 访客 | 周航 / 13900000008 / 外部供应商 |
| 被访部门 | 技术研发部（dept_id=4） |
| 被访人 | 刘洋 |
| 核验码 | `288988` |
| 最终状态 | 4（已离开） |

## 截图清单

| 文件 | 页面 | 操作 | 状态 | 结果 |
|---|---|---|---|---|
| `01-employee-submit.png` | 我的预约 → 详情抽屉 | emp01 创建预约 | 0 待审核 | `visitor_apply` 新增 1 行，CREATE 记录 + `-1→0` 审计 |
| `02-audit-approve.png` | 审核详情 `/audit/16` | auditor01 点「审核通过」并确认 | 1 审核通过 | `status=1`，APPROVE 记录 + `0→1` 审计 |
| `03-verify-code.png` | 审核详情 → 核验码弹窗 | auditor01 点「生成核验码」 | 2 待到访 | `verify_code=288988`，VERIFY_CODE 记录 + `1→2` 审计 |
| `04-gate-checkin.png` | 门岗 `/gate` | 输入 288988，东门-01，核验入场 | 3 已到访 | `visitor_record` 新增，`checkin_time=2026-11-27 13:10:41` |
| `05-gate-checkout.png` | 门岗 `/gate` | 预约 ID 16，登记离场 | 4 已离开 | `checkout_time=2026-11-27 13:10:47`，CHECKOUT 记录 + `3→4` 审计 |

> 入场和离场在同一分钟内完成，所以页面显示停留 0 分钟；这是真实数据，没有改库。

## 核对 SQL

```sql
USE ctts_g12;

SELECT id, apply_no, visitor_name, status, verify_code, dept_id
FROM visitor_apply WHERE id = 16;

SELECT id, apply_id, checkin_time, checkout_time,
       TIMESTAMPDIFF(MINUTE, checkin_time, checkout_time) AS stay_minutes
FROM visitor_record WHERE apply_id = 16;

SELECT action, operator_name, operator_role, remark, create_time
FROM apply_process_record WHERE apply_id = 16 ORDER BY id;

SELECT from_status, to_status, action, operator_name, create_time
FROM biz_status_change_log WHERE apply_id = 16 ORDER BY id;
```

实际结果：`status=4`，核验码 `288988`，处理记录 5 条（CREATE / APPROVE / VERIFY_CODE / CHECKIN / CHECKOUT），
状态审计 5 条（-1→0 → 0→1 → 1→2 → 2→3 → 3→4）。
