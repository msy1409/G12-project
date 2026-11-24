# 06-hpa —— 自动扩缩容

**证明**：A8（HPA 副本数从 2 变化过）。三件套：初始态 → 压测涨到 3/4 → 停压测回落。

| 顺序 | 文件 | 采集命令 | 判据 |
|---|---|---|---|
| 1 | `01-hpa-initial.png` | `kubectl get hpa -n enterprise-g12` | TARGETS 是数字/60%，不是 `<unknown>`；REPLICAS=2 |
| 2 | `02-describe-hpa.png` | `kubectl describe hpa g12-backend-hpa` | Metrics 有值；Events 有 `SuccessfulRescale` |
| 3 | `03-top-pods.png` | `kubectl top pods`、`kubectl top nodes` | 有 CPU/mem 数字 |
| 4 | `04-hpa-scale-up.png` | 一个终端 `get hpa -w`，另一个跑 busybox 死循环 | 2 → 3 → **4** |
| 5 | `05-hpa-cooldown.png` | 停压测后继续 `-w` | 回落到 2 |

## 压测命令（手册原文即可用）

```bash
kubectl run -it load-test --image=busybox:1.36 --rm -n enterprise-g12 -- \
  /bin/sh -c "while true; do wget -q -O- http://g12-backend:8080/api/actuator/health > /dev/null; done"
```

> B 已确认 `k8s/06-backend.yaml` 里 Service 是 `port: 8080 → targetPort: 8080`，所以集群内直接带 `:8080`，手册第 27 条的命令原样能用。**若哪天有人把它改成 `port: 80`，这条压测会连不上、CPU 上不去、HPA 不动** —— 改端口必须同时改 A 的 `BACKEND_PORT`。

## 几个必须提前知道的事实

- **`<unknown>/60%`**：backend 没写 `resources.requests.cpu`，HPA 算不出使用率。B 已确认 `06-backend.yaml` 写了 **requests 200m / limits 500m**，所以这里应该是数字。
- **算一下要多少负载才触发**：HPA 按 `requests × 60%` 算目标 → **200m × 60% = 120m/副本**。一个 busybox 的 `wget` 死循环打到 `/api/actuator/health`，单 Pod 通常只有 **20–40m**，两条腿加起来也到不了 240m，**HPA 很可能一动不动**。
  稳妥做法：**开 4–6 个压测 Pod 并发**（都指向同一个 Service），或者更省事地把 target 临时降到 30%：
  ```bash
  kubectl patch hpa g12-backend-hpa -n enterprise-g12 --type merge -p '{"spec":{"metrics":[{"type":"Resource","resource":{"name":"cpu","target":{"type":"Utilization","averageUtilization":30}}}]}}'
  ```
  演示完记得改回 60%（patch 回去），并在 evidence 的 md 里写明“临时调过 target”，否则和 09-hpa.yaml 里的 60% 对不上。
  多开几个压测 Pod 的写法：
  ```bash
  for i in 1 2 3 4 5 6; do
    kubectl run load-test-$i --image=busybox:1.36 -n enterprise-g12 --restart=Never -- \
      /bin/sh -c "while true; do wget -q -O- http://g12-backend:8080/api/actuator/health > /dev/null; done"
  done
  # 停：kubectl delete pod -n enterprise-g12 -l run=load-test --wait=false
  ```
- **扩/缩的节奏（按仓库里 `09-hpa.yaml` 的实际 `behavior`，不是 k8s 默认值）**：
  `scaleUp: stabilizationWindowSeconds 60 + Pods +1 / 30s` → **2→3→4 至少要 1.5–2 分钟**，压 30 秒就停很可能只到 3 甚至没动，**压够 3 分钟再截图**；
  `scaleDown: stabilizationWindowSeconds 60 + Pods -1 / 60s` → **停压测后约 1–2 分钟开始降**，不是默认的 5 分钟。别按"5 分钟"等。
- **压测结束要确认 Pod 被回收**：`--rm` 的 Pod 在 Ctrl+C 后删除；`kubectl get pods` 里不该残留 `load-test`。
