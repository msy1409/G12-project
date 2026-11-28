# kubectl apply --dry-run=client 全过

| 项 | 内容 |
|---|---|
| 图片 | `02-dry-run.png` |
| 所属目录 | `evidence/03-k8s-deploy/` |
| 采集人 | B（k3s 兜底环境代采） |
| 采集时间 | 2026-11-28 17:58（Asia/Shanghai） |
| 环境 | k3s v1.28.2+k3s1 单节点 / namespace `enterprise-g12` |
| 关联文档 | `docs/runbook.md §7` |

## 对应命令

```bash
kubectl apply --dry-run=client -f k8s/
```

## 图上应当看得出

每个文件一行 `... configured (dry run)` 或 `... created (dry run)`，**不能有任何 `error:`**。
这是 A3 评分点的直接证据，清单全带上，不要只截最后一行。

## 实际输出（截取，raw：`_raw/03-k8s-deploy/02-dry-run.txt`）

```text
namespace/enterprise-g12 configured (dry run)
configmap/g12-config configured (dry run)
secret/g12-secret configured (dry run)
storageclass.storage.k8s.io/g12-local configured (dry run)
persistentvolume/g12-mysql-pv configured (dry run)
persistentvolumeclaim/g12-mysql-pvc configured (dry run)
deployment.apps/g12-mysql configured (dry run)
service/g12-mysql configured (dry run)
deployment.apps/g12-backend configured (dry run)
service/g12-backend configured (dry run)
deployment.apps/g12-frontend configured (dry run)
service/g12-frontend configured (dry run)
ingress.networking.k8s.io/g12-ingress configured (dry run)
horizontalpodautoscaler.autoscaling/g12-backend-hpa configured (dry run)
```

## 观察结论

client 侧 dry-run 通过，各 Deployment/Service/PVC/Ingress/HPA 清单的字段与引用合法，可直接 apply 到集群。

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
