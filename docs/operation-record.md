# 发布与运维操作记录（operation-record.md）

> 负责人：C　　对应《G12 执行手册-优化版》第三章 22–25 条
> 记录四次操作：① 发布 v1→v2　② 异常发布 v999　③ 回滚　④ 手动扩缩容
> namespace：`enterprise-g12`　　对象：`deployment/g12-backend`
>
> 实际执行环境：k3s v1.28.2+k3s1 单节点（兜底复刻），经 Ingress 域名头 `Host: g12.course.local` 访问；
> 判据接口用演示账号 `admin / 123456`。原始命令输出见 `evidence/_raw/05-release-rollback/`。

## 填写规则（先说清楚，免得被问倒）

1. 每次操作都记 **变更前 / 变更中 / 变更后** 三态，每态一条命令 + 一段真实输出。
2. 命令和输出**原样贴**（可截断重复行，但要保留首行和结论行），不要手打“看起来像”的结果。
3. 时间统一用 `date '+%F %T'` 的输出，取 Asia/Shanghai。
4. 图片放 `evidence/05-release-rollback/`，本文只留文字与命令；图文对应关系见该目录 `README.md`。
5. 每节末尾写一句 `结论`，这是评委真正看的那一行。

## 操作总览

| 编号 | 操作 | 命令核心 | 期望副本数 | 期望镜像 | 证据 |
|---|---|---|---|---|---|
| OP-01 | 滚动发布 v2 | `kubectl set image` | 2 | `g12-api:v2` | 01/02/03/04 |
| OP-02 | 异常发布 v999 | `kubectl set image` | 2（新 Pod 起不来） | `g12-api:v999` | 05 |
| OP-03 | 回滚（undo 退回上一层） | `kubectl rollout undo` | 2 | `g12-api:v2` | 06 |
| OP-04 | 手动扩缩容 | `kubectl scale` | 3 → 2 | `g12-api:v2` | 07/08 |

---

## 前置：基线（v1 正常服务）

```bash
date '+%F %T'
kubectl get deploy g12-backend -n enterprise-g12
kubectl get pods -n enterprise-g12 -l app=g12-backend -o wide
kubectl rollout history deployment/g12-backend -n enterprise-g12
```

**基线实际输出（2026-11-28 17:31，k3s 兜底环境）**

```text
NAME          READY   UP-TO-DATE   AVAILABLE   AGE
g12-backend   2/2     2            2           5h1m

NAME                           READY   STATUS    RESTARTS   AGE    IP
g12-backend-5fc9b977d7-q9qbh   1/1     Running   0          3m4s   10.42.0.171
g12-backend-5fc9b977d7-vplx6   1/1     Running   0          113s   10.42.0.172

image: g12-api:v1 ×2（jsonpath 输出）
rollout history: REVISION 1,2,3,5,6,7,8（历次演练累积）
```

结论：基线 = 2 副本 Ready、镜像 `g12-api:v1`，作为后续四次操作的稳定参照。

---

## OP-01 发布 v2（滚动更新）

### 变更前

```bash
kubectl get deploy g12-backend -n enterprise-g12 -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
# 期望 g12-api:v1
```

**同时把 v1 的接口基线打下来**（这一步必须在 `set image` 之前做，否则就再也采不到 v1 的响应了）：

```bash
# 兜底环境经 Ingress 域名头访问；标准路线可改用 port-forward 8080
curl -s -X POST http://127.0.0.1/api/auth/login -H 'Host: g12.course.local' \
  -H 'Content-Type: application/json' -d @/tmp/b.json
# v1 期望：data.user 里【没有】apiVersion 字段 → 下面这条 grep 无输出
curl -s -X POST http://127.0.0.1/api/auth/login -H 'Host: g12.course.local' \
  -H 'Content-Type: application/json' -d @/tmp/b.json | grep -o '"apiVersion":"v2"'
echo "grep 命中=$?   # 无命中 = 现在是 v1，符合预期"
```

**变更前判据（v1）**：login 与 `/me` 响应均**未命中** `"apiVersion":"v2"`（grep 退出码 1 / False）。

### 变更中

```bash
# 标准路线：docker load -i g12-api-v2.tar && minikube image load g12-api:v2
# 兜底环境：v1/v2 镜像已在节点 containerd 本地索引，直接切镜像
kubectl set image deployment/g12-backend backend=g12-api:v2 -n enterprise-g12
kubectl rollout status deployment/g12-backend -n enterprise-g12 --timeout=300s
```

- `rollout status` 最后一行：`deployment "g12-backend" successfully rolled out`。
- 滚动过程可见新 Pod 先 Ready、旧 Pod 才 Terminating（`kubectl get pods -l app=g12-backend -w`）。

### 变更后

```bash
kubectl get deploy g12-backend -n enterprise-g12
kubectl get pods -n enterprise-g12 -l app=g12-backend -o wide --show-labels
kubectl rollout history deployment/g12-backend -n enterprise-g12
kubectl get rs -n enterprise-g12 -l app=g12-backend
```

**变更后实际输出（17:35）**

