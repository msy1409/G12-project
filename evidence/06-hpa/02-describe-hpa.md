# HPA 详情与扩缩容规则

| 项 | 内容 |
|---|---|
| 图片 | `02-describe-hpa.png` |
| 所属目录 | `evidence/06-hpa/` |
| 采集人 | B（k3s 兜底环境代采） |
| 采集时间 | 2026-11-28 16:58（Asia/Shanghai） |
| 环境 | k3s v1.28.2+k3s1 单节点 / namespace `enterprise-g12` |
| 关联文档 | `docs/runbook.md §7` |

## 对应命令

```bash
kubectl describe hpa g12-backend-hpa -n enterprise-g12
```

## 图上应当看得出

Events 里能看到 `SuccessfulRescale` 记录；Metrics 段有当前 CPU 值；Conditions 中 `ScalingActive=True / ValidMetricFound`。

## 实际输出（截取）

```text
Name:                                                  g12-backend-hpa
Namespace:                                             enterprise-g12
Reference:                                             Deployment/g12-backend
Metrics:                                               ( current / target )
  resource cpu on pods  (as a percentage of request):  2% (4m) / 60%
Min replicas:                                          2
Max replicas:                                          4
Behavior:
  Scale Up:
    Stabilization Window: 60 seconds
    Select Policy: Max
    Policies:
      - Type: Pods  Value: 1  Period: 30 seconds
  Scale Down:
    Stabilization Window: 60 seconds
    Select Policy: Max
    Policies:
      - Type: Pods  Value: 1  Period: 60 seconds
Deployment pods:    2 current / 2 desired
Conditions:
  Type            Status  Reason            Message
  ----            ------  ------            -------
  AbleToScale     True    ReadyForNewScale  recommended size matches current size
  ScalingActive   True    ValidMetricFound  the HPA was able to successfully calculate a replica count from cpu resource utilization
  ScalingLimited  True    TooFewReplicas    the desired replica count is less than the minimum replica count
Events:
  Type     Reason                        Age     From                       Message
  Normal   SuccessfulRescale             12m     horizontal-pod-autoscaler  New size: 2; reason: Current number of replicas below Spec.MinReplicas
  Normal   SuccessfulRescale             8m13s   horizontal-pod-autoscaler  New size: 3; reason: cpu resource utilization (percentage of request) above target
  Normal   SuccessfulRescale             7m43s   horizontal-pod-autoscaler  New size: 4; reason: cpu resource utilization (percentage of request) above target
  Normal   SuccessfulRescale             5m13s   horizontal-pod-autoscaler  New size: 3; reason: All metrics below target
  Normal   SuccessfulRescale             4m13s   horizontal-pod-autoscaler  New size: 2; reason: All metrics below target
```

## 观察结论

HPA 指标计算有效（`ValidMetricFound`），事件流完整记录了一次自动扩缩容闭环：低于最小值先补到 2，CPU 超阈值依次扩到 3、4，负载回落后依次缩回 3、2；扩容每 30s 加 1、缩容每 60s 减 1，与 `k8s/09-hpa.yaml` 的 behavior 一致。

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 早期 `FailedGetResourceMetric` 是新 Pod 尚未 Ready、指标暂未上报的瞬时告警，Ready 后即恢复。
