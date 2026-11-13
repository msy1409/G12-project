# 01 · compose 编排状态（docker compose ps）

- **采集时间**：2026-11-12
- **采集环境**：VirtualBox `docker` 虚拟机（Docker 29.8.1 / Compose v5.5.0）
- **来源命令**：
  ```bash
  cd /home/s/g12 && docker compose ps
  ```
- **观察结论**：
  - `g12-backend`（镜像 `g12-api:v1`）：Up，healthy，容器内监听 8080。
  - `g12-mysql`（镜像 `mysql:8.0`）：Up，healthy，内部 3306。
  - 两服务接入自定义网络 `g12-net`，MySQL 数据落在具名卷 `g12-mysql-data`。
- **说明**：本图仅启动 `mysql` + `backend` 两个服务；前端服务 `g12-web`（`07-frontend.yaml` / `g12-web:v1`）属于前端 A 的交付物，前端代码尚未提交，本次 compose 未纳入。待前端镜像产出后，再 `docker compose up -d` 补齐第三服务并补截图。
