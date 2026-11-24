# 浏览器登录页（Ingress 打通）

| 项 | 内容 |
|---|---|
| 图片 | `06-browser-login.png` |
| 所属目录 | `evidence/03-k8s-deploy/` |
| 采集人 | C |
| 采集时间 | 【填：YYYY-MM-DD HH:MM（Asia/Shanghai）】 |
| 环境 | minikube / namespace `enterprise-g12` |
| 关联文档 | `docs/runbook.md §8` |

## 对应命令

```bash
# VM 上
kubectl port-forward -n ingress-nginx --address 0.0.0.0 svc/ingress-nginx-controller 8080:80
# Windows hosts 加一行：<VM IP>  g12.course.local
# 浏览器开 http://g12.course.local:8080
```

## 图上应当看得出

地址栏里能看到 `g12.course.local`，页面正常渲染登录表单 —— 证明 Ingress → 前端 → 静态资源 整条链通。
截图必须**带地址栏**，否则无法证明是走 Ingress 而不是 `npm run dev`。

## 实际输出（截取）

【贴：与图一致的文本输出；长输出只留首行 + 结论行，中间用 ... 省略】

## 观察结论

【填：一句话结论，写“什么现象 + 说明什么”，不要写“运行成功”这种空话】

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
