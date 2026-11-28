# metrics 有数据

| 项 | 内容 |
|---|---|
| 图片 | `03-top-pods.png` |
| 所属目录 | `evidence/06-hpa/` |
| 采集人 | B（k3s 兜底环境代采） |
| 采集时间 | 2026-11-28 16:58（Asia/Shanghai） |
| 环境 | k3s v1.28.2+k3s1 单节点 / namespace `enterprise-g12` |
| 关联文档 | `docs/runbook.md §2` |

## 对应命令

```bash
kubectl top node
kubectl top pod -n enterprise-g12
```

## 图上应当看得出

能打出 CPU/memory 数值（不是 `Metrics API not available`）。
metrics-server 刚启用时要等 1–2 分钟；还是没数就 `kubectl logs -n kube-system deploy/metrics-server` 看报错。

## 实际输出（截取）

```text
NAME   CPU(cores)   CPU%   MEMORY(bytes)   MEMORY%
s      206m         6%     5344Mi          67%
---
NAME                            CPU(cores)   MEMORY(bytes)
g12-backend-7585dd6b86-mspgg    4m           224Mi
g12-backend-7585dd6b86-xwzk4    4m           228Mi
g12-frontend-74c48b9d66-tznxw   1m           3Mi
g12-frontend-74c48b9d66-vhmb4   1m           3Mi
g12-mysql-5cb9987d-t4h5v        18m          460Mi
```

## 观察结论

metrics-server 正常从 kubelet 采集指标，节点与各业务 Pod 的 CPU 核数/百分比、内存字节数均有真实数值，HPA 的 CPU 利用率分母（requests.cpu=200m）有数据源支撑。

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
