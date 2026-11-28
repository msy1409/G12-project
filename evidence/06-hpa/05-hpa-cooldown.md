# 停压测后回落

| 项 | 内容 |
|---|---|
| 图片 | `05-hpa-cooldown.png` |
| 所属目录 | `evidence/06-hpa/` |
| 采集人 | B（k3s 兜底环境代采） |
| 采集时间 | 2026-11-28 17:04（Asia/Shanghai） |
| 环境 | k3s v1.28.2+k3s1 单节点 / namespace `enterprise-g12` |
| 关联文档 | `docs/runbook.md §7` |

## 对应命令

```bash
# Ctrl+C 结束压测，继续看
kubectl get hpa -n enterprise-g12 -w
kubectl get pods -n enterprise-g12 -l app=g12-backend
kubectl top pod -n enterprise-g12 -l app=g12-backend
```

## 图上应当看得出

缩容冷却窗口过后 REPLICAS 回落到 2，两个后端 Pod 保持 1/1 Running。
这张和 04 配对，证明“能上也能下”。

## 实际输出（截取）

```text
NAME              REFERENCE                TARGETS   MINPODS   MAXPODS   REPLICAS   AGE
g12-backend-hpa   Deployment/g12-backend   2%/60%    2         4         2          18m

---
NAME                           READY   STATUS    RESTARTS   AGE    IP            NODE
g12-backend-7585dd6b86-mspgg   1/1     Running   0          20m    10.42.0.165   s
g12-backend-7585dd6b86-xwzk4   1/1     Running   0          19m    10.42.0.166   s

---
NAME                           CPU(cores)   MEMORY(bytes)
g12-backend-7585dd6b86-mspgg    4m           229Mi
g12-backend-7585dd6b86-xwzk4    4m           230Mi
```

## 观察结论

压测停止后 CPU 利用率回落到 2%，低于 60% 目标；经过缩容稳定窗口，HPA 按每 60s 减 1 的策略从 4 → 3 → 2 缩回最小值，两个后端 Pod 保持就绪，多副本在负载消退后自动释放。

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
