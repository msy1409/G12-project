# 加载后端镜像 g12-api:v1

| 项 | 内容 |
|---|---|
| 图片 | `01-docker-load-api.png` |
| 所属目录 | `evidence/01-image/` |
| 采集人 | C |
| 采集时间 | 【填：YYYY-MM-DD HH:MM（Asia/Shanghai）】 |
| 环境 | minikube / namespace `enterprise-g12` |
| 关联文档 | `docs/runbook.md §6` |

## 对应命令

```bash
docker load -i g12-api-v1.tar
docker images | grep g12
```

## 图上应当看得出

最后一行必须是 `Loaded image: g12-api:v1`；`docker images` 里能看到 `g12-api  v1`。若显示的是 `g12-api:v2` 或 tag 为 `latest`，说明拿错包，停下找 B 重发。

## 实际输出（截取）

【贴：与图一致的文本输出；长输出只留首行 + 结论行，中间用 ... 省略】

## 观察结论

【填：一句话结论，写“什么现象 + 说明什么”，不要写“运行成功”这种空话】

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
