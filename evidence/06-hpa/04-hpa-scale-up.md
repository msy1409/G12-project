# 压测：2 → 3 → 4

| 项 | 内容 |
|---|---|
| 图片 | `04-hpa-scale-up.png` |
| 所属目录 | `evidence/06-hpa/` |
| 采集人 | C |
| 采集时间 | 【填：YYYY-MM-DD HH:MM（Asia/Shanghai）】 |
| 环境 | minikube / namespace `enterprise-g12` |
| 关联文档 | `docs/runbook.md §7` |

## 对应命令

```bash
# 终端 1：看 HPA
kubectl get hpa -n enterprise-g12 -w
# 终端 2：压测
kubectl run -it load-test --image=busybox:1.36 --rm -n enterprise-g12 -- \
  /bin/sh -c "while true; do wget -q -O- http://g12-backend:8080/api/actuator/health > /dev/null; done"
```

## 图上应当看得出

终端 1 里 REPLICAS 从 2 涨到 3、再到 4（上限），能截到 `3` 那一行最好。
URL 用 `http://g12-backend:8080/...`（Service 端口 8080 → targetPort 8080，B 已确认）。
**这是 A8 评分点的直接证据**，最好录屏，截图要含“TARGETS 超过 60%”那一刻。

## 实际输出（截取）

【贴：与图一致的文本输出；长输出只留首行 + 结论行，中间用 ... 省略】

## 观察结论

【填：一句话结论，写“什么现象 + 说明什么”，不要写“运行成功”这种空话】

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
