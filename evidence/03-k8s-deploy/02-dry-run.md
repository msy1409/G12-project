# kubectl apply --dry-run=client 全过

| 项 | 内容 |
|---|---|
| 图片 | `02-dry-run.png` |
| 所属目录 | `evidence/03-k8s-deploy/` |
| 采集人 | C |
| 采集时间 | 【填：YYYY-MM-DD HH:MM（Asia/Shanghai）】 |
| 环境 | minikube / namespace `enterprise-g12` |
| 关联文档 | `docs/runbook.md §7` |

## 对应命令

```bash
kubectl apply --dry-run=client -f k8s/
```

## 图上应当看得出

每个文件一行 `... created (dry run)` 或 `configured (dry run)`，**不能有任何 `error:`**。
这是 A3 评分点的直接证据，截图要把 9 行左右全带上，不要只截最后一行。

## 实际输出（截取）

【贴：与图一致的文本输出；长输出只留首行 + 结论行，中间用 ... 省略】

## 观察结论

【填：一句话结论，写“什么现象 + 说明什么”，不要写“运行成功”这种空话】

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
