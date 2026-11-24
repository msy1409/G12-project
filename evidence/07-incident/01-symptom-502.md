# 故障现象：所有 /api 502

| 项 | 内容 |
|---|---|
| 图片 | `01-symptom-502.png` |
| 所属目录 | `evidence/07-incident/` |
| 采集人 | C |
| 采集时间 | 【填：YYYY-MM-DD HH:MM（Asia/Shanghai）】 |
| 环境 | minikube / namespace `enterprise-g12` |
| 关联文档 | `docs/incident-record.md §1` |

## 对应命令

```bash
# 浏览器打开页面，Network 面板看 /api 请求状态码
kubectl logs -l app=g12-frontend -n enterprise-g12 --timestamps --tail=30
```

## 图上应当看得出

Network 里 `/api/...` 全是 **502**（页面本身能打开）；nginx 日志里有 `connect() failed (111: Connection refused) while connecting to upstream`。
关键判别：502 而不是 404/504 —— 网关能收到请求但连不上上游。见 `docs/incident-record.md` §1。

## 实际输出（截取）

【贴：与图一致的文本输出；长输出只留首行 + 结论行，中间用 ... 省略】

## 观察结论

【填：一句话结论，写“什么现象 + 说明什么”，不要写“运行成功”这种空话】

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
