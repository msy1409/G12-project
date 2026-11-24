# 回滚

| 项 | 内容 |
|---|---|
| 图片 | `06-rollout-undo.png` |
| 所属目录 | `evidence/05-release-rollback/` |
| 采集人 | C |
| 采集时间 | 【填：YYYY-MM-DD HH:MM（Asia/Shanghai）】 |
| 环境 | minikube / namespace `enterprise-g12` |
| 关联文档 | `docs/operation-record.md OP-03` |

## 对应命令

```bash
kubectl rollout undo deployment/g12-backend -n enterprise-g12
kubectl rollout status deployment/g12-backend -n enterprise-g12 --timeout=180s
kubectl get deploy g12-backend -n enterprise-g12
kubectl rollout history deployment/g12-backend -n enterprise-g12
```

接口也要验一遍（沿用发 v2 时的判据）：

```bash
curl -s -X POST http://127.0.0.1:8080/api/auth/login \
  -H 'Content-Type: application/json' -d '{"username":"zhangsan","password":"123456"}' \
  | grep -o '"apiVersion":"v2"'
# 有输出 = 回到了 v2；无输出 = 退到了 v1（层级比你预期更早）
```

## 图上应当看得出

IMAGE 从 `v999` 回到 **`g12-api:v2`**（undo 默认退回上一层，不是 v1 —— 这点写错会被抓）；
AVAILABLE 回到 2；history 多一条 revision；curl 的 grep 命中 `"apiVersion":"v2"`。

## 实际输出（截取）

【贴：与图一致的文本输出；长输出只留首行 + 结论行，中间用 ... 省略】

## 观察结论

【填：一句话结论，写“什么现象 + 说明什么”，不要写“运行成功”这种空话】

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