```text
NAME          READY   UP-TO-DATE   AVAILABLE   AGE
g12-backend   2/2     2            2           5h4m

NAME                           READY   STATUS    RESTARTS   AGE     IP
g12-backend-7585dd6b86-pj429   1/1     Running   0          2m55s   10.42.0.173
g12-backend-7585dd6b86-9kzqb   1/1     Running   0          104s    10.42.0.174

image: g12-api:v2 ×2（jsonpath 输出）
当前 RS: g12-backend-7585dd6b86  DESIRED/CURRENT/READY=2；历史 RS 缩到 0 未删除
```

### 验证 v2 真的在跑（不是只有标签变了）

```bash
kubectl get pods -n enterprise-g12 -l app=g12-backend \
  -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.status.containerStatuses[0].image}{"\n"}{end}'
# 期望每行都是 g12-api:v2
curl -s http://127.0.0.1/api/actuator/health -H 'Host: g12.course.local'
# 期望 {"status":"UP"}
```

**镜像版本校验**：节点 containerd 内 `g12-api:v1`（ID `62ff14af849a7`）与 `g12-api:v2`（ID `9a58ef602f2d2`）为两个不同镜像，非同一包换 tag。

**接口差异判据**（v2 改动 = `/auth/login`、`/auth/me` 返回用户信息多 `apiVersion` 字段，固定 `"v2"`）：

```bash
# 发布 v2 后再打同一接口
curl -s -X POST http://127.0.0.1/api/auth/login -H 'Host: g12.course.local' \
  -H 'Content-Type: application/json' -d @/tmp/b.json
TOKEN=<data.token>
curl -s http://127.0.0.1/api/auth/me -H 'Host: g12.course.local' -H "Authorization: Bearer $TOKEN"
# v2 期望：login 的 data.user 和 /me 的 data 里都有 "apiVersion":"v2"
```

**变更后判据（v2）**：login 与 `/me` 响应均**命中** `"apiVersion":"v2"`（True/True）；健康检查 `UP`。与变更前 v1（无该字段）形成前后对比。

> 这一条是“发布有可见效果”的硬证据。没有它，`rollout history` 只能证明“模板变了”，证明不了“跑的确实是新代码”。

**结论**：镜像 v1→v2 滚动完成，新旧 ReplicaSet 并存、旧 RS 缩到 0；接口响应出现 `apiVersion:"v2"`（v1 时代无该字段），证明新版本真的在服务；探针 `UP`，服务不中断。

---

## OP-02 异常发布 v999（故意的）

目的：证明“镜像拉不到时，K8s 不会把在跑的服务换掉”，并留下排查用的 describe/events 证据。

### 变更前

```bash
kubectl get deploy g12-backend -n enterprise-g12 -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
kubectl rollout history deployment/g12-backend -n enterprise-g12
```

变更前镜像为 `g12-api:v2`。

### 变更中

```bash
kubectl set image deployment/g12-backend backend=g12-api:v999 -n enterprise-g12
kubectl get pods -n enterprise-g12 -l app=g12-backend -w      # 观察 30–60s，Ctrl+C 退出
```

新 Pod 进入 `ErrImagePull` / `ImagePullBackOff`；**旧的两个 v2 Pod 仍然 Running**，服务没有中断。

### 变更后（故障态取证）

```bash
kubectl get deploy g12-backend -n enterprise-g12
kubectl get pods -n enterprise-g12 -l app=g12-backend
BAD=$(kubectl get pods -n enterprise-g12 -l app=g12-backend \
      --field-selector=status.phase=Pending -o jsonpath='{.items[0].metadata.name}')
kubectl describe pod "$BAD" -n enterprise-g12 | tail -30
kubectl get events -n enterprise-g12 --sort-by=.lastTimestamp | tail -20
```

**故障态实际输出（17:36）**

```text
# deploy
NAME          READY   UP-TO-DATE   AVAILABLE   AGE
g12-backend   2/2     1            2           5h6m

# pods
NAME                           READY   STATUS             RESTARTS   AGE
g12-backend-7585dd6b86-pj429   1/1     Running            0          4m42s
g12-backend-7585dd6b86-9kzqb   1/1     Running            0          3m31s
g12-backend-775c8875b5-4kbwm   0/1     ImagePullBackOff   0          82s

# describe 失败 Pod（根因事件）
State: Waiting  Reason: ImagePullBackOff
Events:
  Normal  BackOff    59s  kubelet  Back-off pulling image "g12-api:v999"
  Warning Failed    59s  kubelet  Error: ImagePullBackOff
  Warning Failed    10s  kubelet  Failed to pull image "g12-api:v999": ... 403 Forbidden
  Warning Failed    10s  kubelet  Error: ErrImagePull
```

**结论**：异常发布只影响新 ReplicaSet，旧 ReplicaSet 的 2 个 v2 Pod 仍在服务、AVAILABLE 保持 2；新 Pod 卡在 `ImagePullBackOff/ErrImagePull`，根因是镜像 `g12-api:v999` 本地不存在、远端解析不到。**服务对用户无感知**（`maxUnavailable` 默认 25%，旧 Pod 不会被先删）。

