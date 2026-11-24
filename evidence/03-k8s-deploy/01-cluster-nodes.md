# 集群与 addon 就绪

| 项 | 内容 |
|---|---|
| 图片 | `01-cluster-nodes.png` |
| 所属目录 | `evidence/03-k8s-deploy/` |
| 采集人 | C |
| 采集时间 | 【填：YYYY-MM-DD HH:MM（Asia/Shanghai）】 |
| 环境 | minikube / namespace `enterprise-g12` |
| 关联文档 | `docs/runbook.md §2` |

## 对应命令

```bash
minikube version
kubectl get nodes -o wide
kubectl get pods -n ingress-nginx
kubectl get pods -n kube-system | grep metrics
minikube ip
```

## 图上应当看得出

节点 STATUS=Ready、版本一致；`ingress-nginx-controller` 与 `metrics-server` 都是 `1/1 Running`；`minikube ip` 有值（记进 §交接给 A/B 的 hosts 说明）。

## 实际输出（截取）

【贴：与图一致的文本输出；长输出只留首行 + 结论行，中间用 ... 省略】

## 观察结论

【填：一句话结论，写“什么现象 + 说明什么”，不要写“运行成功”这种空话】

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
