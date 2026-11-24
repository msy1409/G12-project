# 命名空间事件流

| 项 | 内容 |
|---|---|
| 图片 | `05-events.png` |
| 所属目录 | `evidence/04-logs-events/` |
| 采集人 | C |
| 采集时间 | 【填：YYYY-MM-DD HH:MM（Asia/Shanghai）】 |
| 环境 | minikube / namespace `enterprise-g12` |
| 关联文档 | `docs/runbook.md §12` |

## 对应命令

```bash
kubectl get events -n enterprise-g12 --sort-by=.lastTimestamp
```

## 图上应当看得出

按时间排，能看到 `Scheduled → Pulled → Created → Started` 的完整链路，以及 `SuccessfulCreate`（RS 建 Pod）。
出现 `FailedScheduling`（资源不够）、`FailedMount`（ConfigMap/PVC 问题）、`Unhealthy`（探针失败）要能解释。

## 实际输出（截取）

【贴：与图一致的文本输出；长输出只留首行 + 结论行，中间用 ... 省略】

## 观察结论

【填：一句话结论，写“什么现象 + 说明什么”，不要写“运行成功”这种空话】

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
