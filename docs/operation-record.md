# 发布与运维操作记录（operation-record.md）

> 负责人：C　　对应《G12 执行手册-优化版》第三章 22–25 条
> 记录四次操作：① 发布 v1→v2　② 异常发布 v999　③ 回滚　④ 手动扩缩容
> namespace：`enterprise-g12`　　对象：`deployment/g12-backend`

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
| OP-03 | 回滚 | `kubectl rollout undo` | 2 | `g12-api:v1` | 06 |
| OP-04 | 手动扩缩容 | `kubectl scale` | 3 → 2 | `g12-api:v1` | 07/08 |

---

## 前置：基线（v1 正常服务）

```bash
date '+%F %T'
kubectl get deploy g12-backend -n enterprise-g12
kubectl get pods -n enterprise-g12 -l app=g12-backend -o wide
kubectl rollout history deployment/g12-backend -n enterprise-g12
```

**变更前（基线）输出**

```
【贴：date 结果】
【贴：kubectl get deploy 输出，应显示 IMAGE=g12-api:v1  READY=2/2】
【贴：两个 Pod 的 NAME/STATUS/READY/RESTARTS】
【贴：rollout history，应只有 REVISION 1 一行】
```

结论：基线 = 2 副本、镜像 `g12-api:v1`、`revision 1`。

---

## OP-01 发布 v2（滚动更新）

### 变更前

```bash
kubectl get deploy g12-backend -n enterprise-g12 -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
# 期望 g12-api:v1
```

**同时把 v1 的接口基线打下来**（这一步必须在 `set image` 之前做，否则就再也采不到 v1 的响应了）：

```bash
# 端子 A：把 Service 转到 VM 本地，发布全程别关（这条会一直占着终端）
kubectl port-forward -n enterprise-g12 svc/g12-backend 8080:8080
```

```bash
# 端子 B：打一个 v1 的响应留基线
curl -s -X POST http://127.0.0.1:8080/api/auth/login \
  -H 'Content-Type: application/json' -d '{"username":"zhangsan","password":"123456"}'
# v1 期望：data.user 里【没有】apiVersion 字段 → 下面这条 grep 无输出
curl -s -X POST http://127.0.0.1:8080/api/auth/login \
  -H 'Content-Type: application/json' -d '{"username":"zhangsan","password":"123456"}' \
  | grep -o '"apiVersion":"v2"'
echo "grep 退出码=$?   # 1 = 没命中 = 现在是 v1，符合预期"
```

【贴：v1 的 login 响应（无 apiVersion）+ grep 退出码 1】

### 变更中

```bash
docker load -i g12-api-v2.tar
minikube image load g12-api:v2
minikube image ls | grep g12-api            # 期望同时看到 v1 和 v2

kubectl set image deployment/g12-backend backend=g12-api:v2 -n enterprise-g12
kubectl rollout status deployment/g12-backend -n enterprise-g12 --timeout=180s
```

- `rollout status` 期望最后一行：`deployment "g12-backend" successfully rolled out`。
- 想看滚动过程可以另开终端 `kubectl get pods -n enterprise-g12 -l app=g12-backend -w`，能看到 `Pending/Running` 交替、新 Pod 先 Ready 旧 Pod 才 Terminating。

【贴：docker load 与 minikube image ls 输出（打码无关内容）】
【贴：set image 的 deployment.apps/g12-backend image updated】
【贴：rollout status 全段输出】

### 变更后

```bash
kubectl get deploy g12-backend -n enterprise-g12
kubectl get pods -n enterprise-g12 -l app=g12-backend -o wide --show-labels
kubectl rollout history deployment/g12-backend -n enterprise-g12
kubectl get rs -n enterprise-g12 -l app=g12-backend
```

【贴输出】

### 验证 v2 真的在跑（不是只有标签变了）

```bash
kubectl get pods -n enterprise-g12 -l app=g12-backend \
  -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.status.containerStatuses[0].image}{"\n"}{end}'
# 期望每行都是 g12-api:v2
kubectl run probe-check --rm -it --image=busybox:1.36 -n enterprise-g12 -- wget -qO- http://g12-backend:8080/api/actuator/health
# 期望 {"status":"UP"}
```

**⚠️ 先验一下两个 tar 真的是两个版本**：B 交来的 `g12-api-v1.tar` 和 `g12-api-v2.tar` 都是 137.8MB，大小一样是正常的（同一套依赖，只改了一个字段），但**万一 v2 包里装的其实还是 v1**，发布演练就成了“换了个 tag”，答辩一问就露。

```bash
docker load -i g12-api-v1.tar; docker load -i g12-api-v2.tar
docker images | grep g12-api          # 两个 IMAGE ID 必须不同
docker image inspect g12-api:v1 --format '{{.Id}}'
docker image inspect g12-api:v2 --format '{{.Id}}'
```

