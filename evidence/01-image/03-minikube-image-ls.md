# 镜像进入 minikube 节点

| 项 | 内容 |
|---|---|
| 图片 | `03-minikube-image-ls.png` |
| 所属目录 | `evidence/01-image/` |
| 采集人 | C |
| 采集时间 | 【填：YYYY-MM-DD HH:MM（Asia/Shanghai）】 |
| 环境 | minikube / namespace `enterprise-g12` |
| 关联文档 | `docs/runbook.md §6` |

## 对应命令

```bash
minikube image load g12-api:v1
minikube image load g12-web:v1
minikube image ls | grep g12
```

## 图上应当看得出

`minikube image ls` 至少两行：`docker.io/library/g12-api:v1`、`docker.io/library/g12-web:v1`。
**只 docker load 不 minikube image load 是最常见的坑**：镜像只在 VM 的 docker 里，节点内没有，Pod 会 `ErrImageNeverPull`。

## 实际输出（截取）

【贴：与图一致的文本输出；长输出只留首行 + 结论行，中间用 ... 省略】

## 观察结论

【填：一句话结论，写“什么现象 + 说明什么”，不要写“运行成功”这种空话】

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
