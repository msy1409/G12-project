# AI 使用情况声明（ai-usage-statement.md）

> 负责人：B（后端）建表　填写：A / B / C 各自补自己那段　版本：v1.0
> 按手册 29 / 36 / 47 条：如实记录用了什么 AI、让它生成了哪些文件、自己改了哪些、哪部分能讲清楚。

## B（后端）

- **用的 AI**：DeepSeek（DSH 终端 Agent）、豆包助手
- **让 AI 做的**：Spring Boot 工程骨架与分层结构（controller / service / mapper / entity / dto / vo / common / config）初稿；MyBatis-Plus 实体、Mapper、VO 的样板代码；多阶段 Dockerfile 初稿；排障思路（Maven 仓库源不通、seed 密文不匹配、接口路径与契约不符、healthcheck 冷启动超时）
- **我自己改的**：表结构与 `docs/api.md` 契约对齐；Service 端口 `8080→8080`、resources `200m/512Mi`、limits `500m/1Gi`、双探针路径等部署口径确认；预约状态流转 0→1→2→3→4 与非法流转拦截、三角色权限隔离在本机 MySQL 自测通过；重生成 seed 的 BCrypt 密文；修复 `AuditController` 审核接口路径与契约不符、补 `PUT /users/{id}/status` 停用启用接口；按修复后代码重出 v1/v2 镜像
- **能讲清楚的**：JWT 签发校验与角色拦截器鉴权；状态机各节点的合法/非法流转与审计留痕；HPA 为什么依赖 `requests.cpu`；v1/v2 镜像差异（仅 `apiVersion` 字段赋值）；compose 健康检查与依赖等待逻辑

## C（K8s 底座与运维）

- **用的 AI**：DeepSeek（DSH 终端 Agent）
- **让它做的**：k8s 底座 5 个 yaml 的初稿与注释、runbook / operation-record / incident-record 的骨架、evidence 36 份截图模板；以及排障思路（PVC Pending、initdb 不执行、selector 与 Endpoints）
- **我自己改的**：StorageClass 与 PVC 的 volumeName、Service 端口 80/8080 的实际值、所有命令都在自己 VM 上跑过并按真实输出回填
- **能讲清楚的**：Endpoints 是 selector 算出来的、Recreate 与 RWO 的关系、rollout undo 是退一层不是退到 v1、HPA 依赖 requests.cpu

> 注：本段为 C 的占位素材，commit 数与实际改动由 C 本人 clone 最新代码后核对填写。

## A（前端）

- **用的 AI**：<待 A 填>
- **让它做的**：<待 A 填：前端页面、nginx 模板、前端镜像等>
- **我自己改的**：<待 A 填>
- **能讲清楚的**：<待 A 填>