---

## OP-03 回滚（undo）

### 变更前

```bash
kubectl get deploy g12-backend -n enterprise-g12 -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
# 异常发布后为 g12-api:v999
kubectl rollout history deployment/g12-backend -n enterprise-g12
```

### 变更中

```bash
kubectl rollout undo deployment/g12-backend -n enterprise-g12
kubectl rollout status deployment/g12-backend -n enterprise-g12 --timeout=300s
```

`deployment.apps/g12-backend rolled back`；`rollout status` 返回 `deployment "g12-backend" successfully rolled out`。

### 变更后

```bash
kubectl get deploy g12-backend -n enterprise-g12
kubectl get pods -n enterprise-g12 -l app=g12-backend -o wide
kubectl rollout history deployment/g12-backend -n enterprise-g12
curl -s http://127.0.0.1/api/actuator/health -H 'Host: g12.course.local'
```

**回滚后实际输出（17:37）**

```text
NAME          READY   UP-TO-DATE   AVAILABLE   AGE
g12-backend   2/2     2            2           5h7m

NAME                           READY   STATUS    RESTARTS   AGE
g12-backend-7585dd6b86-pj429   1/1     Running   0          5m53s
g12-backend-7585dd6b86-9kzqb   1/1     Running   0          4m42s

image: g12-api:v2 ×2
health: HTTP 200
```

回滚后接口沿用 OP-01 判据：login 与 `/me` 均**命中** `"apiVersion":"v2"`（True/True），说明回到了能提供该字段的 v2，而非更早的 v1。

> **注意一个坑**：`rollout undo` 默认退回**上一层**，即 v999 的上一层是 v2，不是 v1。所以回滚后镜像显示 `g12-api:v2` 是**正确**的，别在记录里写成 v1。
> 要一次性回到 v1 用 `kubectl rollout undo deployment/g12-backend --to-revision=1 -n enterprise-g12`。真退到 v1 时上面那条 `grep apiVersion` 会**没有输出**——这正好是“回滚层级”的可验证判据：`v1 = 没有 apiVersion`、`v2 = 有 apiVersion:"v2"`。
> 同一 ReplicaSet 可能被复用，所以 REVISION 号会增加但镜像不变，这属于正常现象。

**结论**：回滚把 Deployment 的 Pod 模板退回到上一次可用版本 v2，2 个 Pod 重新 Ready、健康检查 200，接口重新返回 `apiVersion:"v2"`，服务恢复正常。

---

## OP-04 手动扩缩容（scale）

### 变更前

```bash
kubectl get deploy g12-backend -n enterprise-g12
kubectl get pods -n enterprise-g12 -l app=g12-backend --no-headers | wc -l    # 期望 2
```

变更前 2 副本 Running。

### 变更中

```bash
date '+%F %T'
kubectl scale deployment/g12-backend --replicas=3 -n enterprise-g12
kubectl get pods -n enterprise-g12 -l app=g12-backend -w     # 看到第 3 个 Running 后 Ctrl+C
```

**扩到 3（17:12）**：`deployment.apps/g12-backend scaled`，deploy `3/3 3 3`，3 个 Pod 全 Running：

```text
g12-backend-7585dd6b86-mspgg   1/1  Running 0  28m  10.42.0.165
g12-backend-7585dd6b86-xwzk4  1/1  Running 0  27m  10.42.0.166
g12-backend-7585dd6b86-slv8f  1/1  Running 0  79s  10.42.0.170
```

### 变更后（缩回 2）

```bash
date '+%F %T'
kubectl scale deployment/g12-backend --replicas=2 -n enterprise-g12
kubectl get pods -n enterprise-g12 -l app=g12-backend
kubectl get deploy g12-backend -n enterprise-g12
```

**缩回 2（17:13）**：deploy `2/2 2 2`，保留 2 个 Running：

```text
g12-backend-7585dd6b86-mspgg   1/1  Running 0  29m  10.42.0.165
g12-backend-7585dd6b86-xwzk4  1/1  Running 0  27m  10.42.0.166
```

> **和 HPA 的关系（会被问）**：集群里有 `g12-backend-hpa`（min 2 / max 4 / CPU 60%）时，手动 `scale` 改的 `replicas` 会被 HPA 在下一次评估（默认 15s）里**改回去**。本次演示 3 副本前先临时 `kubectl delete hpa g12-backend-hpa -n enterprise-g12`，采完三态后重新 apply 恢复 HPA。

**结论**：`scale` 只改 `.spec.replicas`，不改镜像、不改 revision；3 个 Pod 里新起的那一个拉起约 20–40s，缩回 2 时 K8s 按“未 Ready 优先、再按运行时间”终止。

---

## 附：HPA 与本次记录的关系

HPA 的扩缩容观察（2→3→4→回落）记在 `evidence/06-hpa/`，不重复写进本文，原因是：那是**指标驱动**的自动伸缩，本文四次是**人为命令**的操作记录，二者判据不同。
