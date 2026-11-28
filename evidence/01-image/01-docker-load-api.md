# 加载后端镜像 g12-api:v1

| 项 | 内容 |
|---|---|
| 图片 | `01-docker-load-api.png` |
| 所属目录 | `evidence/01-image/` |
| 采集人 | C |
| 采集时间 | 2026-11-28 13:50（Asia/Shanghai） |
| 环境 | k3s v1.28.2+k3s1 单节点（兜底复刻） / namespace `enterprise-g12` |
| 关联文档 | `docs/runbook.md §6` |

## 对应命令

```bash
docker load -i g12-api-v1.tar
docker images | grep g12
```

## 图上应当看得出

最后一行必须是 `Loaded image: g12-api:v1`；`docker images` 里能看到 `g12-api  v1`。若显示的是 `g12-api:v2` 或 tag 为 `latest`，说明拿错包，停下找 B 重发。

## 实际输出（截取）

```text
受限说明：g12-api:v1 的 tar 由 B 侧构建导出，load 进节点的命令过程未单独留存截图；节点 containerd 内 g12-api:v1 的存在性见 03-minikube-image-ls.md 清单（IMAGE ID 62ff14af849a7，tag v1，非 latest）。
```

## 观察结论

后端镜像 g12-api:v1 已在节点 containerd 可见、tag 非 latest；load 过程截图缺，镜像存在性由 03 清单佐证。

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
