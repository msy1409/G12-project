# Pod 详情（探针与事件）

| 项 | 内容 |
|---|---|
| 图片 | `04-describe-pod.png` |
| 所属目录 | `evidence/04-logs-events/` |
| 采集人 | B（k3s 兜底环境代采） |
| 采集时间 | 2026-11-28 17:40（Asia/Shanghai） |
| 环境 | k3s v1.28.2+k3s1 单节点 / namespace `enterprise-g12` |
| 关联文档 | `docs/runbook.md §7` |

## 对应命令

```bash
kubectl describe pod <后端pod名> -n enterprise-g12
```

## 图上应当看得出

`Containers` 里 `Image:` 为 `g12-api:v2`、`Port: 8080/TCP`；Readiness / Startup 探针 URL 都是 **`/api/actuator/health`**；资源 requests 200m/512Mi、limits 1/1Gi；`Events` 段最后是 `Started`，没有反复 `Killing`。探针写成 `/actuator/health`（少 `/api`）是典型错法，Pod 会永远不 Ready。

## 实际输出（截取）

```text
Name:             g12-backend-7585dd6b86-pj429
Namespace:        enterprise-g12
Node:             s/10.0.2.15
Labels:           app=g12-backend, pod-template-hash=7585dd6b86, tier=backend
Status:           Running
IP:               10.42.0.173
Controlled By:    ReplicaSet/g12-backend-7585dd6b86
Containers:
  backend:
    Image:          g12-api:v2
    Port:           8080/TCP
    State:          Running
    Ready:          True
    Restart Count:  0
    Limits:        cpu: 1   memory: 1Gi
    Requests:       cpu: 200m   memory: 512Mi
    Readiness:  http-get http://:8080/api/actuator/health delay=60s timeout=3s period=10s #failure=10
    Startup:    http-get http://:8080/api/actuator/health delay=0s timeout=1s period=10s #failure=24
    Environment Variables from: g12-config  ConfigMap  Optional: false
    Environment:
      SERVER_PORT:  8080
      DB_PASSWORD:  <set to the key 'DB_PASSWORD' in secret 'g12-secret'>  Optional: false
      JWT_SECRET:   <set to the key 'JWT_SECRET' in secret 'g12-secret'>   Optional: false
Conditions:
  Initialized   True
  Ready         True
  ContainersReady True
  PodScheduled  True
QoS Class:      Burstable
Events:
  Normal   Scheduled  8m49s  default-scheduler  Successfully assigned ... to s
  Normal   Pulled     8m48s  kubelet            Container image "g12-api:v2" already present on machine
  Normal   Created    8m47s  kubelet            Created container backend
  Normal   Started    8m47s  kubelet            Started container backend
  Warning  Unhealthy  8m19s  kubelet            Startup probe failed: ... connection refused
  Warning  Unhealthy  8m7s   kubelet            Startup probe failed: ... context deadline exceeded
```

## 观察结论

Pod 镜像、端口、探针路径、资源配额与环境变量引用（ConfigMap + Secret 占位）配置正确；启动期 Startup 探针短暂探不到端口（慢启动），被 startupProbe 容忍后进入 Running、Ready True，RESTARTS 为 0，无异常重启。

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图；Secret 值不出现明文。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
