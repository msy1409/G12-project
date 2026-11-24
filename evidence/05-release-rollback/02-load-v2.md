# v2 镜像就位

| 项 | 内容 |
|---|---|
| 图片 | `02-load-v2.png` |
| 所属目录 | `evidence/05-release-rollback/` |
| 采集人 | C |
| 采集时间 | 【填：YYYY-MM-DD HH:MM（Asia/Shanghai）】 |
| 环境 | minikube / namespace `enterprise-g12` |
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

【贴：与图一致的文本输出；长输出只留首行 + 结论行，中间用 ... 省略】

## 观察结论

【填：一句话结论，写“什么现象 + 说明什么”，不要写“运行成功”这种空话】

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
