# PVC 绑定成功

| 项 | 内容 |
|---|---|
| 图片 | `03-pvc-bound.png` |
| 所属目录 | `evidence/03-k8s-deploy/` |
| 采集人 | C |
| 采集时间 | 【填：YYYY-MM-DD HH:MM（Asia/Shanghai）】 |
| 环境 | minikube / namespace `enterprise-g12` |
| 关联文档 | `docs/runbook.md §5` |

## 对应命令

```bash
kubectl get pvc -n enterprise-g12
kubectl get pv
kubectl describe pvc g12-mysql-pvc -n enterprise-g12 | tail -15
```

## 图上应当看得出

`g12-mysql-pvc` 的 STATUS 必须是 **Bound**（不能是 Pending），CAPACITY 5Gi，VOLUME 是 `g12-mysql-pv`。
Pending 时看 `describe` 的 Events：多半是 `storageClassName` 不一致，或集群里没有 `g12-local` 这个类（见 k8s/04 的注释）。

## 实际输出（截取）

【贴：与图一致的文本输出；长输出只留首行 + 结论行，中间用 ... 省略】

## 观察结论

【填：一句话结论，写“什么现象 + 说明什么”，不要写“运行成功”这种空话】

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
