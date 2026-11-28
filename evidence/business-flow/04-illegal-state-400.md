# 异常链路：非法状态流转（HTTP 400 STATE_NOT_ALLOWED）

| 项 | 内容 |
|---|---|
| 图片 | `04-illegal-state-400.png`（待浏览器截图补；当前为 API 文本证据，raw：`_raw/business-flow/04-illegal-state-400.txt`） |
| 所属目录 | `evidence/business-flow/` |
| 采集人 | B（k3s 兜底环境代采） |
| 采集时间 | 2026-11-28 18:09（Asia/Shanghai） |
| 环境 | k3s v1.28.2+k3s1 单节点 / namespace `enterprise-g12` |
| 关联文档 | `docs/business-flow-record.md §7`；`docs/api.md §5.4 / §7.2` |

## 对应命令

```bash
# 取消已离场单1（status=4）——非法
curl -s -o /tmp/resp.txt -w '%{http_code}' -X PUT \
  http://127.0.0.1/api/applies/1/cancel \
  -H 'Host: g12.course.local' -H 'Authorization: Bearer <token>' \
  -H 'Content-Type: application/json' -d @/tmp/b.json

# 取消在场单5（status=3）——非法
curl -s -o /tmp/resp.txt -w '%{http_code}' -X PUT \
  http://127.0.0.1/api/applies/5/cancel \
  -H 'Host: g12.course.local' -H 'Authorization: Bearer <token>' \
  -H 'Content-Type: application/json' -d @/tmp/b.json

# 已离场单1 重复离场 checkout——非法
curl -s -o /tmp/resp.txt -w '%{http_code}' -X POST \
  http://127.0.0.1/api/records/checkout \
  -H 'Host: g12.course.local' -H 'Authorization: Bearer <token>' \
  -H 'Content-Type: application/json' -d @/tmp/b.json
```

## 图上应当看得出

状态机只允许合法前驱状态发起对应流转；对已离场（4）、在场（3）单据取消，或对已离场单据重复登记离场，均被状态机拦截，返回 400，单据状态保持不变。

## 实际输出（截取）

```text
PUT  /api/applies/1/cancel      （status=4 取消）-> HTTP 400  {"code":400,"message":"STATE_NOT_ALLOWED","data":null}
PUT  /api/applies/5/cancel      （status=3 取消）-> HTTP 400  {"code":400,"message":"STATE_NOT_ALLOWED","data":null}
POST /api/records/checkout  （单1 重复离场）    -> HTTP 400  {"code":400,"message":"STATE_NOT_ALLOWED","data":null}
```

## 观察结论

状态机防误操作生效：非法前驱状态下的取消、重复离场均返回 `400 STATE_NOT_ALLOWED`；复核 DB，单1（status=4）、单5（status=3）状态未被改动。
