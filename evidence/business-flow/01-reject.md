# 异常链路：审核员驳回预约（0 → 9，REJECT）

| 项 | 内容 |
|---|---|
| 图片 | `01-reject.png`（待浏览器截图补；当前为 API 文本证据，raw：`_raw/business-flow/01-reject.txt`） |
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
# auditor01（技术研发部审核员）驳回，reason 必填
curl -s -X PUT http://127.0.0.1/api/applies/15/reject \
  -H 'Host: g12.course.local' -H 'Authorization: Bearer <token>' \
  -H 'Content-Type: application/json' -d @/tmp/b.json
# 只读复核：DB 终态
kubectl exec <mysql-pod> -n enterprise-g12 -- sh -c \
  'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" ctts_g12 -t < /tmp/q.sql'
```

## 图上应当看得出

待审核（status=0）的预约经本部门审核员驳回后，状态流转到 9（已驳回，REJECT）；后端写入一条 REJECT 处理记录与一条 `0→9` 状态审计。

## 实际输出（截取）

```text
建单 -> {"code":200,"data":{"id":15,"applyNo":"AP20261128000015","status":0}}
驳回 -> HTTP 200  {"code":200,"message":"ok","data":null}

DB 终态：
+----+------------------+---------+--------+-----------+
| id | apply_no         | dept_id | status | create_by |
+----+------------------+---------+--------+-----------+
| 15 | AP20261128000015 |       4 |      9 |         5 |
+----+------------------+---------+--------+-----------+
```

## 观察结论

驳回分支闭环：`status 0 → 9`，`action=REJECT` 落库；同部门审核员权限内可操作，驳回后单据不再进入核验/入场流程。
