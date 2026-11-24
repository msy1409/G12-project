# MySQL 容器日志

| 项 | 内容 |
|---|---|
| 图片 | `03-mysql-logs.png` |
| 所属目录 | `evidence/04-logs-events/` |
| 采集人 | C |
| 采集时间 | 【填：YYYY-MM-DD HH:MM（Asia/Shanghai）】 |
| 环境 | minikube / namespace `enterprise-g12` |
| 关联文档 | `docs/runbook.md §5` |

## 对应命令

```bash
kubectl logs -l app=g12-mysql -n enterprise-g12 --timestamps --tail=60
```

## 图上应当看得出

关键三段：`ready for connections`；`running /docker-entrypoint-initdb.d/schema.sql`（证明初始化执行了）；没有 `Permission denied`，也没有“拒绝连接”。
**initdb 那行是 04 目录里最有价值的一行**，它直接支撑“6 张表是自动灌进去的”。

## 实际输出（截取）

【贴：与图一致的文本输出；长输出只留首行 + 结论行，中间用 ... 省略】

## 观察结论

【填：一句话结论，写“什么现象 + 说明什么”，不要写“运行成功”这种空话】

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
