# metrics-server 就绪与 Metrics API 注册

| 项 | 内容 |
|---|---|
| 图片 | `08-metrics-server.png` |
| 所属目录 | `evidence/03-k8s-deploy/` |
| 采集人 | B（k3s 兜底环境代采） |
| 采集时间 | 2026-11-28 16:58（Asia/Shanghai） |
| 环境 | k3s v1.28.2+k3s1 单节点 / namespace `kube-system`、`enterprise-g12` |
| 关联文档 | `docs/runbook.md §2` |

## 对应命令

```bash
kubectl get pods -n kube-system -l k8s-app=metrics-server -o wide
kubectl get apiservices | grep -i metrics
kubectl top node
```

## 图上应当看得出

metrics-server Pod `1/1 Running`；APIService `v1beta1.metrics.k8s.io` 状态 `True`；`kubectl top node` 能打出 CPU/内存数值。

## 实际输出（截取）

```text
NAME                             READY   STATUS    RESTARTS   AGE   IP            NODE
metrics-server-87dcdc995-g5ht9   1/1     Running   0          33m   10.42.0.164   s
---
v1beta1.metrics.k8s.io   kube-system/metrics-server   True   38m
```

## 观察结论

metrics-server v0.7.2 正常运行，聚合 API `metrics.k8s.io/v1beta1` 已注册为 Available（True），节点/Pod 指标可被 HPA 与 `kubectl top` 查询，补齐了 HPA 的指标数据源。

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 修复要点：探针补 `scheme: HTTPS`（metrics-server 仅监听 4443 TLS），补齐 `nodes/metrics`、`subjectaccessreviews` 权限，并注册 APIService。
