# 变更前：v1 基线

| 项 | 内容 |
|---|---|
| 图片 | `01-baseline-v1.png` |
| 所属目录 | `evidence/05-release-rollback/` |
| 采集人 | B（k3s 兜底环境代采） |
| 采集时间 | 2026-11-28 17:31（Asia/Shanghai） |
| 环境 | k3s v1.28.2+k3s1 单节点 / namespace `enterprise-g12` |
| 关联文档 | `docs/operation-record.md OP-01` |

## 对应命令

```bash
kubectl get deploy g12-backend -n enterprise-g12
kubectl get pods -n enterprise-g12 -l app=g12-backend -o wide --show-labels
kubectl get rs -n enterprise-g12 -l app=g12-backend
kubectl rollout history deployment/g12-backend -n enterprise-g12
# 接口判据（v1 基线，预期无 apiVersion 字段）
curl -s -X POST http://127.0.0.1/api/auth/login -H 'Host: g12.course.local' \
  -H 'Content-Type: application/json' -d @/tmp/b.json | grep -o '"apiVersion":"v2"'
```

## 图上应当看得出

IMAGE=`g12-api:v1`，READY `2/2`，AVAILABLE `2`；两个后端 Pod 同属一个 ReplicaSet、Running、RESTARTS 0。这是后面发布、异常发布、回滚四次操作的对照基线。

## 实际输出（截取）

### baseline-v1-deploy

```text
NAME          READY   UP-TO-DATE   AVAILABLE   AGE
g12-backend   2/2     2            2           5h1m
```

### baseline-v1-pods

```text
NAME                           READY   STATUS    RESTARTS   AGE    IP            NODE  LABELS
g12-backend-5fc9b977d7-q9qbh   1/1     Running   0          3m4s   10.42.0.171  s      app=g12-backend,pod-template-hash=5fc9b977d7,tier=backend
g12-backend-5fc9b977d7-vplx6   1/1     Running   0          113s   10.42.0.172  s      app=g12-backend,pod-template-hash=5fc9b977d7,tier=backend
```

### baseline-v1-replicasets

```text
NAME                     DESIRED   CURRENT   READY   AGE
g12-backend-5fc9b977d7   2         2         2       3m4s
g12-backend-7677cf4777   0         0         0       5h1m
g12-backend-dfd8ff96d     0         0         0       4h32m
g12-backend-65864b7647    0         0         0       4h24m
g12-backend-567dbb9d45   0         0         0       4h3m
g12-backend-5ddbf66778    0         0         0       4h7m
g12-backend-7585dd6b86    0         0         0       47m
```

### baseline-v1-image

```text
g12-backend-5fc9b977d7-q9qbh   docker.io/library/g12-api:v1
g12-backend-5fc9b977d7-vplx6     docker.io/library/g12-api:v1
```

### baseline-v1-apiversion（接口判据）

```text
# grep '"apiVersion":"v2"'  login 命中=False  me 命中=False  （v1 基线预期均为 False）
```

## 观察结论

v1 基线 Deployment 2 副本 Ready，对应 ReplicaSet `5fc9b977d7` 在位；登录与 `/me` 响应均不含 `apiVersion:"v2"` 字段，作为发布前稳定参照版本。

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图；接口响应里的 JWT 已脱敏。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
