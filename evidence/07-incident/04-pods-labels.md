# Pod 标签正常、后端进程未崩（排除应用问题）

| 项 | 内容 |
|---|---|
| 图片 | `04-pods-labels.png` |
| 所属目录 | `evidence/07-incident/` |
| 采集人 | B（k3s 兜底环境代采） |
| 采集时间 | 2026-11-28 17:45（Asia/Shanghai） |
| 环境 | k3s v1.28.2+k3s1 单节点 / namespace `enterprise-g12` |
| 关联文档 | `docs/incident-record.md §3` |

## 对应命令

```bash
kubectl get pods -n enterprise-g12 -l app=g12-backend -o wide --show-labels
kubectl logs -l app=g12-backend -n enterprise-g12 --timestamps --tail=20
```

## 图上应当看得出

后端 Pod `1/1 Running`、`RESTARTS=0`、标签 `app=g12-backend` 正确；后端日志时间戳连续、Spring Boot 正常 Started、HikariPool 已连上 MySQL —— 证明**后端进程一直健康**，故障纯粹是“Service 选不中这些 Pod”。

## 实际输出（截取）

### Pod 状态与标签（故障态）

```text
NAME                            READY   STATUS    RESTARTS   AGE     IP            LABELS
g12-backend-7585dd6b86-pj429    1/1     Running   0          15m     10.42.0.173  app=g12-backend,pod-template-hash=7585dd6b86,tier=backend
g12-backend-7585dd6b86-9kzqb    1/1     Running   0          14m     10.42.0.174  app=g12-backend,pod-template-hash=7585dd6b86,tier=backend
```

### 后端日志尾部（raw：`_raw/07-incident/04-backend-logs.txt`）

```text
Tomcat started on port 8080 (http) with context path '/api'
Started VisitorApplication in 33.744 seconds (process running for 38.583)
HikariPool-1 - Added connection com.mysql.cj.jdbc.ConnectionImpl@58705d4f
HikariPool-1 - Start completed.
```

## 观察结论

两个后端 Pod 均 Ready、未重启，应用已正常启动并成功连接数据库；日志里没有崩溃堆栈或反复重启记录。据此排除“后端 Pod 挂了 / 探针失败不 Ready / 后端端口错 / 前端模板错”等假设，唯一成立的是 Service selector 与 Pod 标签不匹配（见 02、03）。

## 备注

- 本次故障容器没有重启（RESTARTS=0），`--previous` 日志只会返回 `previous terminated container ... not found`，这本身就是“没重启”的证据。
- 密码、token、kubeconfig 内容一律打码后再截图。
