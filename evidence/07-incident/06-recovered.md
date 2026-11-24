# 恢复验证（浏览器 + 三层直连）

| 项 | 内容 |
|---|---|
| 图片 | `06-recovered.png` |
| 所属目录 | `evidence/07-incident/` |
| 采集人 | C |
| 采集时间 | 【填：YYYY-MM-DD HH:MM（Asia/Shanghai）】 |
| 环境 | minikube / namespace `enterprise-g12` |
| 关联文档 | `docs/incident-record.md §6` |

## 对应命令

```bash
# 后端 Service 端口是 8080，前端 Service 端口是 80（别弄混）
kubectl run probe-check --rm -it --image=busybox:1.36 -n enterprise-g12 -- \
  wget -qO- http://g12-backend:8080/api/actuator/health
kubectl run probe-check --rm -it --image=busybox:1.36 -n enterprise-g12 -- \
  wget -qO- http://g12-frontend/api/actuator/health
```

## 图上应当看得出

两个都返回 `{"status":"UP"}`；浏览器刷新后能登录、列表能出数据。
图片里**不要出现密码或 token**；若必须出现登录框，密码栏遮掉。

## 实际输出（截取）

【贴：与图一致的文本输出；长输出只留首行 + 结论行，中间用 ... 省略】

## 观察结论

【填：一句话结论，写“什么现象 + 说明什么”，不要写“运行成功”这种空话】

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
