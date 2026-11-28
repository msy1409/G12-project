# 异常发布 v999（故意的）

| 项 | 内容 |
|---|---|
| 图片 | `05-bad-image-v999.png` |
| 所属目录 | `evidence/05-release-rollback/` |
| 采集人 | B（k3s 兜底环境代采） |
| 采集时间 | 2026-11-28 17:36（Asia/Shanghai） |
| 环境 | k3s v1.28.2+k3s1 单节点 / namespace `enterprise-g12` |
| 关联文档 | `docs/operation-record.md OP-02` |

## 对应命令

```bash
kubectl set image deployment/g12-backend backend=g12-api:v999 -n enterprise-g12
kubectl get pods -n enterprise-g12 -l app=g12-backend -o wide
kubectl describe pod <Pending的pod> -n enterprise-g12
kubectl get events -n enterprise-g12 --sort-by=.lastTimestamp | tail -20
```

## 图上应当看得出

新 Pod `ErrImagePull` / `ImagePullBackOff`；**旧的 2 个 v2 Pod 仍 Running**（服务没断）。describe 的 Events 里必须有 `Failed to pull image "g12-api:v999"` / `ImagePullBackOff` —— 这就是根因证据。

## 实际输出（截取）

### bad-v999-pods

```text
NAME                           READY   STATUS             RESTARTS   AGE     IP            NODE
g12-backend-7585dd6b86-pj429   1/1     Running            0          4m42s   10.42.0.173  s
g12-backend-7585dd6b86-9kzqb   1/1     Running            0          3m31s   10.42.0.174  s
g12-backend-775c8875b5-4kbwm   0/1     ImagePullBackOff   0          82s     10.42.0.175  s
```

### bad-v999-deploy

```text
NAME          READY   UP-TO-DATE   AVAILABLE   AGE
g12-backend   2/2     1            2           5h6m
```

### bad-v999-describe（失败 Pod 关键段）

```text
Image:          g12-api:v999
State:          Waiting
  Reason:       ImagePullBackOff
Ready:          False

Events:
  Normal  Scheduled  85s   default-scheduler  Successfully assigned ... to s
  Normal  BackOff    59s   kubelet            Back-off pulling image "g12-api:v999"
  Warning Failed    59s   kubelet            Error: ImagePullBackOff
  Normal  Pulling    44s   kubelet            Pulling image "g12-api:v999"
  Warning Failed    10s   kubelet            Failed to pull image "g12-api:v999": failed to resolve reference ... : 403 Forbidden
  Warning Failed    10s   kubelet            Error: ErrImagePull
```

### bad-v999-events（尾部）

```text
87s   Normal   ScalingReplicaSet   deployment/g12-backend   Scaled up replica set g12-backend-775c8875b5 to 1
60s   Normal   BackOff             pod/...775c8875b5-4kbwm   Back-off pulling image "g12-api:v999"
60s   Warning  Failed              pod/...775c8875b5-4kbwm   Error: ImagePullBackOff
11s   Warning  Failed              pod/...775c8875b5-4kbwm   Failed to pull image "g12-api:v999": ... 403 Forbidden
11s   Warning  Failed              pod/...775c8875b5-4kbwm   Error: ErrImagePull
```

## 观察结论

v999 镜像本地不存在、远端也解析不到，新 Pod 卡在 `ImagePullBackOff/ErrImagePull`；而旧的两个 v2 Pod 继续 Running、AVAILABLE 仍为 2，说明错误版本只影响新 ReplicaSet，不会直接打断存量业务。

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
