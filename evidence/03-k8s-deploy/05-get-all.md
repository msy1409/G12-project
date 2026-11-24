# 全栈对象一览（一把抓）

| 项 | 内容 |
|---|---|
| 图片 | `05-get-all.png` |
| 所属目录 | `evidence/03-k8s-deploy/` |
| 采集人 | C |
| 采集时间 | 【填：YYYY-MM-DD HH:MM（Asia/Shanghai）】 |
| 环境 | minikube / namespace `enterprise-g12` |
| 关联文档 | `docs/runbook.md §7` |

## 对应命令

```bash
kubectl get pods,svc,endpoints,pvc,ingress,hpa -n enterprise-g12 -o wide
```

## 图上应当看得出

一张图要同时满足：Pod 全 `Running` 且 READY 满格；`g12-backend`/`g12-frontend`/`g12-mysql` 三个 **ENDPOINTS 都非空**；PVC `Bound`；Ingress 有 ADDRESS；HPA 有 TARGETS。
这张是 A4 评分点的证据，**必须完整不裁切**。

## 实际输出（截取）

【贴：与图一致的文本输出；长输出只留首行 + 结论行，中间用 ... 省略】

## 观察结论

【填：一句话结论，写“什么现象 + 说明什么”，不要写“运行成功”这种空话】

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
