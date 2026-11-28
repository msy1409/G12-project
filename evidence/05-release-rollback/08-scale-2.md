# 手动缩回 2

| 项 | 内容 |
|---|---|
| 图片 | `08-scale-2.png` |
| 所属目录 | `evidence/05-release-rollback/` |
| 采集人 | B（k3s 兜底环境代采） |
| 采集时间 | 2026-11-28 17:19（Asia/Shanghai） |
| 环境 | k3s v1.28.2+k3s1 单节点 / namespace `enterprise-g12` |
| 关联文档 | `docs/operation-record.md OP-04` |

## 对应命令

```bash
kubectl scale deployment/g12-backend --replicas=2 -n enterprise-g12
kubectl get deploy g12-backend -n enterprise-g12
kubectl get pods -n enterprise-g12 -l app=g12-backend
kubectl top pod -n enterprise-g12 -l app=g12-backend
```

## 图上应当看得出

回到 2 个 Pod（第三个进入 Terminating 后消失），`kubectl get deploy` 的 READY `2/2`。

## 实际输出（截取）

```text
NAME          READY   UP-TO-DATE   AVAILABLE   AGE
g12-backend   2/2     2            2           4h42m

NAME                           READY   STATUS    RESTARTS   AGE    IP            NODE
g12-backend-7585dd6b86-mspgg   1/1     Running   0          29m    10.42.0.165   s
g12-backend-7585dd6b86-xwzk4   1/1     Running   0          27m    10.42.0.166   s

NAME                           CPU(cores)   MEMORY(bytes)
g12-backend-7585dd6b86-mspgg    6m           229Mi
g12-backend-7585dd6b86-xwzk4     4m           231Mi
```

## 观察结论

手动 `scale --replicas=2` 后第三个 Pod 被回收，Deployment 回到 `2/2` Available，剩余两个后端 Pod 保持就绪；手动扩容与缩容两个方向均在本集群验证通过。

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 缩回后已重新 apply HPA，副本交由 HPA 按 CPU 利用率自动管理。
