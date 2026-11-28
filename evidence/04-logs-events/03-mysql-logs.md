# MySQL 容器日志

| 项 | 内容 |
|---|---|
| 图片 | `03-mysql-logs.png` |
| 所属目录 | `evidence/04-logs-events/` |
| 采集人 | B（k3s 兜底环境代采） |
| 采集时间 | 2026-11-28 17:40（Asia/Shanghai） |
| 环境 | k3s v1.28.2+k3s1 单节点 / namespace `enterprise-g12` |
| 关联文档 | `docs/runbook.md §5` |

## 对应命令

```bash
kubectl logs -l app=g12-mysql -n enterprise-g12 --timestamps --tail=60
```

## 图上应当看得出

MySQL 8.0 入口脚本启动、InnoDB 初始化完成、`ready for connections` 监听 3306；无 `Permission denied`、无拒绝连接或致命堆栈。（`/docker-entrypoint-initdb.d/*.sql` 仅在首次初始化执行一次，后续重启日志不再重复出现。）

## 实际输出（截取）

```text
2026-11-28 07:10:39 [Note] [Entrypoint]: Entrypoint script for MySQL Server 8.0.46-1.el9 started.
2026-11-28 07:10:40 [Note] [Entrypoint]: Switching to dedicated user 'mysql'
2026-11-28T07:10:42 [System] [MY-013576] [InnoDB] InnoDB initialization has started.
2026-11-28T07:10:50 [System] [MY-013577] [InnoDB] InnoDB initialization has ended.
2026-11-28T07:10:51 [System] [MY-010931] [Server] /usr/sbin/mysqld: ready for connections. Version: '8.0.46'  socket: '/var/run/mysqld/mysqld.sock'  port: 3306  MySQL Community Server - GPL.
2026-11-28T07:10:51 [System] [MY-011323] [Server] X Plugin ready for connections. Bind-address: '::' port: 33060
```

## 观察结论

MySQL 8.0.46 完成 InnoDB 初始化并在 3306 端口就绪接受连接，数据库服务可用；日志无致命错误中断。

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
