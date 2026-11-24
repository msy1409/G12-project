# 滚动更新到 v2

| 项 | 内容 |
|---|---|
| 图片 | `03-rollout-v2.png` |
| 所属目录 | `evidence/05-release-rollback/` |
| 采集人 | C |
| 采集时间 | 【填：YYYY-MM-DD HH:MM（Asia/Shanghai）】 |
| 环境 | minikube / namespace `enterprise-g12` |
| 关联文档 | `docs/operation-record.md OP-01` |

## 对应命令

```bash
kubectl set image deployment/g12-backend backend=g12-api:v2 -n enterprise-g12
kubectl rollout status deployment/g12-backend -n enterprise-g12 --timeout=180s
kubectl get pods -n enterprise-g12 -l app=g12-backend -o wide
```

**再补一张“v2 真的生效”的请求对比**（B 确认 v2 的改动是 `/auth/login`、`/auth/me` 返回的用户信息里多了 `apiVersion:"v2"`）：

```bash
kubectl port-forward -n enterprise-g12 svc/g12-backend 8080:8080    # 另开终端挂着
curl -s -X POST http://127.0.0.1:8080/api/auth/login \
  -H 'Content-Type: application/json' -d '{"username":"zhangsan","password":"123456"}' \
  | grep -o '"apiVersion":"v2"'
# 有输出 = v2 生效；无输出 = 还是 v1（包错了 / Pod 没换）
```

## 图上应当看得出

`rollout status` 最后一行是 `successfully rolled out`；Pod 列表全 `Running`、RESTARTS=0；curl 那条 `grep` 有 `"apiVersion":"v2"` 命中。
能截到“新旧 Pod 并存”的中间态更好，那正是滚动更新（而非停服更新）的证据。
**只有 tag 变、没有 `apiVersion` 命中，就不算发布了 v2** —— 这点答辩一定会问。

## 实际输出（截取）

【贴：与图一致的文本输出；长输出只留首行 + 结论行，中间用 ... 省略】

## 观察结论

【填：一句话结论，写“什么现象 + 说明什么”，不要写“运行成功”这种空话】

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
