# HPA 初始状态

| 项 | 内容 |
|---|---|
| 图片 | `01-hpa-initial.png` |
| 所属目录 | `evidence/06-hpa/` |
| 采集人 | B（k3s 兜底环境代采） |
| 采集时间 | 2026-11-28 16:58（Asia/Shanghai） |
| 环境 | k3s v1.28.2+k3s1 单节点 / namespace `enterprise-g12` |
| 关联文档 | `docs/runbook.md §7` |

## 对应命令

```bash
kubectl get hpa -n enterprise-g12
kubectl get deploy g12-backend -n enterprise-g12
```

## 图上应当看得出

TARGETS 形如 `<n>%/60%`（不是 `<unknown>`），MINPODS 2 MAXPODS 4，REPLICAS 2。
`<unknown>` = backend 没写 `resources.requests.cpu`，HPA 算不出百分比。

## 实际输出（截取）

```text
NAME              REFERENCE                TARGETS   MINPODS   MAXPODS   REPLICAS   AGE
g12-backend-hpa   Deployment/g12-backend   2%/60%    2         4         2          13m
```

## 观察结论

metrics-server 已出数，HPA 能从 CPU 利用率算出目标副本数：当前 2%/60% 低于阈值，副本稳定在最小值 2，MINPODS 2 / MAXPODS 4 与 `k8s/09-hpa.yaml` 一致。

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
