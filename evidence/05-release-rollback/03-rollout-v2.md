# 滚动更新到 v2

| 项 | 内容 |
|---|---|
| 图片 | `03-rollout-v2.png` |
| 所属目录 | `evidence/05-release-rollback/` |
| 采集人 | B（k3s 兜底环境代采） |
| 采集时间 | 2026-11-28 17:35（Asia/Shanghai） |
| 环境 | k3s v1.28.2+k3s1 单节点 / namespace `enterprise-g12` |
| 关联文档 | `docs/operation-record.md OP-01` |

## 对应命令

```bash
kubectl set image deployment/g12-backend backend=g12-api:v2 -n enterprise-g12
kubectl rollout status deployment/g12-backend -n enterprise-g12 --timeout=300s
kubectl get pods -n enterprise-g12 -l app=g12-backend -o wide
# v2 生效判据：登录与 /me 响应应出现 "apiVersion":"v2"
curl -s -X POST http://127.0.0.1/api/auth/login -H 'Host: g12.course.local' \
  -H 'Content-Type: application/json' -d @/tmp/b.json | grep -o '"apiVersion":"v2"'
```

## 图上应当看得出

`rollout status` 成功完成；Pod 列表全 `Running`、RESTARTS=0；两个 Pod 镜像均为 `g12-api:v2`；登录与 `/me` 响应里 `grep` 命中 `"apiVersion":"v2"`。只有 tag 变、没有 `apiVersion` 命中，就不算发布了 v2。

## 实际输出（截取）

### after-v2-deploy

```text
NAME          READY   UP-TO-DATE   AVAILABLE   AGE
g12-backend   2/2     2            2           5h4m
```

### after-v2-pods

```text
NAME                           READY   STATUS    RESTARTS   AGE     IP            NODE  LABELS
g12-backend-7585dd6b86-pj429   1/1     Running   0          2m55s   10.42.0.173  s      app=g12-backend,pod-template-hash=7585dd6b86,tier=backend
g12-backend-7585dd6b86-9kzqb   1/1     Running   0          104s    10.42.0.174  s      app=g12-backend,pod-template-hash=7585dd6b86,tier=backend
```

### after-v2-image

```text
g12-backend-7585dd6b86-pj429   docker.io/library/g12-api:v2
g12-backend-7585dd6b86-9kzqb   docker.io/library/g12-api:v2
```

### after-v2-apiversion（接口判据）

```text
# grep '"apiVersion":"v2"'  login 命中=True  me 命中=True  （v2 预期均为 True）
--- login 响应（token 已脱敏）---
{"code":200,"message":"ok","data":{"token":"eyJ…(脱敏)","user":{...,"apiVersion":"v2"}}}
--- /me 响应 ---
{"code":200,"message":"ok","data":{...,"apiVersion":"v2"}}
```

## 观察结论

`set image` 到 v2 后新 ReplicaSet `7585dd6b86` 的两个 Pod 依次就绪、旧版本逐步下线，滚动更新完成且服务未中断；登录与 `/me` 响应均返回 `apiVersion:"v2"`，与 v1 基线（无该字段）形成前后对比，证明新版本确实在服务。

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图；接口响应里的 JWT 已脱敏。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
