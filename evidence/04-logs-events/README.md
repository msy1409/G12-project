# 04-logs-events —— 日志与事件

**证明**：三个工作负载都真的在干活（不是“Pod Running 就算完”），以及探针路径、事件链正常 —— 也是 A9 复盘里“日志/Events 证据”那两段的素材来源。

| 顺序 | 文件 | 采集命令 | 看什么 |
|---|---|---|---|
| 1 | `01-backend-logs.png` | `kubectl logs -l app=g12-backend --timestamps --tail=50` | `Started ...Application`、Tomcat 8080、请求日志 |
| 2 | `02-frontend-logs.png` | `kubectl logs -l app=g12-frontend --timestamps --tail=50` | `GET /` 200、`/api/` 转发记录 |
| 3 | `03-mysql-logs.png` | `kubectl logs -l app=g12-mysql --timestamps --tail=60` | `ready for connections`、`running /docker-entrypoint-initdb.d/schema.sql` |
| 4 | `04-describe-pod.png` | `kubectl describe pod <pod> -n enterprise-g12` | 探针 URL 是 `/api/actuator/health` |
| 5 | `05-events.png` | `kubectl get events --sort-by=.lastTimestamp \| tail -25` | `Scheduled→Pulled→Created→Started` |

**`--previous` 的正确写法**（群里要求“没日志的写明原因，别截空图”）：

- 容器重启过（`RESTARTS>0`）：`kubectl logs <pod> -n enterprise-g12 --previous` 有内容，截。
- 容器**从未启动**（`ImagePullBackOff`/`ErrImageNeverPull`）：`--previous` 返回 `previous terminated container ... not found`。这时**截 `kubectl describe pod` 的 Events 段**，并在 md 里写一句：
  > 「容器未成功启动过，`--previous` 无日志属预期；崩溃原因见 Events 中 `Failed to pull image g12-api:v999`。」
- 一个 Pod 有多个容器（本项目的 Pod 都只有一个业务容器 + 无 sidecar），要指定 `-c <容器名>`。

**注意**：`--timestamps` 一定要带，否则 md 里的“采集时间”和日志对不上，会被追问。
