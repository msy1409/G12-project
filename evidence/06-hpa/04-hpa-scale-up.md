# 压测：2 → 3 → 4

| 项 | 内容 |
|---|---|
| 图片 | `04-hpa-scale-up.png` |
| 所属目录 | `evidence/06-hpa/` |
| 采集人 | B（k3s 兜底环境代采） |
| 采集时间 | 2026-11-28 17:01（Asia/Shanghai） |
| 环境 | k3s v1.28.2+k3s1 单节点 / namespace `enterprise-g12` |
| 关联文档 | `docs/runbook.md §7` |

## 对应命令

```bash
# 终端 1：看 HPA
kubectl get hpa -n enterprise-g12 -w
# 终端 2：并发压测（经 Ingress 域名头打后端业务接口，登录拿 token 后循环请求 applies/users/stats 等接口）
bash g12-load.sh 12 150
# 压测时看 Pod CPU
kubectl top pod -n enterprise-g12 -l app=g12-backend
```

## 图上应当看得出

REPLICAS 从 2 涨到 3、再到 4（上限）；TARGETS 超过 60% 那一刻。
URL 走 Service `http://g12-backend:8080/...`（Service 端口 8080 → targetPort 8080）。

## 实际输出（截取）

扩容到 3 副本快照（CPU 利用率 81%，已超 60% 阈值）：

```text
NAME              REFERENCE                TARGETS   MINPODS   MAXPODS   REPLICAS   AGE
g12-backend-hpa   Deployment/g12-backend   81%/60%   2         4         3          16m

---
NAME                           CPU(cores)   MEMORY(bytes)
g12-backend-7585dd6b86-mspgg   165m         228Mi
g12-backend-7585dd6b86-xwzk4   161m         230Mi
```

达到上限 4 副本由 `kubectl describe hpa` 事件流记录（见 `02-describe-hpa.md`）：

```text
Normal  SuccessfulRescale  8m13s  New size: 3; reason: cpu resource utilization (percentage of request) above target
Normal  SuccessfulRescale  7m43s  New size: 4; reason: cpu resource utilization (percentage of request) above target
```

## 观察结论

并发压测把后端 CPU 利用率抬到阈值之上，HPA 按每 30s 加 1 的策略依次扩容：2 → 3 → 4，达到 MAXPODS 4 上限；新 Pod 由 startupProbe 容忍慢启动后陆续 Ready，负载被分摊到多副本。

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 单节点 3 vCPU 资源有限，第 4 个 Pod 的瞬时 `kubectl get hpa` 快照受调度/启动节奏影响，扩容到 4 以 describe 事件 `New size: 4` 为准。
