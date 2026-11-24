# Pod 标签正常（排除应用问题）

| 项 | 内容 |
|---|---|
| 图片 | `04-pods-labels.png` |
| 所属目录 | `evidence/07-incident/` |
| 采集人 | C |
| 采集时间 | 【填：YYYY-MM-DD HH:MM（Asia/Shanghai）】 |
| 环境 | minikube / namespace `enterprise-g12` |
| 关联文档 | `docs/incident-record.md §3` |

## 对应命令

```bash
kubectl get pods -n enterprise-g12 -l app=g12-backend -o wide --show-labels
kubectl logs -l app=g12-backend -n enterprise-g12 --timestamps --tail=20
```

## 图上应当看得出

Pod Running、RESTARTS=0、标签正确、日志时间戳连续 —— 证明**后端进程一直健康**，故障纯粹是“选不中”。

## 实际输出（截取）

【贴：与图一致的文本输出；长输出只留首行 + 结论行，中间用 ... 省略】

## 观察结论

【填：一句话结论，写“什么现象 + 说明什么”，不要写“运行成功”这种空话】

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