两个 ID **相同 = 包发错了**，找 B 重打；不同才算过。这一步的输出也贴进来，后面的“镜像确实从 v1 换成了 v2”才有说服力。

【贴输出】

**⚠️ 但 ID 不同也只说明包不同，还要证明“改的是我们说的那个地方”。** B 确认 v2 的改动是：`/auth/login` 和 `/auth/me` 返回的用户信息里多了 `apiVersion` 字段，v2 固定返回 `"v2"`。所以发布前后各打一次同样请求，用响应差异当证据：

```bash
# 在 VM 上把 Service 转到本地（另一个终端挂着，别关）
kubectl port-forward -n enterprise-g12 svc/g12-backend 8080:8080

# —— 发布 v1 时（变更前）先打一次，留存基线 ——
curl -s -X POST http://127.0.0.1:8080/api/auth/login \
  -H 'Content-Type: application/json' -d '{"username":"zhangsan","password":"123456"}'
# v1 期望：data.user 里【没有】apiVersion 字段
```

```bash
# —— 发布 v2 后再打一次同一接口（port-forward 那个终端还挂着） ——
curl -s -X POST http://127.0.0.1:8080/api/auth/login \
  -H 'Content-Type: application/json' -d '{"username":"zhangsan","password":"123456"}'
TOKEN=<上面返回的 data.token>
curl -s http://127.0.0.1:8080/api/auth/me -H "Authorization: Bearer $TOKEN"
# v2 期望：data.user（/auth/login）和 data（/auth/me）里都有 "apiVersion":"v2"
```

一次 grep 就能判定（比肉眼看快）：

```bash
curl -s http://127.0.0.1:8080/api/auth/me -H "Authorization: Bearer $TOKEN" | grep -o '"apiVersion":"v2"'
# 有输出 = v2；无输出 = 还是 v1（说明 set image 没换到新 Pod，或 v2 包里装的是 v1）
```

【贴：发布后 /auth/login 响应（有 apiVersion）+ /auth/me 响应 + grep 命中的那一行】

> 这一条是“发布有可见效果”的硬证据。没有它，`rollout history` 只能证明“模板变了”，证明不了“跑的确实是新代码”。
> 和变更前那张（grep 退出码 1、无 `apiVersion`）配成一组，就是完整的前后对比。

**结论**：镜像 v1→v2 滚动完成，`revision 1 → 2`，新旧 ReplicaSet 并存，旧 RS 缩到 0；接口响应里出现 `apiVersion:"v2"`（v1 时代没有该字段），证明新版本真的在服务；探针仍 `UP`，服务不中断。

---

## OP-02 异常发布 v999（故意的）

目的：证明“镜像拉不到时，K8s 不会把在跑的服务换掉”，并留下排查用的 describe/events 证据。

### 变更前

```bash
kubectl get deploy g12-backend -n enterprise-g12 -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
kubectl rollout history deployment/g12-backend -n enterprise-g12
```

【贴输出，应为 v2 / revision 2】

### 变更中

```bash
kubectl set image deployment/g12-backend backend=g12-api:v999 -n enterprise-g12
kubectl get pods -n enterprise-g12 -l app=g12-backend -w      # 观察 30–60s，Ctrl+C 退出
```

期望看到：新 Pod `ErrImageNeverPull`（本地没这个镜像且 `imagePullPolicy: IfNotPresent`）或 `ImagePullBackOff`；
**旧的两个 v2 Pod 仍然 Running**，服务没有中断。

### 变更后（故障态取证）

```bash
kubectl get deploy g12-backend -n enterprise-g12
kubectl get pods -n enterprise-g12 -l app=g12-backend
BAD=$(kubectl get pods -n enterprise-g12 -l app=g12-backend \
      --field-selector=status.phase=Pending -o jsonpath='{.items[0].metadata.name}')
echo "$BAD"
kubectl describe pod "$BAD" -n enterprise-g12 | tail -30
kubectl get events -n enterprise-g12 --sort-by=.lastTimestamp | tail -20
kubectl rollout status deployment/g12-backend -n enterprise-g12 --timeout=60s   # 会超时，故意留证
```

【贴：get deploy（AVAILABLE 仍 2，IMAGE 变 v999）】
【贴：get pods（1 个新的 Pending/ImagePullBackOff + 2 个旧 v2 Running）】
【贴：describe 的 Events 段——要有 `Failed to pull image "g12-api:v999"` 或 `container image not present`】
【贴：events 尾部】
【贴：rollout status 超时信息 `Waiting for deployment ... rollout to finish: 1 out of 2 new replicas ...`】

**结论**：异常发布只影响新 ReplicaSet，旧 ReplicaSet 的 2 个 Pod 仍在服务；`rollout status` 卡在 `1 out of 2 new replicas have been updated`，这行就是“发布没成功”的判据。**服务对用户无感知**（`maxUnavailable` 默认 25% 时旧 Pod 不会被先删）。

