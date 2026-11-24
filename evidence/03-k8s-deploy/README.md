# 03-k8s-deploy —— 底座与全栈部署

**证明**：A3（`kubectl apply --dry-run=client -f k8s/` 全过）、A4（Endpoints 非空 + Ingress 能访问）、A6（PVC Bound），以及“MySQL 初始化真的跑了”（6 张表 + ≥10 行）。

**采集顺序**（不能乱，后面依赖前面）：

| 顺序 | 文件 | 采集命令 | 判据 |
|---|---|---|---|
| 1 | `01-cluster-nodes.png` | `kubectl get nodes -o wide`、两个 addon 的 Pod、`minikube ip` | Ready + `1/1 Running` |
| 2 | `02-dry-run.png` | `kubectl apply --dry-run=client -f k8s/` | 全 `created (dry run)`，无 error |
| 3 | `03-pvc-bound.png` | `kubectl get pvc/pv -n enterprise-g12` | STATUS=`Bound` |
| 4 | `04-mysql-init-tables.png` | `kubectl exec ... show tables; select count(*)` | **6 表 + ≥10 行** |
| 5 | `05-get-all.png` | `kubectl get pods,svc,endpoints,pvc,ingress,hpa -n enterprise-g12 -o wide` | 全 Running、Endpoints 非空、Ingress 有 ADDRESS |
| 6 | `06-browser-login.png` | 浏览器 `http://g12.course.local:8080` | 带地址栏的登录页 |
| 7 | `07-browser-main.png` | 登录后主界面 | 有真实种子数据 |

**这个目录最容易翻车的地方**

- 3 号 Pending：多半是 `g12-local` StorageClass 不存在（`k8s/04-pv-pvc.yaml` 里已经补了）。
- 4 号 0 张表：`/var/lib/mysql` 不空，initdb 跳过。按 `docs/runbook.md §5` 清空 hostPath 重来。
- 5 号 Endpoints 空：Service selector 与 Pod 标签不一致 → 见 `evidence/07-incident/`。
- 5 号要采**两次**：A 的前端还没交时先采一张“mysql+backend+HPA 已 Ready、frontend 还没有”的（对应 `docs/runbook.md §7.1`），A 交付后再采一张全绿的（7.2），md 里注明哪张是哪次，别用旧的冒充新的。
- 6/7 号打不开：先确认 `kubectl port-forward` 那个终端没关、hosts 写的是 VM 的 IP 而不是 `minikube ip`。**这两张必须等 A 的前端镜像起来**，没有前端就没有登录页可截。
