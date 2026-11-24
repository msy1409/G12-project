# 05-release-rollback —— 发布、异常发布、回滚、手动扩缩容

**证明**：A7（发布、回滚、手动 scale 记录齐全）。四次操作各记“变更前 / 中 / 后”三态，文字版在 `docs/operation-record.md`，本目录只放图。

| 顺序 | 文件 | 对应操作 | 关键判据 |
|---|---|---|---|
| 1 | `01-baseline-v1.png` | 基线 | IMAGE `g12-api:v1`、READY `2/2`、history 只有 rev1 |
| 2 | `02-load-v2.png` | 备 v2 | `minikube image ls` 里 v1、v2 并存 |
| 3 | `03-rollout-v2.png` | OP-01 发布 | `successfully rolled out` |
| 4 | `04-rollout-history.png` | OP-01 事后 | rev2 出现、两个 ReplicaSet |
| 5 | `05-bad-image-v999.png` | OP-02 异常发布 | 新 Pod `ErrImageNeverPull`，**旧 Pod 仍 Running** |
| 6 | `06-rollout-undo.png` | OP-03 回滚 | 镜像回到 **v2**（不是 v1） |
| 7 | `07-scale-3.png` | OP-04 扩容 | 3 个 Pod Ready |
| 8 | `08-scale-2.png` | OP-04 缩容 | 回到 2 个 |

**三态怎么写**（评委会逐条看）：变更前贴 `get deploy`；变更中贴 `set image` + `rollout status`；变更后贴 `get deploy` + `get pods` + `rollout history`。**没有 `rollout history` 就等于没证明版本真的换了。**

**两个必踩的坑，先记在这里**

1. `rollout undo` 退的是**上一层**。v999 的上一层是 v2，所以回滚后看到 `g12-api:v2` 是对的；要回 v1 得 `--to-revision=1`。
2. 集群里有 HPA（min2/max4）时，手动 `scale` 到 3 会在 15s 内被 HPA 改回 2。要稳定演示就先 `kubectl delete hpa g12-backend-hpa -n enterprise-g12`，演示完再 apply 回来 —— 并在 md 里写明这一步，否则会被问“你的 scale 怎么又变回去了”。
