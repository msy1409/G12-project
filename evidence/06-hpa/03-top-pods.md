# metrics 有数据

| 项 | 内容 |
|---|---|
| 图片 | `03-top-pods.png` |
| 所属目录 | `evidence/06-hpa/` |
| 采集人 | C |
| 采集时间 | 【填：YYYY-MM-DD HH:MM（Asia/Shanghai）】 |
| 环境 | minikube / namespace `enterprise-g12` |
| 关联文档 | `docs/runbook.md §2` |

## 对应命令

```bash
kubectl top pods -n enterprise-g12
kubectl top nodes
```

## 图上应当看得出

能打出 CPU/memory 数值（不是 `Metrics API not available`）。
metrics-server 刚启用时要等 1–2 分钟；还是没数就 `kubectl logs -n kube-system deploy/metrics-server` 看 TLS 报错。

## 实际输出（截取）

【贴：与图一致的文本输出；长输出只留首行 + 结论行，中间用 ... 省略】

## 观察结论

【填：一句话结论，写“什么现象 + 说明什么”，不要写“运行成功”这种空话】

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
