# 成员分工与贡献表（contribution-table.md）

> 负责人：B（后端）建表　填写：A / B / C 各自补自己那行　版本：v1.0
> commit 数统计口径：`git log --author="<提交者>" --oneline | wc -l`；下表 B 行统计截至提交 `c080c6c`，后续有新提交请自行更新。

| 成员 | commit 数 | 负责文件 | 负责模块 |
| --- | --- | --- | --- |
| B | 9 | `backend/`（controller / service / mapper / entity / dto / vo / common / config 全部 Java 源码、`Dockerfile`、`pom.xml`、`settings.xml`、`application.yml`）、`database/seed.sql`、`k8s/06-backend.yaml`、`k8s/09-hpa.yaml`、`k8s/06-hpa/README.md`、`compose.yaml`、`docs/api.md`、`evidence/02-compose/`（compose 运行与接口自查记录）、`.gitignore` | 后端业务接口与鉴权权限隔离、预约全生命周期状态流转、数据库种子数据、后端镜像构建、后端 K8s Deployment/Service 与 HPA、本地 compose 编排、接口自查证据 |
| C | <待 C 填> | <待 C 填：k8s/01~05、08-ingress、docs/runbook.md、operation-record.md、incident-record.md、evidence/01、03~07> | <待 C 填：K8s 底座、MySQL 持久化、全栈部署、发布回滚演练、HPA 压测、F2 故障复盘、证据采集> |
| A | <待 A 填> | <待 A 填：frontend/、k8s/07-frontend.yaml、08-ingress.yaml（归属确认后）、PPT/视频> | <待 A 填：前端页面、nginx 反向代理、前端镜像、演示材料> |

## 备注

- `database/schema.sql`、`docs/requirements.md`、`docs/database-design.md`、`docs/env-contract.md` 为初始契约骨架（init 提交），三方在评审中对齐，不计入上表某一方的独立产出。
- 镜像 tar（`g12-api-v1.tar`、`g12-api-v2.tar`）由 B 在虚拟机构建并回传，已被 `.gitignore` 排除，不进 Git 历史。
