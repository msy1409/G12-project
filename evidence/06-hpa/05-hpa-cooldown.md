# 停压测后回落

| 项 | 内容 |
|---|---|
| 图片 | `05-hpa-cooldown.png` |
| 所属目录 | `evidence/06-hpa/` |
| 采集人 | C |
| 采集时间 | 【填：YYYY-MM-DD HH:MM（Asia/Shanghai）】 |
| 环境 | minikube / namespace `enterprise-g12` |
| 关联文档 | `docs/runbook.md §7` |

## 对应命令

```bash
# Ctrl+C 结束压测，继续看
kubectl get hpa -n enterprise-g12 -w
kubectl get pods -n enterprise-g12 -l app=g12-backend
```

## 图上应当看得出

3–5 分钟后 REPLICAS 回落到 2（默认缩容冷却 5 分钟，要有耐心，别以为坏了）。
这张和 04 配对，证明“能上也能下”。

## 实际输出（截取）

【贴：与图一致的文本输出；长输出只留首行 + 结论行，中间用 ... 省略】

## 观察结论

【填：一句话结论，写“什么现象 + 说明什么”，不要写“运行成功”这种空话】

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