---

## OP-03 回滚（undo）

### 变更前

```bash
kubectl get deploy g12-backend -n enterprise-g12 -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
# 期望 g12-api:v999（异常发布后的状态）
kubectl rollout history deployment/g12-backend -n enterprise-g12
```

【贴输出，应看到 revision 3 对应 v999】

### 变更中

```bash
kubectl rollout undo deployment/g12-backend -n enterprise-g12
kubectl rollout status deployment/g12-backend -n enterprise-g12 --timeout=180s
```

【贴：`deployment.apps/g12-backend rolled back`】
【贴：rollout status 成功行】

### 变更后

```bash
kubectl get deploy g12-backend -n enterprise-g12
kubectl get pods -n enterprise-g12 -l app=g12-backend -o wide
kubectl rollout history deployment/g12-backend -n enterprise-g12
kubectl run probe-check --rm -it --image=busybox:1.36 -n enterprise-g12 -- wget -qO- http://g12-backend:8080/api/actuator/health
```

【贴输出：IMAGE 回到 g12-api:v2】

回滚后接口也要验一遍（沿用 OP-01 那个 `apiVersion` 判据）：

```bash
curl -s -X POST http://127.0.0.1:8080/api/auth/login \
  -H 'Content-Type: application/json' -d '{"username":"zhangsan","password":"123456"}' \
  | grep -o '"apiVersion":"v2"'
# 有输出 = 回滚到了能提供 apiVersion 的 v2 版本；无输出 = 退到了更早的 v1，说明回滚层级不是你预期的那一层
```

【贴：grep 命中的那一行】

> **注意一个坑**：`rollout undo` 默认退回**上一层**，即 v999 的上一层是 v2，不是 v1。所以回滚后镜像显示 `g12-api:v2` 是**正确**的，别在记录里写成 v1。
> 要一次性回到 v1 用 `kubectl rollout undo deployment/g12-backend --to-revision=1 -n enterprise-g12`。真退到 v1 时上面那条 `grep apiVersion` 会**没有输出**——这正好是“回滚层级”的可验证判据：`v1 = 没有 apiVersion`、`v2 = 有 apiVersion:"v2"`。想演示得更漂亮，可以再 `--to-revision=1` 一次、截图对比，然后 `set image` 回到 v2。
> 历史里 `revision 3` 会变成“v999 那版”，回滚后 `revision 4` 是新的 v2 记录（同一个 ReplicaSet 可能被复用，所以 REVISION 号会增加但镜像不变，这属于正常现象）。

**结论**：回滚把 Deployment 的 Pod 模板退回到上一次发布的版本，`AVAILABLE` 在 30–60s 内回到 2/2，探针 `UP`，接口重新返回 `apiVersion:"v2"`，`rollout history` 多了新 revision。

---

## OP-04 手动扩缩容（scale）

### 变更前

```bash
kubectl get deploy g12-backend -n enterprise-g12
kubectl get pods -n enterprise-g12 -l app=g12-backend --no-headers | wc -l    # 期望 2
```

【贴输出】

### 变更中

```bash
date '+%F %T'
kubectl scale deployment/g12-backend --replicas=3 -n enterprise-g12
kubectl get pods -n enterprise-g12 -l app=g12-backend -w     # 看到第 3 个从 Pending → ContainerCreating → Running 后 Ctrl+C
```

【贴：`deployment.apps/g12-backend scaled`】
【贴：3 个 Pod 的列表】

### 变更后（缩回 2）

```bash
date '+%F %T'
kubectl scale deployment/g12-backend --replicas=2 -n enterprise-g12
kubectl get pods -n enterprise-g12 -l app=g12-backend
kubectl get deploy g12-backend -n enterprise-g12
```

【贴输出】

> **和 HPA 的关系（会被问）**：集群里有 `g12-backend-hpa`（min 2 / max 4 / CPU 60%）时，手动 `scale` 改的 `replicas` 会被 HPA 在下一次评估（默认 15s）里**改回去**。要稳定演示 3 副本，先 `kubectl delete hpa g12-backend-hpa -n enterprise-g12` 或者接受它被拉回。

**结论**：`scale` 只改 `.spec.replicas`，不改镜像、不改 revision；3 个 Pod 里新起的那一个拉起约 20–40s，缩回 2 时 K8s 按“未 Ready 优先、再按运行时间”终止。

---

## 附：HPA 与本次记录的关系

HPA 的扩缩容观察（2→3→4→回落）记在 `evidence/06-hpa/README.md`，不重复写进本文，原因是：那是**指标驱动**的自动伸缩，本文四次是**人为命令**的操作记录，二者判据不同。
