# Ingress 域名路由访问

| 项 | 内容 |
|---|---|
| 图片 | `09-ingress-route.png` |
| 所属目录 | `evidence/03-k8s-deploy/` |
| 采集人 | B（k3s 兜底环境代采） |
| 采集时间 | 2026-11-28 16:58（Asia/Shanghai） |
| 环境 | k3s v1.28.2+k3s1 单节点 / namespace `enterprise-g12` |
| 关联文档 | `docs/runbook.md §7` |

## 对应命令

```bash
kubectl get ingress -n enterprise-g12
curl -s -m 8 -o /tmp/ing.html -w 'HTTP %{http_code}\n' \
  -H 'Host: g12.course.local' http://127.0.0.1/
head -c 300 /tmp/ing.html
```

## 图上应当看得出

`g12-ingress` CLASS=`nginx`，HOSTS=`g12.course.local`，ADDRESS 有值；带域名头请求返回 HTTP 200，正文是前端首页 HTML（标题 G12 访客预约平台）。

## 实际输出（截取）

```text
NAME          CLASS   HOSTS              ADDRESS     PORTS   AGE
g12-ingress   nginx   g12.course.local   10.0.2.15   80      3h23m
---
HTTP 200
---
<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <meta name="description" content="G12 企业访客预约与到访核验平台" />
    <title>G12 访客预约平台</title>
```

## 观察结论

nginx Ingress 控制器按 `g12.course.local` 域名把请求路由到 g12-frontend 后端，首页 HTML 正常返回 200，域名路由在集群内真实打通；Ingress ADDRESS 回填为节点内网 IP 10.0.2.15。

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 浏览器访问需把 `g12.course.local` 解析到 VM IP（10.0.2.15），或用 `Host` 头直连控制器 80 端口。
