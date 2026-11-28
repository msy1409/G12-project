# 答辩演示脚本（demo-script.md）

> 适用：G12 访客预约与到访核验平台 · 小组现场演示 + 可选演示视频。
> 依据：《02-Docker-期末考查方案》与课程纪要——现场小组演示约 2 分钟；录屏视频为可选留档，课程纪要明确期中报告无需录屏，未强制交付视频。
> 环境：k3s v1.28.2+k3s1 单节点，namespace `enterprise-g12`；业务 Pod 5 个 Running；库 `ctts_g12`，种子数据已由 `schema.sql` + `seed.sql` 重灌。

## 0. 演示前自检（1 分钟内完成，不在正式演示里讲）

```bash
kubectl get pods,svc,pvc,hpa -n enterprise-g12
kubectl get ingress -n enterprise-g12
```

- 5 个业务 Pod 全部 `Running`；`g12-mysql` / `g12-frontend` / `g12-backend` 三个 Service 存在；MySQL PVC `Bound`；HPA `g12-backend-hpa` minReplicas 2 / maxReplicas 4。
- 浏览器可打开 `g12.course.local`（本机 hosts 已解析）；不通则 `kubectl port-forward` 后走本机端口访问。
- 演示账号密码统一 `123456`：`admin`（管理员）、`auditor01`（审核员·技术研发部）、`emp01`（员工）。

## 1. 现场演示分镜（约 2 分钟）

| 时段 | 画面 / 命令 | 讲稿要点 | 对应证据 |
|---|---|---|---|
| 0:00–0:30 集群概览 | 终端执行 `kubectl get pods,svc,pvc,hpa -n enterprise-g12`，再 `kubectl get ingress` | 单节点 k3s 跑起整套：前端、后端两副本、MySQL；Service 集群内互通；MySQL 用 RWO PVC 持久化；后端挂 HPA，目标 CPU 60%；Ingress 按域名 `g12.course.local` 路由到前端 | `evidence/03-k8s-deploy/05-get-all.md`、`09-ingress-route.md`、`03-pvc-bound.md` |
| 0:30–1:00 登录与角色边界 | 浏览器 `admin` 登录，看审核列表与统计总览；切到 `auditor01` 只显示本部门单 | 三种角色：员工发起/取消本人预约，审核员本部门审核与门岗核验，管理员管用户部门与全局统计；菜单隐藏只是体验，路由守卫 + 后端 `@RequireRole` + 部门校验才是边界；跨部门访问返回 403 | `evidence/business-flow/03-dept-scope-403.md`、PPT 第 3 页 |
| 1:00–1:40 业务主流程 | 按 员工提交 → 审核通过 → 生成核验码 → 凭码入场 → 登记离场 走一条单；页面状态 0→1→2→3→4 | 预约状态机受控：`approve` 只做 0→1，核验码单独 1→2；非法流转后端返回 400 `STATE_NOT_ALLOWED`；每次变更同事务写 `biz_status_change_log` + `apply_process_record` | `evidence/business-flow/01-employee-submit` … `05-gate-checkout`；`docs/business-flow-record.md` |
| 1:40–2:00 运维与证据收口 | 切到终端或直接指证据目录：发布 v1→v2、异常发布回滚、手动扩缩容、HPA、F2 故障复盘 | 镜像多阶段构建；后端 v1→v2 用 `kubectl set image` + `rollout` 观察；异常发布后 `rollout undo` 回滚；HPA 压测观察扩容；F2（Service selector 与 Pod 标签不一致、Endpoints 为空）已按七段复盘结案 | `evidence/05-release-rollback/`、`06-hpa/`、`07-incident/` |

## 2. 可选演示视频分镜（约 5 分钟，非强制）

> 若后续需要留档视频，按下面顺序录屏；每段先稳定画面再操作，命令输出停留到能看清。课程不强制，答辩以现场 2 分钟演示为准。

| 段 | 时长 | 画面 | 旁白要点 |
|---|---|---|---|
| ① 项目与架构 | 0:00–0:40 | PPT 封面 + 架构图；浏览器首页 | Vue3 + Element Plus 前端，Spring Boot 后端，MySQL 6 张表；28 个业务接口；前后端经 Nginx / Ingress 反代，JWT 鉴权 |
| ② 集群与部署 | 0:40–1:40 | `kubectl get nodes,pods,svc,pvc,hpa,ingress`；`docker images` / 节点 containerd 镜像清单 | k8s 资源齐全：Namespace、ConfigMap、Secret 结构、PV/PVC、Deployment/Service、Ingress、HPA；镜像 `g12-web:v1`、`g12-api:v1/v2`，版本标签非 latest |
| ③ 业务全流程 | 1:40–3:10 | 浏览器走 5 步链路，穿插详情抽屉里的处理记录与状态审计 | 三角色权限隔离；状态机 0/1/2/3/4/9；核验码 6 位；越权 403、非法流转 400 |
| ④ 发布回滚与扩缩容 | 3:10–4:10 | `rollout history`、v1→v2、异常发布 v999、`rollout undo`；`kubectl scale` 3→2；`get hpa` / `top pods` | 发布可观察、可回滚；手动扩缩容即时生效；HPA 基于 metrics-server 的 CPU 指标，requests.cpu 是利用率计算前提 |
| ⑤ 故障复盘收口 | 4:10–5:00 | F2 现象 502 → Endpoints 为空 → selector/标签不一致 → 最小修复 → Endpoints 恢复；最后回到首页与 PPT 总结页 | 排查路径：现象 → 状态检查 → 日志/Events 证据 → 根因 → 最小修复 → 恢复验证 → 预防措施；事件编号 `INC-G12-001` |

## 3. 常见追问索引（口头追问备用）

- **后端怎么鉴权 / 权限隔离**：JWT 登录发 token，请求带 Bearer；拦截器校验有效期，Controller 角色注解 + Service 部门范围校验；401 清态跳登录，403 统一提示。
- **状态机与一致性**：状态流转集中在后端事务内，同时更新预约单、写处理记录与状态审计日志；前端只做展示与提交。
- **镜像怎么构建**：前端 Node 22 Alpine 构建 → Nginx 1.27 Alpine 运行；后端 Maven 构建 → JRE 运行；环境变量只换值，不写死口令/密钥。
- **HPA 为什么不扩容**：先区分负载不足、metrics-server 异常、HPA 配置错误、节点资源不足；`requests.cpu` 是 CPU 利用率的计算基准。
- **持久化**：MySQL Deployment 1 副本 + RWO PVC + `strategy: Recreate`；删容器重建后数据仍在。
- **故障 F2**：Service selector 与 Pod 标签不一致导致 Endpoints 为空、502；对齐标签后 Endpoints 自动恢复。

## 4. 演示口径注意

- 不在演示或截图里出现真实密码、Token；Secret 真值只在部署现场敲，仓库只留 `secret.example.yaml`。
- 业务数据均为种子模拟数据；历史联调记录（单号、访客名、核验码）见 `docs/business-flow-record.md` 与 `evidence/business-flow/`，答辩描述以这些留痕为准。
- 命令与输出原文以 `evidence/_raw/` 为准；被追问具体细节时直接指到对应文件。
