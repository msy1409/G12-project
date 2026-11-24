# 02-compose —— Compose 编排（B 的交付，C 只归档）

**证明**：A2（compose 编排、健康检查、库表自检）。

**这个目录已经由 B 做完了，C 不再往这里写文件。** 仓库现有：

| 文件 | 内容 |
|---|---|
| `01-compose-ps.md` + `.png` | `docker compose ps`：mysql / backend 两服务 Up + healthy，网络 `g12-net`，卷 `g12-mysql-data` |
| `02-health-db.md` + `.png` | 健康检查与库表自检 |
| `03-api-check.md` | 接口自查记录（暂无 png） |

> C 原先准备的两份同名模板（`01-compose-ps.md`、`02-compose-mysql-recreate.md`）**已删除**，否则会把 B 填好的成品覆盖成空模板。

**唯一缺口**：compose 里第三个服务 `frontend` 没起来（A 的前端代码/镜像还没交）。等 A 交付后由 B 补跑 `docker compose up -d` 并补一张三服务齐全的图，C 不参与。

**为什么 C 不自己跑一份**：C 的机器要跑 minikube，VM 的 `8080` 已被 Ingress 的 port-forward 占用，重跑 compose 会抢端口；更何况 A2 考的是"compose 编排"，由 B 的机器出图更真实。
