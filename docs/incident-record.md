# 故障复盘记录 · INC-G12-001

| 项 | 内容 |
|---|---|
| 编号 | `INC-G12-001` |
| 标题 | 前端所有 `/api` 请求返回 502 —— `g12-backend` Service 的 selector 与 Pod 标签不一致 |
| 故障类型 | F2（配置类故障，人为注入演练） |
| 影响范围 | 登录、9 个业务页面的全部接口调用；**静态页面仍能打开**（前端 Pod 正常），MySQL 与数据无影响 |
| 环境 | k3s v1.28.2+k3s1 单节点 / namespace `enterprise-g12` / Ingress `g12.course.local` → `g12-frontend` → `g12-backend` |
| 注入方式 | `kubectl patch svc g12-backend -p '{"spec":{"selector":{"app":"wrong-label"}}}'` |
| 发现方式 | 演示中打开页面，接口 502；再看 Endpoints 为空 |
| 严重级别 | 高（业务不可用，页面可打开但无法登录，用户感知为“能进但什么都点不动”） |
| 记录人 / 时间 | B（k3s 兜底环境代采） / 2026-11-28 17:46（Asia/Shanghai） |

---

## 1. 现象

浏览器打开 `http://g12.course.local:8080`，**页面能加载**（HTML/CSS/JS 都有），但登录按钮点下去转圈，Network 面板里所有 `/api/auth/login`、`/api/applies/mine` 请求都是：

```
502 Bad Gateway
```

nginx（前端容器）访问日志同时出现：

```
[error] ... connect() failed (111: Connection refused) while connecting to upstream
```

**第一判断**：502 是**网关能收到请求、但连不上上游**，不是 404（路径错）也不是 504（上游超时）。所以先把“后端到底有没有被找到”作为首要方向。

经 Ingress 实测：`POST /api/auth/login` 与 `GET /api/actuator/health` 均返回 **HTTP 502**；前端 nginx 日志留下上游拒绝记录（见 `evidence/07-incident/01-symptom-502.md`，raw：`evidence/_raw/07-incident/01-symptom-frontend-logs.txt`）：

```text
"POST /api/auth/login HTTP/1.1" 502 157
[error] connect() failed (111: Connection refused) while connecting to upstream,
  upstream: "http://10.43.180.101:8080/api/auth/login", host: "g12.course.local"
"GET /api/actuator/health HTTP/1.1" 502 157
```

> 说明：502 是 nginx 给的。nginx 在 `frontend` 容器里，它只认 `proxy_pass http://g12-backend:8080`；它连不上 `g12-backend` 这个**域名（=Service）**，才会 502。所以问题在 Service→Pod 这一段，不在 nginx，也不在后端代码。

---

## 2. 状态检查（缩小范围）

按“从外往里”四层查：Ingress → Service/Endpoints → Pod → 容器端口。

```bash
# 2.1 外层：Ingress 有没有地址、后端 Service 名字对不对
kubectl get ingress -n enterprise-g12
kubectl describe ingress g12-ingress -n enterprise-g12 | tail -20

# 2.2 关键一层：Service 的 Endpoints
kubectl get endpoints g12-backend -n enterprise-g12
kubectl get endpointslices -n enterprise-g12 -l kubernetes.io/service-name=g12-backend

# 2.3 Pod 到底在不在、标签是什么
kubectl get pods -n enterprise-g12 -o wide --show-labels

# 2.4 Service 期望选中谁
kubectl get svc g12-backend -n enterprise-g12 -o yaml | sed -n '/selector/,+4p'
```

**状态判据表**

| 层 | 正常 | 本次实际 | 结论 |
|---|---|---|---|
| Ingress | ADDRESS 有值，BACKEND 是 `g12-frontend:80` | 正常：ADDRESS `10.0.2.15`，host `g12.course.local` | 入口链路正常 |
| Endpoints | 有 1–2 个 `IP:8080` | **空（`<none>`）** | **断点在这里** |
| Pod | `g12-backend-*` 2 个 Running | 2 个 `1/1 Running`，`RESTARTS=0` | Pod 本身健康 |
| Service selector | `app: g12-backend` | `app: wrong-label` | **与 Pod 标签不一致** |

```bash
# 期望看到（正常时）
NAME          ENDPOINTS           AGE
g12-backend   10.244.0.15:8080    35m

# 本次实际
NAME          ENDPOINTS   AGE
g12-backend   <none>      35m
```

真实输出见 `evidence/07-incident/02-endpoints-empty.md`（raw：`evidence/_raw/07-incident/02-endpoints-empty.txt`）：Endpoints `<none>`；后端两 Pod 标签 `app=g12-backend`；Service selector `app: wrong-label`。

```text
NAME          ENDPOINTS   AGE
g12-backend   <none>      5h17m

selector:
  app: wrong-label
```

> **判据**：`Endpoints` 为空 + Pod 标签正常 + Service selector 与标签不符 → 故障在 **Service 的 selector**，与后端应用、镜像、探针都无关。**这是 30 秒内定位的关键一条**：Endpoints 是 K8s 由 selector 算出来的，它空 = 没有任何 Pod 被选中。

