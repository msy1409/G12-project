# 前端 nginx 日志

| 项 | 内容 |
|---|---|
| 图片 | `02-frontend-logs.png` |
| 所属目录 | `evidence/04-logs-events/` |
| 采集人 | B（k3s 兜底环境代采） |
| 采集时间 | 2026-11-28 17:40（Asia/Shanghai） |
| 环境 | k3s v1.28.2+k3s1 单节点 / namespace `enterprise-g12` |
| 关联文档 | `docs/runbook.md §7` |

## 对应命令

```bash
kubectl logs -l app=g12-frontend -n enterprise-g12 --timestamps --tail=50
```

## 图上应当看得出

`GET /` 探针持续 200；`/api/...` 转发记录（登录、健康检查、`/me`）均返回 200；无 502 / 504 转发失败。

## 实际输出（截取）

```text
2026-11-28T17:35:21.226 10.42.0.1 - - "GET / HTTP/1.1" 200 495 "-" "kube-probe/1.28" "-"
2026-11-28T17:35:26.629 10.42.0.1 - - "GET / HTTP/1.1" 200 495 "-" "kube-probe/1.28" "-"
2026-11-28T17:35:55.576 10.42.0.1 - - "POST /api/auth/login HTTP/1.1" 200 464 "-" "curl/7.81.0" "127.0.0.1"
2026-11-28T17:37:54.554 10.42.0.1 - - "GET /api/actuator/health HTTP/1.1" 200 60 "-" "curl/7.81.0" "127.0.0.1"
2026-11-28T17:37:55.823 10.42.0.1 - - "GET /api/auth/me HTTP/1.1" 200 237 "-" "curl/7.81.0" "127.0.0.1"
（其余为 kube-probe 周期性 GET / 200，已省略重复行）
```

## 观察结论

前端 nginx 持续响应存活探针（`GET /` 200），`/api/auth/login`、`/api/actuator/health`、`/api/auth/me` 经前端代理转发后端均返回 200，无转发失败，前后端链路联通。

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
