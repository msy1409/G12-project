# 发布后版本历史

| 项 | 内容 |
|---|---|
| 图片 | `04-rollout-history.png` |
| 所属目录 | `evidence/05-release-rollback/` |
| 采集人 | B（k3s 兜底环境代采） |
| 采集时间 | 2026-11-28 17:35（Asia/Shanghai） |
| 环境 | k3s v1.28.2+k3s1 单节点 / namespace `enterprise-g12` |
| 关联文档 | `docs/operation-record.md OP-01` |

## 对应命令

```bash
kubectl rollout history deployment/g12-backend -n enterprise-g12
kubectl get rs -n enterprise-g12 -l app=g12-backend
```

## 图上应当看得出

`rollout history` 列出历次修订版本；ReplicaSet 中当前版本 DESIRED/CURRENT/READY=2，历史版本 DESIRED=0。旧 RS 没被删是 `revisionHistoryLimit` 的默认行为，也是 undo 能生效的前提。

## 实际输出（截取）

### after-v2-history

```text
deployment.apps/g12-backend
REVISION  CHANGE-CAUSE
1         <none>
2         <none>
3         <none>
5         <none>
6         <none>
8         <none>
9         <none>
```

### after-v2-replicasets

```text
NAME                     DESIRED   CURRENT   READY   AGE
g12-backend-7585dd6b86   2         2         2       51m
g12-backend-7677cf4777   0         0         0       5h4m
g12-backend-dfd8ff96d    0         0         0       4h36m
g12-backend-65864b7647   0         0         0       4h27m
g12-backend-567dbb9d45   0         0         0       4h6m
g12-backend-5ddbf66778   0         0         0       4h10m
g12-backend-5fc9b977d7   0         0         0       6m22s
```

## 观察结论

rollout history 保留了 v1、v2 等历次修订记录，当前 ReplicaSet `7585dd6b86` 承载 2 个就绪副本，历史 RS 缩到 0 但未删除，可据此定位并回退到任一历史版本。

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