---

## 3. 日志 / Events 证据

```bash
# 3.1 前端（nginx）日志：证明是它连不上上游
kubectl logs -l app=g12-frontend -n enterprise-g12 --timestamps --tail=40
kubectl logs -l app=g12-frontend -n enterprise-g12 --previous --tail=20 2>&1 | head -5

# 3.2 后端日志：证明后端自己没崩（还能正常打日志 = JVM 活着）
kubectl logs -l app=g12-backend -n enterprise-g12 --timestamps --tail=30

# 3.3 Endpoints 的细节：为什么是空的
kubectl describe endpoints g12-backend -n enterprise-g12
kubectl describe svc g12-backend -n enterprise-g12

# 3.4 事件流：按时间看最后一次改动
kubectl get events -n enterprise-g12 --sort-by=.lastTimestamp | tail -25
```

- 前端 nginx `connect() failed ... while connecting to upstream` 段（带时间戳）：`evidence/07-incident/01-symptom-502.md`，raw `_raw/07-incident/01-symptom-frontend-logs.txt`。
- 后端日志尾部：时间戳在故障期间连续，Spring Boot `Started`、HikariPool 已连上 MySQL，进程未重启；见 `evidence/07-incident/04-pods-labels.md`，raw `_raw/07-incident/04-backend-logs.txt`。
- `describe endpoints`：`Subsets:` 下无 Addresses/Ports 子集；`describe svc`：`Selector: app=wrong-label`、`Endpoints: <none>`；见 `evidence/07-incident/03-describe-svc.md`，raw `_raw/07-incident/03-describe-svc.txt`、`03-describe-endpoints.txt`。
- events 尾部：`evidence/_raw/07-incident/07-events.txt`（无 FailedScheduling / FailedMount / 驱逐事件）。

**排除法结论**

| 假设 | 证据 | 结论 |
|---|---|---|
| 后端 Pod 挂了 | `get pods` 两个 Running，RESTARTS=0 | 排除 |
| 探针失败导致不 Ready | Pod READY 列是 `1/1`，describe 无 `Readiness probe failed` | 排除 |
| 后端端口错了 | `describe pod` 里 `Port: 8080/TCP`，且故障前后端正常过 | 排除 |
| 前端 nginx 模板渲染错 | 前端日志里上游写的是 `g12-backend:8080`，正是期望值 | 排除 |
| **Service selector 不匹配 Pod 标签** | Endpoints `<none>`，selector=`wrong-label`，Pod 标签=`app=g12-backend` | **确认** |

> `--previous` 日志说明：本次故障中容器**没有重启**（RESTARTS=0），所以 `--previous` 只会返回 `previous terminated container ... not found`。这不是“没证据”，而是“证明没重启”的证据，记录里要写清这句，别只截一张报错图。

---

## 4. 根因

`g12-backend` 这个 Service 的 `spec.selector` 被改成 `app: wrong-label`，而 Deployment 生成的 Pod 标签是 `app: g12-backend`。

K8s 的 Service **不直接记 Pod 列表**，它的 Endpoints 由 **Service.selector 与 Pod.labels 做集合运算**实时算出：selector 不匹配 → 选中集合为空 → Endpoints 为空 → kube-proxy 在这条 Service 的 iptables/IPVS 规则里没有任何后端 → 发往 ClusterIP 的包被拒绝（RST）→ nginx 收到连接拒绝 → 返回 **502**。

**为什么 502 而不是 404/超时**：域名能解析（Service 的 ClusterIP 还在）、连接被立刻拒绝（不是丢包），所以 nginx 立刻报 502。若 selector 匹配但端口写错（targetPort），现象会是 502/503 且 Endpoints 有值；若 Ingress host 配错，会是 404 或默认后端；若后端卡死，会是 504。**这四种现象的区别就是这次要能讲清的点**：

| 现象 | Endpoints | 第一嫌疑 |
|---|---|---|
| 502，立即返回 | 空 | **Service selector 不匹配**（本次） |
| 502/503，Endpoints 有值 | 有 | targetPort 与容器端口不一致 |
| 404（Ingress 默认后端） | 有 | Ingress host/path 配错 |
| 504 | 有 | 后端慢/卡（DB 连接、GC） |

**人为注入复现（F2 命令，留档用）**

```bash
kubectl patch svc g12-backend -n enterprise-g12 -p '{"spec":{"selector":{"app":"wrong-label"}}}'
kubectl get endpoints g12-backend -n enterprise-g12     # 立刻变 <none>
```

注入返回 `service/g12-backend patched`，随后 `kubectl get endpoints g12-backend` 立刻变为 `<none>`（见 `evidence/07-incident/02-endpoints-empty.md`）。

---

## 5. 最小修复

**不重启、不重建、不重新发布镜像**——只把 selector 改回正确值。Service 的 `selector` 字段是可变的，patch 一次即生效，Endpoints 由控制器自动重算。

```bash
kubectl patch svc g12-backend -n enterprise-g12 -p '{"spec":{"selector":{"app":"g12-backend"}}}'
kubectl get endpoints g12-backend -n enterprise-g12     # 期望 1–2 秒后重新出现 IP:8080
```

