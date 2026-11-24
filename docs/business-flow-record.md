# 业务全流程走查记录（business-flow-record.md）

> 负责人：A（前端）　环境：`VITE_USE_MOCK=true` 或真实后端均可
> 目的：用一条预约串起「员工提交 → 审核通过 → 生成核验码 → 门岗入场 → 离场」，
> 每一步记录页面截图位置、状态码变化和对应表的新增/更新行。

## 0. 演示账号

| 角色 | 用户名 | 密码 | 入口 |
|---|---|---|---|
| 员工 | `emp01` | `123456` | `/apply/mine` |
| 审核员（技术研发部） | `auditor01` | `123456` | `/audit/list` |
| 管理员 | `admin` | `123456` | `/stats` |

## 1. 员工提交预约（∅ → 0）

页面：`/apply/new`　接口：`POST /api/applies`

- 填写访客姓名、手机号、单位、被访部门（技术研发部）、被访人、事由、预计到访/离开时间。
- 提交成功后页面显示预约单号与「待审核」状态。

**期望落库：**

| 表 | 变化 |
|---|---|
| `visitor_apply` | 新增 1 行，`status=0`，`verify_code=NULL`，`create_by=5`（emp01） |
| `apply_process_record` | 新增 1 行，`action='CREATE'` |
| `biz_status_change_log` | 新增 1 行，`from_status=-1, to_status=0` |

**核验 SQL：**

```sql
SELECT id, apply_no, visitor_name, status, verify_code, create_by
FROM visitor_apply ORDER BY id DESC LIMIT 1;

SELECT action, operator_name, operator_role, create_time
FROM apply_process_record WHERE apply_id = <新单 id> ORDER BY id;

SELECT from_status, to_status, action FROM biz_status_change_log
WHERE apply_id = <新单 id> ORDER BY id;
```

**截图位置：** `evidence/business-flow/01-employee-submit.png`（待补）

## 2. 审核通过（0 → 1）

页面：`/audit/list`（待审核）或 `/audit/{id}`　接口：`PUT /api/applies/{id}/approve`

- 审核员在待审核列表点「通过」，确认后状态变为「审核通过」。
- 契约规定：approve 只做 0→1，不生成核验码、不跳状态。

**期望落库：**

| 表 | 变化 |
|---|---|
| `visitor_apply` | `status: 0 → 1`，`verify_code` 仍为 NULL |
| `apply_process_record` | 新增 `action='APPROVE'` |
| `biz_status_change_log` | 新增 `from_status=0, to_status=1` |

**异常验证：** 用 `auditor02`（市场部）访问技术研发部的单，接口返回 `403 DEPT_SCOPE_DENIED`，页面弹出「只能操作本部门的数据」。

**截图位置：** `evidence/business-flow/02-audit-approve.png`（待补）

## 3. 生成核验码（1 → 2）

页面：`/audit/{id}`　接口：`PUT /api/applies/{id}/verify-code`

- 审核员点「生成核验码」，弹窗展示 6 位数字码（页面用大字号展示，可复制）。
- 再次点击会返回 `400 STATE_NOT_ALLOWED`，证明该动作只在状态 1 可用。

**期望落库：**

| 表 | 变化 |
|---|---|
| `visitor_apply` | `status: 1 → 2`，`verify_code='483920'`（示例） |
| `apply_process_record` | 新增 `action='VERIFY_CODE'`，remark 含核验码 |
| `biz_status_change_log` | 新增 `from_status=1, to_status=2` |

**截图位置：** `evidence/business-flow/03-verify-code.png`（待补）

## 4. 门岗入场（2 → 3）

页面：`/gate`　接口：`POST /api/records/checkin`

- 门岗输入 6 位核验码，选择门岗（东门-01），点「核验入场」。
- 成功后页面显示访客姓名与入场时间；核验码作废，记录表出现一条未离场记录。

**期望落库：**

| 表 | 变化 |
|---|---|
| `visitor_record` | 新增 1 行，`checkin_time=now`，`checkout_time=NULL`，`gate='东门-01'` |
| `visitor_apply` | `status: 2 → 3` |
| `apply_process_record` | 新增 `action='CHECKIN'` |
| `biz_status_change_log` | 新增 `from_status=2, to_status=3` |

**异常验证：**

| 操作 | 期望 |
|---|---|
| 同一核验码再入场 | `409 DUPLICATE_CHECKIN` |
| 输入不存在的码 | `400 VERIFY_CODE_INVALID` |
| 已离场（4）再入场 | `400 STATE_NOT_ALLOWED` |

**截图位置：** `evidence/business-flow/04-gate-checkin.png`（待补）

## 5. 离场登记（3 → 4）

页面：`/gate`　接口：`POST /api/records/checkout`

- 在「离场登记」输入预约 ID（页面提供「当前在场」快捷选择），点「登记离场」。
- 页面显示离场时间与停留时长，到访记录的状态变为「已离开」。

**期望落库：**

| 表 | 变化 |
|---|---|
| `visitor_record` | 更新 `checkout_time=now`、`checkout_by`、`gate` |
| `visitor_apply` | `status: 3 → 4`（终态） |
| `apply_process_record` | 新增 `action='CHECKOUT'` |
| `biz_status_change_log` | 新增 `from_status=3, to_status=4` |

**异常验证：** 未入场直接离场 → `400 NOT_CHECKED_IN`；已离场再离场 → `400 STATE_NOT_ALLOWED`。

**截图位置：** `evidence/business-flow/05-gate-checkout.png`（待补）

## 6. 终态核对

```sql
SELECT a.id, a.apply_no, a.status, a.verify_code,
       r.checkin_time, r.checkout_time,
       TIMESTAMPDIFF(MINUTE, r.checkin_time, r.checkout_time) AS stay_minutes
FROM visitor_apply a
LEFT JOIN visitor_record r ON r.apply_id = a.id
WHERE a.id = <预约 id>;

SELECT from_status, to_status, action, operator_name, create_time
FROM biz_status_change_log WHERE apply_id = <预约 id> ORDER BY id;

SELECT action, operator_name, operator_role, remark, create_time
FROM apply_process_record WHERE apply_id = <预约 id> ORDER BY id;
```

一条完整链路应有 5 条处理记录、5 条状态审计（CREATE / APPROVE / VERIFY_CODE / CHECKIN / CHECKOUT），
`visitor_apply.status=4`，`visitor_record` 有入场和离场两个时间。

## 7. 异常链路（答辩备用）

| 场景 | 页面操作 | 状态变化 | 证据 |
|---|---|---|---|
| 驳回 | 审核列表点「驳回」，必填原因 | 0 → 9（REJECT） | 待补 |
| 员工取消 | 我的预约点「取消」 | 0/1/2 → 9（CANCEL） | 待补 |
| 越权审核 | 用别的部门审核员打开详情 | HTTP 403 | 待补 |
| 非法流转 | 已离场再入场 | HTTP 400 `STATE_NOT_ALLOWED` | 待补 |

> 截图统一放 `evidence/business-flow/`，文件名与上文一致；补图后在每张图旁标注采集时间和登录角色。
