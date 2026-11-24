# 01-image —— 镜像产物与入集群

**证明**：A1（两个镜像能构建、多阶段、非 `latest`、无敏感信息）+ 镜像确实进到了 minikube 节点。

**前置**：拿到 A 的 `g12-web-v1.tar`、B 的 `g12-api-v1.tar`。

| 顺序 | 文件 | 采集命令 |
|---|---|---|
| 1 | `01-docker-load-api.png` | `docker load -i g12-api-v1.tar` |
| 2 | `02-docker-load-web.png` | `docker load -i g12-web-v1.tar` |
| 3 | `03-minikube-image-ls.png` | `minikube image load` ×2 + `minikube image ls \| grep g12` |

**判据**：`minikube image ls` 里 `g12-api:v1` 和 `g12-web:v1` 都在；tag 不是 `latest`。

**常见坑**：只 `docker load` 没 `minikube image load` → Pod `ErrImageNeverPull`。镜像是 A/B 打的，C 只负责“装进节点”，构建日志由 A/B 各自提供。
