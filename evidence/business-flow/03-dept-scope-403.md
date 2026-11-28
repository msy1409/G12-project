# 异常链路：跨部门审核员越权访问/操作（HTTP 403 DEPT_SCOPE_DENIED）

| 项 | 内容 |
|---|---|
| 图片 | `03-dept-scope-403.png`（待浏览器截图补；当前为 API 文本证据，raw：`_raw/business-flow/03-dept-scope-403.txt`） |
| 所属目录 | `evidence/business-flow/` |
| 采集人 | B（k3s 兜底环境代采） |
| 采集时间 | 2026-11-28 18:09（Asia/Shanghai） |
| 环境 | k3s v1.28.2+k3s1 单节点 / namespace `enterprise-g12` |
| 关联文档 | `docs/business-flow-record.md §7`；`docs/api.md §5.3 / §6.2` |

## 对应命令

```bash
# auditor02（市场部 dept5）尝试访问销售部(dept6) 单13 详情
curl -s -o /tmp/resp.txt -w '%{http_code}' -X GET \
  http://127.0.0.1/api/applies/13 \
  -H 'Host: g12.course.local' -H 'Authorization: Bearer <token>'

# auditor02 尝试跨部门 approve 单13
curl -s -o /tmp/resp.txt -w '%{http_code}' -X PUT \
  http://127.0.0.1/api/applies/13/approve \
  -H 'Host: g12.course.local' -H 'Authorization: Bearer <token>' \
  -H 'Content-Type: application/json' -d @/tmp/b.json
```

## 图上应当看得出

跨部门审核员既看不到别部门预约详情，也不能执行审核动作；请求在部门数据范围校验处被拦截，返回 403，且不产生任何状态流转或审计记录。

## 实际输出（截取）

```text
GET  /api/applies/13          -> HTTP 403  {"code":403,"message":"DEPT_SCOPE_DENIED","data":null}
PUT  /api/applies/13/approve  -> HTTP 403  {"code":403,"message":"DEPT_SCOPE_DENIED","data":null}
```

## 观察结论

部门级数据隔离生效：AUDITOR 的可见与可操作范围被限定在本部门，跨部门读、写均返回 `403 DEPT_SCOPE_DENIED`；越权动作没有落到单据状态上。
