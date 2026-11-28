# 命名空间事件流

| 项 | 内容 |
|---|---|
| 图片 | `05-events.png` |
| 所属目录 | `evidence/04-logs-events/` |
| 采集人 | B（k3s 兜底环境代采） |
| 采集时间 | 2026-11-28 17:40（Asia/Shanghai） |
| 环境 | k3s v1.28.2+k3s1 单节点 / namespace `enterprise-g12` |
| 关联文档 | `docs/runbook.md §12` |

## 对应命令

```bash
kubectl get events -n enterprise-g12 --sort-by=.lastTimestamp | tail -25
```

## 图上应当看得出

按时间排，能看到 `Scheduled → Pulled → Created → Started` 完整链路、`SuccessfulCreate`（RS 建 Pod）；`ScalingReplicaSet` 扩缩记录。出现 `FailedScheduling`（资源不够）、`FailedMount`（ConfigMap/PVC 问题）、反复 `Unhealthy`（探针失败）要能解释。

## 实际输出（截取）

```text
8m48s  Normal   Created              pod/...7585dd6b86-pj429   Created container backend
8m20s  Warning  Unhealthy            pod/...7585dd6b86-pj429   Startup probe failed: ... connection refused
8m8s   Warning  Unhealthy            pod/...7585dd6b86-pj429   Startup probe failed: ... context deadline exceeded
7m39s  Normal   ScalingReplicaSet    deployment/g12-backend     Scaled up replica set g12-backend-7585dd6b86 to 2 from 1
7m39s  Normal   SuccessfulCreate     replicaset/...7585dd6b86  Created pod: ...7585dd6b86-9kzqb
7m37s  Normal   Pulled               pod/...7585dd6b86-9kzqb   Container image "g12-api:v2" already present on machine
7m36s  Normal   Started              pod/...7585dd6b86-9kzqb   Started container backend
6m48s  Warning  Unhealthy            pod/...7585dd6b86-9kzqb   Startup probe failed: ... connection refused
5m30s  Normal   ScalingReplicaSet    deployment/g12-backend     Scaled up replica set g12-backend-775c8875b5 to 1
5m30s  Normal   SuccessfulCreate     replicaset/...775c8875b5  Created pod: ...775c8875b5-4kbwm
4m     Normal   BackOff              pod/...775c8875b5-4kbwm   Back-off pulling image "g12-api:v999"
4m     Warning  Failed               pod/...775c8875b5-4kbwm   Error: ImagePullBackOff
3m38s  Normal   ScalingReplicaSet    deployment/g12-backend     Scaled down replica set g12-backend-775c8875b5 to 0 from 1
3m38s  Normal   SuccessfulDelete     replicaset/...775c8875b5  Deleted pod: ...775c8875b5-4kbwm
3m21s  Warning  Failed               pod/...775c8875b5-4kbwm   Failed to pull image "g12-api:v999": ... 403 Forbidden
3m21s  Warning  Failed               pod/...775c8875b5-4kbwm   Error: ErrImagePull
```

## 观察结论

事件链完整覆盖本次发布、异常发布与回滚：v2 两个 Pod 经 Pulled/Created/Started 进入就绪；启动期 Startup 探针短暂探不到端口（慢启动，被 startupProbe 容忍）；v999 新 RS 因镜像拉取失败进入 ImagePullBackOff，回滚后该 RS 缩到 0 并删除。全程无 `FailedScheduling`、`FailedMount` 或 Pod 驱逐告警。

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
