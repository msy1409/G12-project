# G12 前端（frontend）

负责人：A　技术栈：Vue 3 + Vite + Vue Router 4 + Pinia + Axios + Element Plus
环境要求：Node **22+**（Element Plus 2.14 依赖 `@vueuse/core@15`，Node 20 会在构建时报警告）

## 启动

```bash
cd frontend
npm install
npm run dev      # http://localhost:5173
npm run build    # 产物在 dist/
npm run preview
```

开发环境默认 `VITE_USE_MOCK=true`（见 `.env.development`），全部 27 个业务接口由
`src/mock/adapter.js` 按 `docs/api.md` 在浏览器内模拟，数据落 localStorage。
联调真实后端时改成 `VITE_USE_MOCK=false`，请求走 Vite 代理 `/api → http://localhost:8080`。

## 演示账号

| 角色 | 用户名 | 密码 | 登录后首页 |
|---|---|---|---|
| 员工 EMPLOYEE | `emp01` | `123456` | 我的预约 |
| 审核员 AUDITOR | `auditor01` | `123456` | 审核管理 |
| 管理员 ADMIN | `admin` | `123456` | 数据统计 |

右上角菜单可「重置演示数据」，恢复到种子状态。

## 9 个页面 ↔ 接口

| 页面 | 路由 | 角色 | 接口 |
|---|---|---|---|
| Login | `/login` | 公开 | `POST /auth/login`、`GET /auth/me` |
| ApplyNew | `/apply/new` | EMPLOYEE | `POST /applies`、`GET /departments` |
| MyApplies | `/apply/mine` | EMPLOYEE | `GET /applies/mine`、`GET /applies/{id}`、`PUT /applies/{id}/cancel` |
| AuditList | `/audit/list` | AUDITOR/ADMIN | `GET /applies/pending`、`PUT approve/reject`、`GET /applies` |
| AuditDetail | `/audit/{id}` | AUDITOR/ADMIN | `GET /applies/{id}`、approve、reject、`PUT /applies/{id}/verify-code` |
| GateCheck | `/gate` | AUDITOR/ADMIN | `POST /records/checkin`、`POST /records/checkout`、`GET /records` |
| AdminUsers | `/admin/users` | ADMIN | `/users` CRUD + `/departments` |
| AdminDepts | `/admin/depts` | ADMIN | `/departments` CRUD |
| StatsDashboard | `/stats` | AUDITOR/ADMIN | `/stats/overview`、`/stats/trend`、`/stats/department` |

## 5 个复用组件

`StatusTag`（0/1/2/3/4/9 彩签）、`PageHeader`、`DataTable`（表格 + 分页 + loading）、
`ConfirmDialog`、`PermissionButton`；另有 `SimpleLineChart`、`SimpleBarChart` 两个图表组件。

## 目录

```
frontend/
├─ src/
│  ├─ api/           request.js（axios 实例 + 拦截器）、index.js（27 接口封装）
│  ├─ components/    5 个复用组件 + 2 个图表组件
│  ├─ constants/     状态、角色、业务错误码字典
│  ├─ layouts/       MainLayout.vue（按角色渲染菜单）
│  ├─ mock/          db.js 种子数据、adapter.js 接口模拟
│  ├─ router/        9 页面路由 + meta.roles + 守卫
│  ├─ stores/        Pinia 用户态
│  ├─ utils/         日期与时长格式化
│  └─ views/         9 个页面 + 403/404
├─ nginx.conf.template
├─ Dockerfile
└─ .env.development / .env.production
```

## 镜像

```bash
docker build -t g12-web:v1 .
docker save g12-web:v1 -o g12-web-v1.tar
```

镜像内 nginx 用 `${BACKEND_HOST}` / `${BACKEND_PORT}` 模板，Compose 传 `backend`，
K8s 传 `g12-backend`，同一镜像两套环境不重打。

## 无后端演示

后端还没起来时，用 mock 镜像即可完整点完 9 个页面：

```bash
docker build -f Dockerfile.mock -t g12-web:mock .
docker run -d --name g12-web-demo -p 8080:80 g12-web:mock
# 浏览器打开 http://localhost:8080
```

