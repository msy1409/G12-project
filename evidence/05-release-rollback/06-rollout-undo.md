# 回滚

| 项 | 内容 |
|---|---|
| 图片 | `06-rollout-undo.png` |
| 所属目录 | `evidence/05-release-rollback/` |
| 采集人 | B（k3s 兜底环境代采） |
| 采集时间 | 2026-11-28 17:37（Asia/Shanghai） |
| 环境 | k3s v1.28.2+k3s1 单节点 / namespace `enterprise-g12` |
| 关联文档 | `docs/operation-record.md OP-03` |

## 对应命令

```bash
kubectl rollout undo deployment/g12-backend -n enterprise-g12
kubectl rollout status deployment/g12-backend -n enterprise-g12 --timeout=300s
kubectl get deploy g12-backend -n enterprise-g12
kubectl rollout history deployment/g12-backend -n enterprise-g12
# 接口判据：回滚后应仍命中 "apiVersion":"v2"（undo 退回上一层 v2，不是 v1）
curl -s http://127.0.0.1/api/actuator/health -H 'Host: g12.course.local'
```

## 图上应当看得出

IMAGE 从 `v999` 回到 **`g12-api:v2`**（undo 默认退回上一层，不是 v1）；AVAILABLE 回到 2；健康检查 HTTP 200；登录与 `/me` 响应 `grep` 仍命中 `"apiVersion":"v2"`。

## 实际输出（截取）

### undo-after-deploy

```text
NAME          READY   UP-TO-DATE   AVAILABLE   AGE
g12-backend   2/2     2            2           5h7m
```

### undo-after-pods

```text
NAME                           READY   STATUS    RESTARTS   AGE     IP            NODE
g12-backend-7585dd6b86-pj429   1/1     Running   0          5m53s   10.42.0.173  s
g12-backend-7585dd6b86-9kzqb   1/1     Running   0          4m42s   10.42.0.174  s
```

### undo-after-image

```text
g12-backend-7585dd6b86-pj429   docker.io/library/g12-api:v2
g12-backend-7585dd6b86-9kzqb   docker.io/library/g12-api:v2
```

### undo-after-health / apiversion

```text
# health: HTTP 200
# grep '"apiVersion":"v2"'  login 命中=True  me 命中=True  （回滚到 v2 预期均为 True）
```

## 观察结论

`rollout undo` 把 Pod 模板退回到上一次可用版本 v2，两个 Pod 重新 Ready、健康检查 200，接口仍返回 `apiVersion:"v2"`，说明回滚链路有效、服务恢复正常。

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图；接口响应里的 JWT 已脱敏。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
