# 加载前端镜像 g12-web:v1

| 项 | 内容 |
|---|---|
| 图片 | `02-docker-load-web.png`（终端过程截图；兜底环境本地终端模拟器无法拉起，load 结果以命令输出与 03 镜像清单佐证） |
| 所属目录 | `evidence/01-image/` |
| 采集人 | B（k3s 兜底复刻环境） |
| 采集时间 | 2026-11-28 18:55（Asia/Shanghai） |
| 环境 | k3s v1.28.2+k3s1 单节点（兜底复刻） / namespace `enterprise-g12` |
| 关联文档 | `docs/runbook.md §6` |

## 对应命令

```bash
docker load -i g12-web-v1.tar
docker images --format '{{.Repository}}:{{.Tag}}  {{.ID}}  {{.Size}}' | grep g12
```

## 图上应当看得出

`docker load` 末行为 `Loaded image: g12-web:v1`；`docker images` 中三个 g12 镜像并列，web 镜像 SIZE 约 106MB（CONTENT SIZE 约 51.3MB），tag 非 `latest`。

## 实际输出（截取）

```text
Loaded image: g12-web:v1

== docker images（g12 相关）==
g12-api:v2  8e0832416684  518MB
g12-web:v1  7bc085d06b71  106MB
g12-api:v1  8127b48ef7a0  518MB
```

完整文本见 `evidence/_raw/01-image/02-docker-load-web.txt`。

## 观察结论

前端镜像 `g12-web:v1` 已由 tar 包加载进节点 docker daemon，tag 非 `latest`；节点 containerd(CRI) 索引内的镜像存在性另见 `03-minikube-image-ls.md`。

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 兜底环境（无头/轻量桌面）本地终端模拟器无法在 SSH 会话中拉起，故未留存终端窗口过程截图；load 动作以命令实测输出 + 镜像清单文本留痕，未以文本渲染图冒充实拍。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
