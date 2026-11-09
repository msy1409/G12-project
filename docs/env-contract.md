# 环境变量与配置契约（env-contract.md）

> 负责人：B　使用人：B（application.yml）、C（ConfigMap / Secret / MySQL）、A（nginx 模板）
> **这份文件的 key 名不许改。** 改一个字母，三边一起挂。
> 最后更新：2026-11-09（v1.0 冻结）

---

## 1. 后端读的（Spring Boot `application.yml`）

```yaml
server:
  port: ${SERVER_PORT:8080}
  servlet:
    context-path: /api

spring:
  datasource:
    url: jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
    username: ${DB_USER}
    password: ${DB_PASSWORD}

g12:
  jwt:
    secret: ${JWT_SECRET}
    expire-seconds: ${JWT_EXPIRE_SECONDS:7200}
```

| 变量 | 必填 | 示例 | 谁提供 |
|---|---|---|---|
| `DB_HOST` | 是 | `g12-mysql`（K8s）/ `mysql`（Compose）/ `127.0.0.1`（本机） | ConfigMap |
| `DB_PORT` | 是 | `3306` | ConfigMap |
| `DB_NAME` | 是 | `ctts_g12` | ConfigMap |
| `DB_USER` | 是 | `root` | ConfigMap |
| `DB_PASSWORD` | 是 | 见 Secret | Secret |
| `JWT_SECRET` | 是 | 32 位以上随机串 | Secret |
| `JWT_EXPIRE_SECONDS` | 否 | `7200`（默认 2 小时） | ConfigMap |
| `SERVER_PORT` | 否 | `8080` | 默认值 |

**注意 `context-path: /api`**：所有业务路径都带 `/api` 前缀，健康检查是 `/api/actuator/health`。C 写探针、A 写 baseURL 都用这个完整路径。

---

## 2. MySQL 容器读的

| 变量 | 值 | 说明 |
|---|---|---|
| `MYSQL_ROOT_PASSWORD` | 见 Secret | 必填，否则容器起不来 |
| `MYSQL_DATABASE` | `ctts_g12` | 容器自动建库；`schema.sql` 里仍要写 `CREATE DATABASE IF NOT EXISTS ctts_g12`，保证手工执行也成立 |

字符集：`--character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci`（Compose 的 command 和 K8s 的 args 都要带）。

---

## 3. 前端 nginx 读的

`frontend/nginx.conf.template`（构建时用 `envsubst` 渲染）：

```nginx
location /api/ {
    proxy_pass http://${BACKEND_HOST}:${BACKEND_PORT};
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
}

location / {
    root /usr/share/nginx/html;
    try_files $uri $uri/ /index.html;
}
```

| 变量 | Compose 值 | K8s 值 |
|---|---|---|
| `BACKEND_HOST` | `backend`（服务名） | `g12-backend`（Service 名） |
| `BACKEND_PORT` | `8080` | `8080` |

> 用同一套镜像、两个环境只换这两个值，是 A7 发布回滚和 A4 网络评分的加分点。

---

## 4. Secret 里到底放什么

只放三个：

| key | 用途 |
|---|---|
| `MYSQL_ROOT_PASSWORD` | MySQL 容器初始化 |
| `DB_PASSWORD` | 后端连库（值同上） |
| `JWT_SECRET` | 签发 token |

`secret.example.yaml` 里这三个值全部写 `CHANGE_ME`，进仓库；真实值只在本机 `kubectl create secret` 时敲，不进仓库、不进文档、不进截图。

---

## 5. 三边对齐检查（C 部署前逐条核）

- [ ] `g12-config` 的 key 和上表第 1 节一字不差
- [ ] `g12-secret` 的 key 和上表第 4 节一字不差
- [ ] 后端 ConfigMap 挂进去后，容器里 `env | grep DB_` 能看到四个 DB 变量
- [ ] 探针路径是 `/api/actuator/health`，不是 `/actuator/health`
- [ ] 前端 `BACKEND_HOST=g12-backend`，不是 `backend`
- [ ] 没有任何真实密码出现在 `k8s/*.yaml`、`compose.yaml`、`Dockerfile`、`README.md`

---

## 6. 本地 / Compose / K8s 三套值对照

| 变量 | 本机直跑 | Compose | K8s |
|---|---|---|---|
| `DB_HOST` | `127.0.0.1` | `mysql` | `g12-mysql` |
| `DB_PORT` | `3306` | `3306` | `3306` |
| `DB_NAME` | `ctts_g12` | `ctts_g12` | `ctts_g12` |
| `BACKEND_HOST` | — | `backend` | `g12-backend` |
| `BACKEND_PORT` | — | `8080` | `8080` |

> 表里三列就是答辩会问的"同一套代码怎么跑三个环境"，答案：**只换环境变量，不重新打包**。
