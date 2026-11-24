# Service 详情

| 项 | 内容 |
|---|---|
| 图片 | `03-describe-svc.png` |
| 所属目录 | `evidence/07-incident/` |
| 采集人 | C |
| 采集时间 | 【填：YYYY-MM-DD HH:MM（Asia/Shanghai）】 |
| 环境 | minikube / namespace `enterprise-g12` |
| 关联文档 | `docs/incident-record.md §3` |

## 对应命令

```bash
kubectl describe svc g12-backend -n enterprise-g12
kubectl describe endpoints g12-backend -n enterprise-g12
```

## 图上应当看得出

`Selector: app=wrong-label`、`Endpoints: <none>`、`Subsets: <none>` 要能看清。
对应 §3 的排除法。

## 实际输出（截取）

【贴：与图一致的文本输出；长输出只留首行 + 结论行，中间用 ... 省略】

## 观察结论

【填：一句话结论，写“什么现象 + 说明什么”，不要写“运行成功”这种空话】

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
