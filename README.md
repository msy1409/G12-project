# G12 访客预约与到访核验平台

Docker 云容器与管理技术 · 期末课程设计 · Group 12

## 目录结构

```
G12-project/
├─ frontend/          前端 Vue3 源码（A 负责）：11 个页面、7 个组件、axios 封装、路由守卫、mock 适配器
├─ backend/           后端 Spring Boot 源码（B 负责）：28 个业务接口、鉴权状态机、实体/Mapper/VO
├─ database/         schema.sql + seed.sql（B 负责）
├─ k8s/               全部 K8s yaml（C 01–05、08；B 06+09；A 07）
├─ evidence/          截图与命令输出证据 01–07（C 采集）
├─ docs/              设计契约、Runbook、操作/事故记录、分工与 AI 声明
├─ compose.yaml       本地整套编排（B 负责）
├─ .env.example       本地环境变量示例（复制为 .env，.env 不入库）
└─ README.md          本文件
```

## 当前进度

| 模块 | 状态 | 说明 |
|---|---|---|
| 后端代码 | 已完成 | 28 个业务接口、三角色权限隔离、预约状态流转、SQL/seed、v1/v2 镜像（含 AUDITOR 403 与创建预约返回体修复） |
| 前端代码 | 已完成 | 页面/组件/路由守卫/拦截/mock 齐全，已用真实后端联调跑通预约全流程；生产镜像 `VITE_USE_MOCK=false` |
| K8s 部署文件 | 已完成 | `k8s/01–09` yaml 齐，端口口径 8080→8080 |
| 镜像产物 | 已就位 | `g12-web:v1`、`g12-api:v1`、`g12-api:v2` 的 tar 在仓库根目录（`.gitignore` 已排除 `*.tar`，不进 Git） |
| K8s 现场演练与证据 | 待 C | minikube 全栈部署、v1→v2 发布/回滚、异常发布、扩缩容、HPA 压测、故障复盘的真实输出与截图回填 `evidence/03–07` |
| 演示材料 | 待 A | PPT、5 分钟演示视频 |

## 本地启动（Docker Compose）

前置：本机已装 Docker Desktop / Docker Compose。

```bash
# 1. 准备本地环境变量（.env 已被 .gitignore 忽略，不会入库）
cp .env.example .env
#    编辑 .env：填 MYSQL_ROOT_PASSWORD、JWT_SECRET（≥32 位随机串）

# 2. 起整套（mysql + backend + frontend）
docker compose up -d

# 3. 查看状态
docker compose ps
curl http://127.0.0.1:8080/api/actuator/health    # 后端健康检查：{"status":"UP"}
```

- 前端只对外暴露 **8080**，浏览器访问：**http://localhost:8080**
- 后端、MySQL 仅在 Compose 内部网络可达，不对外暴露。
- 演示账号见 `database/seed.sql`，密码统一 `123456`：`admin`（管理员）、`auditor01`（审核员）、`emp01`（员工）。

## K8s 部署（minikube）

完整步骤见 `docs/runbook.md`（装环境 → 起集群与 addon → Secret/灌 SQL → 加载镜像 → 部署业务层 → 浏览器访问）。关键命令：

```bash
# 加载镜像（tar 在仓库根目录）
docker load -i g12-api-v1.tar
docker load -i g12-web-v1.tar
minikube image load g12-api:v1
minikube image load g12-web:v1

# 按底座 → 后端/HPA → 前端/Ingress 顺序 apply
kubectl apply -f k8s/01-namespace.yaml
kubectl apply -f k8s/02-configmap.yaml
kubectl apply -f k8s/04-pv-pvc.yaml
kubectl apply -f k8s/05-mysql.yaml
kubectl apply -f k8s/06-backend.yaml
kubectl apply -f k8s/09-hpa.yaml
kubectl apply -f k8s/07-frontend.yaml
kubectl apply -f k8s/08-ingress.yaml

# 全量验收
kubectl get pods,svc,ingress,pvc,hpa -n enterprise-g12
```

- 访问域名：`g12.course.local`；经 `kubectl port-forward` 映射后浏览器开 **http://g12.course.local:8080**（详见 runbook §8）。
- Secret 真值只在部署现场敲，不写进任何仓库文件；结构见 `k8s/03-secret.example.yaml`。

## 关键约定

- 接口数 **28**；`approve` 只做 0→1，`verify-code` 单独做 1→2。
- 状态机：`0→{1,9}`、`1→{2,9}`、`2→{3,9}`、`3→{4}`，4 与 9 为终态。
- 每次状态变更必须同事务写 `biz_status_change_log` + `apply_process_record`。
- 命名：Namespace `enterprise-g12`、Host `g12.course.local`、镜像 `g12-web:v1` / `g12-api:v1` / `g12-api:v2`、库 `ctts_g12`。
- 敏感项（口令、JWT 密钥）只走环境变量/Secret，不进代码、不进 yaml 明文。
- 后端 Service 与前端 `BACKEND_PORT` 统一 **8080**；探针路径 `/api/actuator/health`。

## 打包

```bash
# Linux / macOS
zip -r G12-project.zip G12-project -x "*/node_modules/*" -x "*/target/*" -x "*/.git/*" -x "*.tar"
```

Windows 直接右键 `G12-project` → 压缩为 ZIP；压缩前删掉 `node_modules`、`target`、`.git`，镜像 tar 单独交付、不打进源码包。
