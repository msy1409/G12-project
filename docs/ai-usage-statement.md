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

- **用的 AI**：DeepSeek V4.1 Flash（OpenCode Agent，包含终端执行与浏览器自动化）
- **让 AI 做的**：Vue 3 + Vite 工程骨架；9 个页面（Login / ApplyNew / MyApplies / AuditList / AuditDetail / GateCheck / AdminUsers / AdminDepts / StatsDashboard）；5 个复用组件（StatusTag / PageHeader / DataTable / ConfirmDialog / PermissionButton）和 2 个图表组件；`src/api/request.js` 的 axios 封装与 401/403 拦截；`meta.roles` 路由守卫与按角色菜单；按 `docs/api.md` 实现的 27 个接口 mock 适配器；`nginx.conf.template`、`Dockerfile`、`Dockerfile.mock`；`k8s/07-frontend.yaml`、`k8s/08-ingress.yaml`；`docs/business-flow-record.md` 骨架
- **我自己改的 / 验收确认的**：把镜像构建阶段从 `node:20` 调整到 `node:22`（Element Plus 2.14 依赖的 `@vueuse/core@15` 要求 Node ≥22）；构建参数增加 `NODE_OPTIONS=--dns-result-order=ipv4first`，解决容器 DNS 只返回 IPv6 导致 `npm ci` 中途崩溃；按 B 的 `06-backend.yaml` Service 端口把前端 `BACKEND_PORT` 对齐为 8080（C 已复核）；去掉接口契约中没有的部门“启用”按钮，只保留停用；用真实浏览器验收员工端、管理员端页面，并切换到真实后端跑通预约全流程
- **能讲清楚的**：路由守卫与三种角色的菜单隔离；axios 请求/响应拦截器与 token 失效跳转；0/1/2/3/4/9 状态在页面和 `StatusTag` 上的表现；mock 适配器与真接口的切换方式；nginx `/api` 反向代理与 `BACKEND_HOST` / `BACKEND_PORT` 注入；Docker 多阶段构建；K8s 前端 Deployment / Service / Ingress 与探针
