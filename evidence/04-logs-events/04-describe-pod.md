# Pod 详情（探针与事件）

| 项 | 内容 |
|---|---|
| 图片 | `04-describe-pod.png` |
| 所属目录 | `evidence/04-logs-events/` |
| 采集人 | C |
| 采集时间 | 【填：YYYY-MM-DD HH:MM（Asia/Shanghai）】 |
| 环境 | minikube / namespace `enterprise-g12` |
| 关联文档 | `docs/runbook.md §7` |

## 对应命令

```bash
kubectl describe pod <pod名> -n enterprise-g12
```

## 图上应当看得出

要看三块：`Containers` 里的 `Image:` 和 `Port:`；`Readiness probe`/`Liveness probe` 的 URL 是 **`/api/actuator/health`**；`Events` 段最后是 `Started` 而不是反复 `Killing`。
探针写成 `/actuator/health`（少了 `/api`）是典型错法，Pod 会永远不 Ready。

## 实际输出（截取）

【贴：与图一致的文本输出；长输出只留首行 + 结论行，中间用 ... 省略】

## 观察结论

【填：一句话结论，写“什么现象 + 说明什么”，不要写“运行成功”这种空话】

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
