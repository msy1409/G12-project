# 后端启动与访问日志

| 项 | 内容 |
|---|---|
| 图片 | `01-backend-logs.png` |
| 所属目录 | `evidence/04-logs-events/` |
| 采集人 | B（k3s 兜底环境代采） |
| 采集时间 | 2026-11-28 17:25（Asia/Shanghai） |
| 环境 | k3s v1.28.2+k3s1 单节点 / namespace `enterprise-g12` |
| 关联文档 | `docs/runbook.md §7` |

## 对应命令

```bash
kubectl logs -l app=g12-backend -n enterprise-g12 --timestamps --tail=40
```

## 图上应当看得出

能看到 Spring Boot 启动完成（`Started VisitorApplication`）、Tomcat 8080（context-path `/api`）、HikariPool 连上 MySQL，以及 DispatcherServlet 初始化；时间戳连续、无异常堆栈。

## 实际输出（截取）

```text
:: Spring Boot ::               (v3.5.16)
INFO 1 --- [g12-backend] [main] com.g12.visitor.VisitorApplication : Starting VisitorApplication v1.0.0 using Java 21.0.13 with PID 1 (/app/app.jar started by root in /app)
INFO 1 --- [g12-backend] [main] o.s.b.w.embedded.tomcat.TomcatWebServer : Tomcat initialized with port 8080 (http)
INFO 1 --- [g12-backend] [main] o.a.c.c.C.[Tomcat].[localhost].[/api] : Initializing Spring embedded WebApplicationContext
INFO 1 --- [g12-backend] [main] o.s.b.a.e.web.EndpointLinksResolver : Exposing 2 endpoints beneath base path '/actuator'
INFO 1 --- [g12-backend] [main] o.s.b.w.embedded.tomcat.TomcatWebServer : Tomcat started on port 8080 (http) with context path '/api'
INFO 1 --- [g12-backend] [main] com.g12.visitor.VisitorApplication : Started VisitorApplication in 18.801 seconds
INFO 1 --- [g12-backend] [nio-8080-exec-1] o.a.c.c.C.[Tomcat].[localhost].[/api] : Initializing Spring DispatcherServlet 'dispatcherServlet'
INFO 1 --- [g12-backend] [nio-8080-exec-1] com.zaxxer.hikari.HikariDataSource : HikariPool-1 - Starting...
INFO 1 --- [g12-backend] [nio-8080-exec-1] com.zaxxer.hikari.pool.HikariPool : HikariPool-1 - Added connection com.mysql.cj.jdbc.ConnectionImpl@...
INFO 1 --- [g12-backend] [nio-8080-exec-1] com.zaxxer.hikari.HikariDataSource : HikariPool-1 - Start completed.
```

## 观察结论

两个后端 Pod 均完成 Spring Boot 启动（Java 21、v3.5.16），Tomcat 在 8080 端口、context-path `/api` 就绪，HikariPool 成功建立到 MySQL 的连接，无异常堆栈，后端服务与数据库、认证链路正常。

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图；数据库连接对象 ID 已省略。
