# 异常链路：员工取消本人预约（0/1/2 → 9，CANCEL）

| 项 | 内容 |
|---|---|
| 图片 | `02-cancel.png`（待浏览器截图补；当前为 API 文本证据，raw：`_raw/business-flow/02-cancel.txt`） |
| 所属目录 | `evidence/business-flow/` |
| 采集人 | B（k3s 兜底环境代采） |
| 采集时间 | 2026-11-28 18:09（Asia/Shanghai） |
| 环境 | k3s v1.28.2+k3s1 单节点 / namespace `enterprise-g12` |
| 关联文档 | `docs/business-flow-record.md §7` |

## 对应命令

```bash
# emp01（技术研发部 dept4）提交预约，拿新建单 id
curl -s -X POST http://127.0.0.1/api/applies \
  -H 'Host: g12.course.local' -H 'Authorization: Bearer <token>' \
  -H 'Content-Type: application/json' -d @/tmp/b.json
# emp01 本人取消自己未开始的预约
curl -s -X PUT http://127.0.0.1/api/applies/16/cancel \
  -H 'Host: g12.course.local' -H 'Authorization: Bearer <token>' \
  -H 'Content-Type: application/json' -d @/tmp/b.json
# 只读复核：DB 终态
kubectl exec <mysql-pod> -n enterprise-g12 -- sh -c \
  'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" ctts_g12 -t < /tmp/q.sql'
```

## 图上应当看得出

员工对自己创建、尚未到场（状态 0/1/2）的预约发起取消，状态流转到 9（已取消，CANCEL）；后端写入 CANCEL 处理记录与对应状态审计。

## 实际输出（截取）

```text
建单 -> {"code":200,"data":{"id":16,"applyNo":"AP20261128000016","status":0}}
取消 -> HTTP 200  {"code":200,"message":"ok","data":null}

DB 终态：
+----+------------------+---------+--------+-----------+
| id | apply_no         | dept_id | status | create_by |
+----+------------------+---------+--------+-----------+
| 16 | AP20261128000016 |       4 |      9 |         5 |
+----+------------------+---------+--------+-----------+
```

## 观察结论

取消分支闭环：`status 0 → 9`，`action=CANCEL` 落库；仅本人（或 ADMIN）可取消，取消后单据终态，不参与到访核验。