patch 返回 `service/g12-backend patched`，Endpoints 1–2 秒后重新出现 `10.42.0.173:8080,10.42.0.174:8080`（见 `evidence/07-incident/05-fix-endpoints-back.md`，raw `_raw/07-incident/05-fix-endpoints-back.txt`）。

> **为什么不用 `kubectl apply -f k8s/06-backend.yaml` 覆盖**：也能修好，但那会把 B 的整个 Deployment 一起动到（可能触发滚动重启），排障原则是**改动面最小**。同理不要 `kubectl delete pod`——Pod 一直是对的，删了纯属浪费 40 秒。
> **如果 `get endpoints` 还是空**：`kubectl get pods --show-labels` 看 Pod 标签是否被改过（比如有人给 Deployment 模板改了 label）；还有一种情况是改了 `targetPort`，那时 Endpoints 有值但端口不对。

---

## 6. 恢复验证（四层都要过）

```bash
# 1) Endpoints 恢复
kubectl get endpoints g12-backend -n enterprise-g12

# 2) 集群内直连后端（绕开前端，先证明 Service 通了）
#    Service 端口是 8080（B 定的 port 8080 → targetPort 8080），必须带 :8080
kubectl run probe-check --rm -it --image=busybox:1.36 -n enterprise-g12 -- \
  wget -qO- http://g12-backend:8080/api/actuator/health
# 期望 {"status":"UP"}

# 3) 经前端反代（证明 nginx → Service 这段也通了）
kubectl run probe-check --rm -it --image=busybox:1.36 -n enterprise-g12 -- \
  wget -qO- http://g12-frontend/api/actuator/health
# 期望 {"status":"UP"}

# 4) 经 Ingress（带 Host 头，等价浏览器访问）
kubectl run probe-check --rm -it --image=busybox:1.36 -n enterprise-g12 -- \
  wget -qO- --header='Host: g12.course.local' http://ingress-nginx-controller.ingress-nginx.svc/api/actuator/health
# 期望 {"status":"UP"}
```

**浏览器**：刷新登录页 → 用 `seed.sql` 里的账号登录 → 进主界面 → 打开“我的预约”能看到列表。

恢复后经 Ingress（带 Host 头，等价浏览器访问）实测：`GET /api/actuator/health` 与 `POST /api/auth/login` 均 **HTTP 200**；全栈 5 个 Pod `1/1 Running`、三条 Endpoints 齐全、HPA 2 副本（见 `evidence/07-incident/06-recovered.md`，raw `_raw/07-incident/06-recovered-get-all.txt`）。浏览器 GUI 登录截图受当前无图形环境限制，以经域名头的 curl 文本旁证，答辩前在有图形环境的机器上补一张登录成功截图（密码栏打码）。

| 指标 | 值 |
|---|---|
| 故障发现 → 定位 | 约 1 分钟内（定位判据：`get endpoints` 为空 + Pod 全 Running） |
| 注入 → 恢复 | 约 2 分钟内（1 条 patch 修复，Endpoints 自动重算） |
| 修复动作数 | 1 条 patch |
| 数据影响 | 无（MySQL、PVC 未动） |
| 用户影响 | 故障窗口内所有接口 502；恢复后无需重新登录（token 仍在 2 小时有效期内） |

---

## 7. 预防措施

| # | 措施 | 落到哪里 | 谁 |
|---|---|---|---|
| 1 | Service selector 与 Deployment 模板 label **同名同值**，写在同一份 yaml 里相邻放置，改一处就同时看到另一处 | `k8s/06-backend.yaml` | B |
| 2 | 部署后**必须**查 `kubectl get endpoints` 非空，写进 runbook 验收清单 | `docs/runbook.md` §7 | C |
| 3 | 加一条 30 秒巡检：`kubectl get endpoints g12-backend -n enterprise-g12 \| grep -q '<none>' && echo ALERT` | 答辩演示用脚本 | C |
| 4 | 后端 `readinessProbe` 保持 `/api/actuator/health`，让“选中的 Pod 是否真能服务”这件事有独立判据 | `k8s/06-backend.yaml` | B |
| 5 | Service/Pod 的 label 用统一前缀（`app.kubernetes.io/name`）而不是裸 `app`，减少手误面 | 下一版 | 三人 |
| 6 | 禁止在生产/演示环境直接 `kubectl patch` 现场对象，改动一律走 yaml + `apply`；本次是演练，patch 命令留存档 | 流程约定 | 三人 |

---

## 附：本次故障的一句话总结（答辩口述版）

> 「我故意把 `g12-backend` 这个 Service 的 selector 改成 `wrong-label`。K8s 的 Endpoints 是由 selector 和 Pod 标签算出来的，所以它立刻变空；nginx 连不上上游，所有 `/api` 请求就 502。定位只用一条命令——`get endpoints` 为空、Pod 却全是 Running，就能断定不是应用问题而是 Service 选中关系断了。修复就是把 selector 改回去，1 秒生效，不用重启任何 Pod。预防靠两条：selector 和标签写在同一份 yaml 里，部署验收必须查 Endpoints 非空。」
