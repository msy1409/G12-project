# 故障现象：所有 /api 502

| 项 | 内容 |
|---|---|
| 图片 | `01-symptom-502.png` |
| 所属目录 | `evidence/07-incident/` |
| 采集人 | B（k3s 兜底环境代采） |
| 采集时间 | 2026-11-28 17:45（Asia/Shanghai） |
| 环境 | k3s v1.28.2+k3s1 单节点 / namespace `enterprise-g12` |
| 关联文档 | `docs/incident-record.md §1` |

## 对应命令

```bash
# 经 Ingress 打后端接口，观测状态码
curl -o /dev/null -w 'login HTTP %{http_code}\n' -X POST http://127.0.0.1/api/auth/login \
  -H 'Host: g12.course.local' -H 'Content-Type: application/json' -d @/tmp/b.json
curl -o /dev/null -w 'health HTTP %{http_code}\n' -H 'Host: g12.course.local' \
  http://127.0.0.1/api/actuator/health
# 前端 nginx 访问/错误日志
kubectl logs -l app=g12-frontend -n enterprise-g12 --timestamps --tail=40
```

## 图上应当看得出

页面静态资源仍能打开，但登录、健康检查等 `/api/...` 请求全部返回 **502 Bad Gateway**；前端 nginx 日志出现 `connect() failed (111: Connection refused) while connecting to upstream`，上游指向 `http://10.43.180.101:8080`（backend Service ClusterIP）。

关键判别：是 **502**（网关收到请求但连不上上游），不是 404（路径错）也不是 504（上游超时）。见 `docs/incident-record.md` §1。

## 实际输出（截取）

### 接口状态码（故障态）

```text
login  HTTP 502
health HTTP 502
```

### 前端 nginx 日志（故障态关键行，raw：`_raw/07-incident/01-symptom-frontend-logs.txt`）

```text
2026-11-28T17:45:22.602620487+08:00 10.42.0.1 - - [28/Nov/2026:09:45:22 +0000] "POST /api/auth/login HTTP/1.1" 502 157 "-" "curl/7.81.0" "127.0.0.1"
2026-11-28T17:45:22.603971645+08:00 2026/11/28 09:45:22 [error] 37#37: *16066 connect() failed (111: Connection refused) while connecting to upstream, client: 10.42.0.1, server: _, request: "POST /api/auth/login HTTP/1.1", upstream: "http://10.43.180.101:8080/api/auth/login", host: "g12.course.local"
2026-11-28T17:45:22.663770730+08:00 2026/11/28 09:45:22 [error] 37#37: *16068 connect() failed (111: Connection refused) while connecting to upstream, client: 10.42.0.1, server: _, request: "GET /api/actuator/health HTTP/1.1", upstream: "http://10.43.180.101:8080/api/actuator/health", host: "g12.course.local"
2026-11-28T17:45:22.668849170+08:00 10.42.0.1 - - [28/Nov/2026:09:45:22 +0000] "GET /api/actuator/health HTTP/1.1" 502 157 "-" "curl/7.81.0" "127.0.0.1"
```

## 观察结论

502 由前端 nginx 对 backend Service 上游连接被拒绝造成：nginx 配置的上游 `g12-backend:8080`（ClusterIP 10.43.180.101）此刻没有任何可转发的后端，请求被 RST。故障入口在 Service→Pod 选中关系这一段，而非 nginx 模板或后端代码。

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图；接口响应里的 JWT 已脱敏。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
