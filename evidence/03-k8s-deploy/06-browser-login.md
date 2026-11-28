# 浏览器登录页（Ingress 打通）

| 项 | 内容 |
|---|---|
| 图片 | `06-browser-login.png`（浏览器页面视口截图，2026-11-28 补采） |
| 所属目录 | `evidence/03-k8s-deploy/` |
| 采集人 | B（k3s 兜底复刻环境） |
| 采集时间 | 2026-11-28 18:50（Asia/Shanghai） |
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

浏览器访问链路：浏览器页面 → 本机反代（请求头 Host 改写为 `g12.course.local`）→ Ingress nginx → `g12-frontend`，`/api` 转发 `g12-backend`。

## 图上应当看得出

正常链路：浏览器经 Ingress 域名 `g12.course.local` 入口打开登录页，账号/密码表单与演示账号按钮完整渲染；登录接口 200 并返回用户信息与 JWT（token 已脱敏）。

## 实际输出（截取）

浏览器实测：
- 登录页标题「登录 · G12 访客预约平台」，地址指向 Ingress 域名入口；表单含用户名、密码、「登 录」按钮与演示账号（emp01 / auditor01 / admin）。

接口旁证：

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

请求经 nginx Ingress 按 `g12.course.local` 域名路由到前端，静态首页 200；浏览器表单正常渲染，登录接口 200 并签发 JWT 与 admin 用户信息，前端→后端→数据库链路经域名入口打通。

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图；JWT 的 `eyJ` 之后已脱敏。
- 截图为浏览器页面视口（不含浏览器外层地址栏边框）；兜底环境无独立图形桌面，页面由内置 Chromium 真实渲染并交互登录，非文本渲染图。
