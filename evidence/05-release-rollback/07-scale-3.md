# 手动扩容到 3

| 项 | 内容 |
|---|---|
| 图片 | `07-scale-3.png` |
| 所属目录 | `evidence/05-release-rollback/` |
| 采集人 | B（k3s 兜底环境代采） |
| 采集时间 | 2026-11-28 17:18（Asia/Shanghai） |
| 环境 | k3s v1.28.2+k3s1 单节点 / namespace `enterprise-g12` |
| 关联文档 | `docs/operation-record.md OP-04` |

## 对应命令

```bash
# 集群已有 HPA（min2/max4），手动 scale 会在 ~15s 内被 HPA 拉回 2；
# 演示手动扩缩容前先临时删除 HPA，演示完再 apply 回来。
kubectl delete hpa g12-backend-hpa -n enterprise-g12
kubectl scale deployment/g12-backend --replicas=3 -n enterprise-g12
kubectl get deploy g12-backend -n enterprise-g12
kubectl get pods -n enterprise-g12 -l app=g12-backend -o wide
kubectl top pod -n enterprise-g12 -l app=g12-backend
```

## 图上应当看得出

3 个 Pod 全 Running，deploy 的 READY `3/3`。
新 Pod 由 startupProbe 容忍慢启动，就绪后纳入 Endpoints。

## 实际输出（截取）

```text
NAME          READY   UP-TO-DATE   AVAILABLE   AGE
g12-backend   3/3     3            3           4h42m

NAME                           READY   STATUS    RESTARTS   AGE    IP            NODE
g12-backend-7585dd6b86-mspgg   1/1     Running   0          28m    10.42.0.165   s
g12-backend-7585dd6b86-xwzk4   1/1     Running   0          27m    10.42.0.166   s
g12-backend-7585dd6b86-slv8f  1/1     Running   0          79s    10.42.0.170   s

NAME                           CPU(cores)   MEMORY(bytes)
g12-backend-7585dd6b86-mspgg    6m           229Mi
g12-backend-7585dd6b86-slv8f   156m         174Mi
g12-backend-7585dd6b86-xwzk4     4m           231Mi
```

## 观察结论

删除 HPA 后手动 `scale --replicas=3`，第三个后端 Pod 启动并通过 startup/readiness 探针就绪，Deployment 达到 `3/3` Available，三个 Pod 均纳入流量后端，证明手动多副本调度在本集群可落地。

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 演示结束后已重新 `kubectl apply -f k8s/09-hpa.yaml` 恢复 HPA（见 `06-hpa/` 证据）。
