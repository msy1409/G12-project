# 镜像进入 minikube 节点

| 项 | 内容 |
|---|---|
| 图片 | `03-minikube-image-ls.png` |
| 所属目录 | `evidence/01-image/` |
| 采集人 | B（k3s 兜底复刻环境） |
| 采集时间 | 2026-11-28 13:39（Asia/Shanghai） |
| 环境 | k3s v1.28.2+k3s1 单节点 / namespace `enterprise-g12` |
| 关联文档 | `docs/runbook.md §6` |

## 对应命令

```bash
# k3s 内置 containerd，经 k3s socket 导入后用 crictl 列出节点内镜像
/var/lib/rancher/k3s/data/current/bin/crictl \
  --runtime-endpoint unix:///run/k3s/containerd/containerd.sock images | grep g12
```

## 图上应当看得出

节点 containerd 内至少两行：`docker.io/library/g12-api:v1`、`docker.io/library/g12-web:v1`，tag 均为 `v1`（非 `latest`）。
**只在 VM docker 里 load、没进节点 containerd 是最常见的坑**：镜像不在节点内，Pod 会 `ErrImageNeverPull` / `ImagePullBackOff`。

## 实际输出（截取）

```text
IMAGE                                           TAG                    IMAGE ID            SIZE
docker.io/library/g12-api                       v1                     62ff14af849a7       145MB
docker.io/library/g12-api                       v2                     9a58ef602f2d2       145MB
docker.io/library/g12-web                       v1                     6dbbc9b1ef8c7       51.3MB
```

## 观察结论

`g12-api:v1`、`g12-web:v1` 均已注册进 k3s 节点的 containerd（CRI 索引可见），后端/前端 Pod 可在本地镜像来源下正常拉起，tag 非 `latest`；`g12-api:v2` 为发布回滚演示用版本。

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
