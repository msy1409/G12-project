# 浏览器登录页（Ingress 打通）

| 项 | 内容 |
|---|---|
| 图片 | `06-browser-login.png`（兜底环境以经 Ingress 域名头的 curl 文本证据替代浏览器截图） |
| 所属目录 | `evidence/03-k8s-deploy/` |
| 采集人 | B（k3s 兜底复刻环境） |
| 采集时间 | 2026-11-28 17:06（Asia/Shanghai） |
| 环境 | k3s v1.28.2+k3s1 单节点 / namespace `enterprise-g12` |
| 关联文档 | `docs/runbook.md §8` |

## 对应命令

```bash
# 经 Ingress 域名头访问 g12.course.local（控制器监听 80）
curl -s -o /dev/null -w '首页 HTTP %{http_code}\n' -H 'Host: g12.course.local' http://127.0.0.1/
curl -s -H 'Host: g12.course.local' -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"123456"}' \
  http://127.0.0.1/api/auth/login
```

## 图上应当看得出

正常链路：浏览器经 Ingress 域名 `g12.course.local` 访问，登录表单渲染、登录后返回 token。
兜底环境以 curl 带域名头作为旁证：首页静态资源 200，登录接口 200 并返回用户信息与 JWT（token 已脱敏）。

## 实际输出（截取）

```text
== 首页(经 Ingress 域名头) ==
首页 HTTP 200
<title>G12 访客预约平台</title>
== 登录接口(经 Ingress 域名头) ==
{"code":200,"message":"ok","data":{"token":"eyJ……（JWT 已脱敏）",
  "user":{"id":1,"username":"admin","realName":"系统管理员","deptName":"行政部","role":"ADMIN","status":1}}}
登录 HTTP 200
```

## 观察结论

请求经 nginx Ingress 按 `g12.course.local` 域名路由到前端，静态首页 200；登录接口 200 并签发 JWT 与 admin 用户信息，前端→后端→数据库链路经域名入口打通。

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图；JWT 的 `eyJ` 之后已脱敏。
- 带浏览器地址栏的登录页/主界面截图需在有图形桌面的答辩环境补采；当前为命令行旁证。
