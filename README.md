# G12 访客预约与到访核验平台

Docker 云容器与管理技术 · 期末课程设计 · Group 12

## 目录说明

```
G12-project/
├─ frontend/          前端 Vue3 源码（A 负责）
├─ backend/           后端 SpringBoot 源码（B 负责）
├─ database/          schema.sql + seed.sql（B 负责）
├─ k8s/               全部 K8s yaml（C 01–05、08，B 06+09，A 07）
├─ evidence/          截图证据 01–07（C 采集）
├─ docs/              全部设计与记录文档
├─ compose.yaml       本地整套编排（B 负责）
├─ README.md          本文件
└─ 课程设计报告/      最终报告 + PPT + 视频
```

## 当前进度（2026-11-09）

| 目录 | 状态 | 说明 |
|---|---|---|
| `docs/` | 已有 4 份 | requirements / database-design / api / env-contract，**契约已冻结** |
| `database/` | 已完成 | schema.sql、seed.sql，待在虚拟机 MySQL 验证 |
| `frontend/` | 空 | 等 A 依 `api.md` 开页面 |
| `backend/` | 空 | B 下一步写 SpringBoot |
| `k8s/` | 空 | 等 C 写底座 01–05 |
| `evidence/` | 空 | 部署完成后采集 |

## 文档清单

**已完成**

- `docs/requirements.md` —— 权限矩阵、状态机、异常场景、需求编号、验收对照
- `docs/database-design.md` —— 6 张表逐字段、外键、索引、设计决策
- `docs/api.md` —— 27 个接口、错误码、页面↔接口对照、状态×接口速查
- `docs/env-contract.md` —— 环境变量名、Secret 清单、nginx 模板机制

**待补**

- `docs/runbook.md` —— 部署与回滚步骤（C）
- `docs/operation-record.md` —— 发布与日常运维记录（C）
- `docs/incident-record.md` —— F2 服务路由故障工单 `INC-G12-001`（C）
- `docs/business-flow-record.md` —— 完整业务流程走查记录（A）
- `docs/contribution-table.md` —— 个人分工与提交数（三人）
- `docs/ai-usage-statement.md` —— AI 使用说明（三人）

## 关键约定（改代码前先看）

- 接口数 **27**，`approve` 只做 0→1，`verify-code` 单独做 1→2，状态 1 真实落库。
- 状态机：`0→{1,9}`、`1→{2,9}`、`2→{3,9}`、`3→{4}`，4 与 9 为终态。
- 每次状态变更必须同事务写 `biz_status_change_log` + `apply_process_record`。
- 命名：Namespace `enterprise-g12`、Host `g12.course.local`、镜像 `g12-web:v1` / `g12-api:v1`、库 `ctts_g12`。
- 敏感项只走环境变量，不进代码、不进 yaml 明文。

## 打包

```bash
# Linux / macOS
zip -r G12-project.zip G12-project -x "*/node_modules/*" -x "*/.git/*"
```

Windows 直接右键 `G12-project` → 压缩为 ZIP。压缩前删掉 `node_modules`、`target`、`.git`。
