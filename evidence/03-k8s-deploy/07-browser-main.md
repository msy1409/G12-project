# 登录后主界面（真数据）

| 项 | 内容 |
|---|---|
| 图片 | `07-browser-main.png`（兜底环境以登录后带 token 查询业务列表的 curl 文本证据替代浏览器截图） |
| 所属目录 | `evidence/03-k8s-deploy/` |
| 采集人 | B（k3s 兜底复刻环境） |
| 采集时间 | 2026-11-28 17:07（Asia/Shanghai） |
| 环境 | k3s v1.28.2+k3s1 单节点 / namespace `enterprise-g12` |
| 关联文档 | `docs/runbook.md §8` |

## 对应命令

```bash
# 先登录拿 token，再带 Authorization 头经 Ingress 域名头查询访客申请列表（ADMIN 视角）
TOKEN=$(curl -s -H 'Host: g12.course.local' -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"123456"}' \
  http://127.0.0.1/api/auth/login | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
curl -s -H 'Host: g12.course.local' -H "Authorization: Bearer $TOKEN" \
  http://127.0.0.1/api/applies
```

## 图上应当看得出

界面/返回里有从 `seed.sql` 来的真实数据（不是 mock 假数据）：访客姓名、单位、部门、接待人、事由、预约时间、状态/验证码等。
这是“全栈真的通了”的最终证据；A 关 mock 联调的结论也对得上。

## 实际输出（截取）

```text
{"code":200,"message":"ok","data":{"list":[
 {"id":14,"applyNo":"AP20261109000014","visitorName":"郭静","visitorCompany":"明德律所",
  "deptName":"技术研发部","hostName":"刘洋","visitReason":"合同评审","visitTime":"2026-11-13 09:00:00","status":9},
 {"id":13,"applyNo":"AP20261109000013","visitorName":"胡军","visitorCompany":"远景能源",
  "deptName":"销售部","hostName":"赵磊","visitReason":"商务拜访","status":0},
 {"id":12,"visitorName":"朱婷","visitorCompany":"海通咨询","deptName":"市场部",
  "hostName":"刘洋","visitReason":"需求沟通"}
 ...（列表已截断）]}}
applies HTTP 200
```

## 观察结论

登录后带 token 经域名入口查询 `/api/applies` 返回 200，列表为 seed.sql 灌入的真实访客申请数据（姓名/部门/接待人/事由/预约时间/状态齐全），证明后端直连数据库读取真实业务数据，前后端联调链路经 Ingress 域名闭环；主界面浏览器截图列为受限项，答辩环境补拍。

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图；Authorization 头中的 JWT 不写入证据文本。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
