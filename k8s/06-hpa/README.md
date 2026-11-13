# HPA 演示口径（G12-backend）

对应清单：`k8s/09-hpa.yaml`。

> 目录名按任务书写作 `06-hpa`；`k8s/` 里 HPA 清单本身编号是 09（06 号文件是 backend 的 Deployment/Service），两者不冲突。

| 参数 | 值 |
| --- | --- |
| apiVersion | `autoscaling/v2` |
| 名称 | `g12-backend-hpa` |
| target | `Deployment/g12-backend` |
| minReplicas | 2 |
| maxReplicas | 4 |
| CPU 目标 | `averageUtilization: 60` |

## 1. 先算清楚阈值（答辩必问）

HPA 的 CPU 百分比是**相对 Pod 的 `requests.cpu`**，不是节点、也不是 limits。

```
g12-backend 的 requests.cpu = 200m
扩容阈值 = 200m × 60% = 120m/副本
```

也就是说，**平均每副本 CPU 超过 120m 才会从 2 扩到 3**。

单个 `busybox wget` 死循环打 `/api/actuator/health` 时，后端单 Pod 实测只有 **20–40m**，
离 120m 差得远，副本会**一直停在 2 不动**。这不是 HPA 坏了，是压不上去。

```bash
# 观察实时用量
kubectl top pods -n enterprise-g12 -l app=g12-backend
kubectl get hpa -n enterprise-g12 g12-backend-hpa -w
```

## 2. 两套可行的演示解法

### 方案 A：加并发（推荐，讲得最干净）

单个压测 Pod 不够，就开 4–6 个一起打：

```bash
for i in $(seq 1 6); do
  kubectl run g12-load-$i -n enterprise-g12 --image=busybox:1.36 --restart=Never \
    --labels=app=g12-load -- sh -c \
    'while true; do wget -q -O- http://g12-backend:8080/api/actuator/health >/dev/null; done'
done
```

再看：

```bash
kubectl get hpa -n enterprise-g12 g12-backend-hpa -w
```

正常情况下 1–3 分钟内 `REPLICAS` 由 2 → 3 → 4，`TARGETS` 会显示类似 `185%/60%`。

演示完清理：

```bash
kubectl delete pod -n enterprise-g12 -l app=g12-load
```

### 方案 B：临时降低目标值（不够快时用）

压力已经打上了、但 60% 还是够不着，就临时把目标降到 30%：

```bash
kubectl patch hpa g12-backend-hpa -n enterprise-g12 --type=merge \
  -p '{"spec":{"metrics":[{"type":"Resource","resource":{"name":"cpu","target":{"type":"Utilization","averageUtilization":30}}}]}}'
```

> **演示完必须改回 60%**，否则和仓库里的 `09-hpa.yaml` 对不上，验收时会被抓：
>
> ```bash
> kubectl patch hpa g12-backend-hpa -n enterprise-g12 --type=merge \
>   -p '{"spec":{"metrics":[{"type":"Resource","resource":{"name":"cpu","target":{"type":"Utilization","averageUtilization":60}}}]}}'
> kubectl get hpa -n enterprise-g12 g12-backend-hpa
> ```

## 3. 三种现场翻车样子 + 怎么解释

| 现象 | 原因 | 口径 / 动作 |
| --- | --- | --- |
| `REPLICAS` 一直是 `2` | 没压到 120m 阈值 | 加并发 Pod，或临时降到 30%（方案 B） |
| `TARGETS` 显示 `<unknown>/60%` | 没装 / 没起 metrics-server，或 Pod 刚建还没采集到 | 先 `kubectl top pods -n enterprise-g12`；有数再看 HPA，没数先装 metrics-server |
| 涨到 3 之后 5 分钟又缩回 2 | 缩容冷却（stabilization 60s + 默认 5 分钟窗口） | **属正常行为**，说明扩容已经成功过；看 Events 里的 `SuccessfulRescale` |

查事件：

```bash
kubectl describe hpa g12-backend-hpa -n enterprise-g12 | tail -20
kubectl get events -n enterprise-g12 --sort-by=.lastTimestamp | tail -15
```

## 4. 前置条件（没有这个 HPA 永远 `<unknown>`）

- metrics-server 已安装且 `kubectl top pods` 有数；
- backend Deployment 必须写 `resources.requests.cpu`（本仓库 `06-backend.yaml` 里是 `200m`）；
- Pod 处于 Ready，否则不会被统计。
