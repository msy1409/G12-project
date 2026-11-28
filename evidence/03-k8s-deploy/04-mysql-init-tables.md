# MySQL 初始化：6 张表 + 种子数据

| 项 | 内容 |
|---|---|
| 图片 | `04-mysql-init-tables.png` |
| 所属目录 | `evidence/03-k8s-deploy/` |
| 采集人 | B（k3s 兜底环境代采） |
| 采集时间 | 2026-11-28 17:56（Asia/Shanghai） |
| 环境 | k3s v1.28.2+k3s1 单节点 / namespace `enterprise-g12` |
| 关联文档 | `docs/runbook.md §5` |

## 对应命令

```bash
kubectl exec deploy/g12-mysql -n enterprise-g12 -- \
  sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "use ctts_g12; show tables; select count(*) as apply_cnt from visitor_apply;"'
```

## 图上应当看得出

必须 6 张表：`sys_department` `sys_user` `visitor_apply` `visitor_record` `apply_process_record` `biz_status_change_log`；
`visitor_apply` **≥10 行**。**这张图是 C 的核心证据（A6/MySQL 初始化）**，表名和行数都要看得清，密码打码。
0 张表 = initdb 没执行（datadir 不空），按 runbook §5 的“清空重来”处理，**不要**改 yaml 糊过去。

## 实际输出（截取，raw：`_raw/03-k8s-deploy/04-mysql-init-tables.txt`）

```text
Tables_in_ctts_g12
apply_process_record
biz_status_change_log
sys_department
sys_user
visitor_apply
visitor_record
apply_cnt
14
```

## 观察结论

ctts_g12 库下 6 张业务表建成，访客申请 14 条（≥10），部门/用户/记录等种子数据行数齐全，初始化 SQL 执行成功。

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
