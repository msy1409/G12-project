# 02 · 后端健康检查、数据库表与行数、v1/v2 镜像

- **采集时间**：2026-11-12
- **采集环境**：VirtualBox `docker` 虚拟机
- **来源命令**：
  ```bash
  # 后端健康检查（容器内访问，后端 8080 未对宿主映射）
  docker exec g12-backend wget -qO- http://127.0.0.1:8080/api/actuator/health

  # 数据库表与行数核对
  docker exec g12-mysql mysql -uroot -p****** ctts_g12 -e 'show tables; select count(*) ...'

  # 本地后端镜像
  docker images | grep g12-api
  ```
- **观察结论**：
  - 后端健康检查返回 `{"status":"UP"}`，后端可正常连接数据库。
  - 数据库 `ctts_g12` 共 6 张表：`apply_process_record`、`biz_status_change_log`、`sys_department`、`sys_user`、`visitor_apply`、`visitor_record`。
  - 行数：`sys_user` 12、`visitor_apply` 14、`visitor_record` 10，与 `seed.sql` 一致。
  - 后端镜像 `g12-api:v1`、`g12-api:v2` 均已在本地构建完成。
- **持久化复核**：执行 `docker compose stop mysql && docker compose rm -f mysql && docker compose up -d mysql` 删除并重建 MySQL 容器后，具名卷内数据保留（12 / 14 / 10 仍在）。
- **说明**：MySQL 命令中的密码已打码；前端服务状态见 `01-compose-ps.md` 说明。
