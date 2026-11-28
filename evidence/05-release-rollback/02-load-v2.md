# v2 镜像就位

| 项 | 内容 |
|---|---|
| 图片 | `02-load-v2.png` |
| 所属目录 | `evidence/05-release-rollback/` |
| 采集人 | B（k3s 兜底环境代采） |
| 采集时间 | 2026-11-28 17:31（Asia/Shanghai） |
| 环境 | k3s v1.28.2+k3s1 单节点 / namespace `enterprise-g12` |
| 关联文档 | `docs/operation-record.md OP-01` |

## 对应命令

```bash
docker load -i g12-api-v2.tar
minikube image load g12-api:v2
minikube image ls | grep g12-api
```

## 图上应当看得出

`g12-api:v1` 和 `g12-api:v2` 同时存在（v1 不能删，回滚还要用）。
只有 v2 没有 v1 = 没法演示 undo，得重新 load v1。

## 实际输出（截取）

### 02-load-v2-images

```
docker.io/library/g12-api                       v1                     62ff14af849a7       145MB
docker.io/library/g12-api                       v2                     9a58ef602f2d2       145MB
docker.io/library/g12-web                       v1                     6dbbc9b1ef8c7       51.3MB
```

## 观察结论

g12-api:v2 镜像已导入 k3s 节点 containerd 并在 CRI 索引可见，可被新 Pod 本地拉取。

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
