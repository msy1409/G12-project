# 成员分工与贡献表（contribution-table.md）

> 负责人：B（后端）建表　填写：A / B / C 各自补自己那行　版本：v1.0
> commit 数统计口径：`git log --author="<提交者>" --oneline | wc -l`；下表 B 行统计截至提交 `c080c6c`，后续有新提交请自行更新。

| 成员 | commit 数 | 负责文件 | 负责模块 |
| --- | --- | --- | --- |
| B | 12 | `backend/`（controller / service / mapper / entity / dto / vo / common / config 全部 Java 源码、`Dockerfile`、`pom.xml`、`settings.xml`、`application.yml`）、`database/seed.sql`、`k8s/06-backend.yaml`、`k8s/09-hpa.yaml`、`docs/hpa-notes.md`、`compose.yaml`、`docs/api.md`、`evidence/02-compose/`（compose 运行与接口自查记录）、`.gitignore` | 后端业务接口与鉴权权限隔离、预约全生命周期状态流转、数据库种子数据、后端镜像构建、后端 K8s Deployment/Service 与 HPA、本地 compose 编排、接口自查证据 |
| C | 3 | `k8s/01-namespace.yaml`、`k8s/02-configmap.yaml`、`k8s/03-secret.example.yaml`、`k8s/04-pv-pvc.yaml`、`k8s/05-mysql.yaml`、`docs/runbook.md`、`docs/operation-record.md`、`docs/incident-record.md`、`docs/handoff-C.md`、`evidence/01-image/`、`evidence/03-k8s-deploy/`~`evidence/07-incident/`、`evidence/collect.sh` | K8s 底座（命名空间/配置/密钥结构/PV-PVC/MySQL）、MySQL 持久化与初始化验证、全栈部署与 Ingress 访问、发布 v1→v2 与异常发布/回滚/手动扩缩容演练、HPA 压测观察、F2（Service selector）故障复盘结案、证据目录与采集脚本 |
| A | 2（`102f8d9` + 业务流程/证据提交） | `frontend/` 全部（9 个页面、5 个复用组件、2 个图表组件、`src/api`、`src/router`、`src/stores`、`src/mock`、`Dockerfile`、`Dockerfile.mock`、`nginx.conf.template`、`.env.*`、`README.md`）、`k8s/07-frontend.yaml`、`k8s/08-ingress.yaml`、`docs/business-flow-record.md`、`evidence/business-flow/`、PPT/视频（待做） | 前端页面与交互、路由守卫与角色菜单、axios 请求/响应拦截、mock 联调与真接口切换、前端多阶段镜像与 nginx 反向代理、K8s 前端 Deployment/Service/Ingress、业务流程走查证据与演示材料 |

## 备注

- `database/schema.sql`、`docs/requirements.md`、`docs/database-design.md`、`docs/env-contract.md` 为初始契约骨架（init 提交），三方在评审中对齐，不计入上表某一方的独立产出。
- 镜像 tar（`g12-api-v1.tar`、`g12-api-v2.tar`）由 B 在虚拟机构建并回传，已被 `.gitignore` 排除，不进 Git 历史。
