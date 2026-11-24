# 异常发布 v999（故意的）

| 项 | 内容 |
|---|---|
| 图片 | `05-bad-image-v999.png` |
| 所属目录 | `evidence/05-release-rollback/` |
| 采集人 | C |
| 采集时间 | 【填：YYYY-MM-DD HH:MM（Asia/Shanghai）】 |
| 环境 | minikube / namespace `enterprise-g12` |
| 关联文档 | `docs/operation-record.md OP-02` |

## 对应命令

```bash
kubectl set image deployment/g12-backend backend=g12-api:v999 -n enterprise-g12
kubectl get pods -n enterprise-g12 -l app=g12-backend
kubectl describe pod <Pending的pod> -n enterprise-g12 | tail -20
kubectl get events -n enterprise-g12 --sort-by=.lastTimestamp | tail -15
```

## 图上应当看得出

新 Pod `ErrImageNeverPull` / `ImagePullBackOff`；**旧的 2 个 v2 Pod 仍 Running**（服务没断）。
describe 的 Events 里必须有 `Failed to pull image "g12-api:v999"` 或 `container image not present` —— 这就是根因证据。
这张和 `docs/operation-record.md` OP-02 一一对应。

## 实际输出（截取）

【贴：与图一致的文本输出；长输出只留首行 + 结论行，中间用 ... 省略】

## 观察结论

【填：一句话结论，写“什么现象 + 说明什么”，不要写“运行成功”这种空话】

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
