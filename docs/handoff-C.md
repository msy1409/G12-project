# C 的进度与交接清单（内部协作文档，不是交付物）

> 用途：说明 C 这边**已经做完什么**、**卡在等谁**、**要跟谁要什么**。
> 对应《G12 执行手册-优化版》第三章 1–36 条。最后更新：2026-11-13

---

## 1. 已完成（不需要任何人配合的部分）

| 手册条目 | 产出 | 状态 |
|---|---|---|
| 6 | `k8s/01-namespace.yaml` | ✅ 写完 |
| 7 | `k8s/02-configmap.yaml` | ✅ 写完，key 与 `docs/env-contract.md` 一致 |
| 8 | `k8s/03-secret.example.yaml` | ✅ 写完，值全 `CHANGE_ME`，只有结构进仓库 |
| 9 | `k8s/04-pv-pvc.yaml` | ✅ 写完，**额外补了 `StorageClass g12-local`**（否则 PVC 永久 Pending） |
| 10 | `k8s/05-mysql.yaml` | ✅ 写完，`strategy: Recreate` + 挂 `g12-mysql-init` |
| 30 | `docs/runbook.md` | ✅ 零到跑起来的完整命令序列 + 验收判据 + 故障速查 + **§7.1 前端未交时先分批部署** |
| 31 | `docs/operation-record.md` | ✅ 四次操作的三态记录骨架 + **v2 的 `apiVersion` 验证判据**，真实输出待部署后回填 |
| 32 | `docs/incident-record.md` | ✅ INC-G12-001 七段写完，证据输出待演练后回填 |
| 33 | `evidence/` 7 个目录 | ✅ 目录 + 36 份截图配套 `.md` 模板 + 每目录 README，**图待采集** |
| 33+ | `evidence/collect.sh` | ✅ 采集脚本：一条命令把 kubectl 输出按截图名落成 `evidence/_raw/*.txt`（已用桩命令跑通 23 份） |
| — | 08-ingress | ⚠️ 已备一份，**归属待确认**（见 §3） |

**没有做的事**：一条 K8s 命令都还没执行。按约定“部署等 C 本人操作”，集群由 C 自己起。

---

## 2. 交接状态（2026-11-16 最终核对后）

### 2.1 B 侧：原 7 项 + 文档尾巴 B-8，**全部关闭 ✅**

| # | 事项 | B 的答复 | C 的动作 |
|---|---|---|---|
| B-1 | 06 的 Service 端口 | **`port: 8080 → targetPort: 8080`** | 已把 runbook / operation-record / incident-record / evidence 里所有集群内 URL 改成 `http://g12-backend:8080/...`；手册原文第 27 条压测命令原样可用 |
| B-2 | 06/09 yaml push | 已 push（`fe4194f`；最新 `26ce9fc`） | `git pull` 后 `ls k8s/` 确认 |
| B-3 | `g12-api-v1.tar` | 本机根目录 + VM `/home/s/` 各一份，137.8MB | `docker load` + `minikube image load` |
| B-4 | `g12-api-v2.tar` | 同上，137.8MB | 同上；**用 image ID + 接口 `apiVersion` 双重校验**（见 §2.4） |
| B-5 | 06 的 resources | **requests 200m / 512Mi，limits 500m / 1Gi**（按手册口径，不再是 100m/500m） | HPA 有数；但 **60% × 200m = 120m/副本**，一个 busybox 打不到，压测要开 4–6 个并发 Pod，见 `evidence/06-hpa/README.md` |
| B-6 | SQL 不再改 | 6 表 / 12/14/10 行 | 初始化验收基线用这三个数 |
| B-7 | compose 证据 | ✅ 已交付 3 份：`01-compose-ps`、`02-health-db`、`03-api-check`（B 的编号与 C 模板不同） | C 的两份同名模板**已删除**，避免覆盖 B 的成品；缺的"前端第三服务"等 A |
| B-8 | `api.md` 补 `apiVersion` | ✅ 已补，代码 + `docs/api.md` 一起推到 `26ce9fc` | `git pull` 后 `grep apiVersion docs/api.md` 应搜到 |
| B-9 | 两张自评表 | ✅ B 建 `contribution-table.md`、`ai-usage-statement.md` 框架并填 B 行，A/C 留占位 | clone 后**只替换 C 的占位段**（素材见 §6），不要另建文件 |

> **口径已三方统一**：`g12-backend` Service = 8080 → 8080；A 的 nginx 模板 `BACKEND_PORT=8080`；C 的探针/压测/证据全用 `:8080`。**任何一处写成 80 都会 502。**

### 2.2 B 侧唯一未闭环：B-7（不是 B 的锅）

`evidence/02-compose/` 的“三服务 Up”图缺**前端服务**，因为 A 的前端代码/镜像还没交，compose 里第三个服务起不来。
→ **处置**：等 A 交付后由 B 重跑一次 `docker compose up -d` 并补图；C 只归档。**这不阻塞 C 的 K8s 主线**（K8s 里不需要 compose）。

### 2.3 A 侧（前端）—— 只剩一个 tar 包

A 在 `102f8d9` 交付了：`frontend/`（9 个页面 + 7 个组件 + 路由/拦截器/请求封装/MainLayout/mock）、`frontend/Dockerfile` + `Dockerfile.mock`、`nginx.conf.template`、`k8s/07-frontend.yaml`、`k8s/08-ingress.yaml`、`docs/business-flow-record.md`。

| # | 要什么 | 状态 |
|---|---|---|
| A-1 | `g12-web-v1.tar` | ❌ **仍缺，唯一阻塞** —— 镜像 tar 不进 git，必须 A 单独发 |
| A-2 | `k8s/07-frontend.yaml` | ✅ 已交付，但**内有 502 bug，C 已修**（见下） |
| A-3 | 08-ingress 归属 | ✅ 已定：A 写，用 A 的（见 §3） |
| A-4 | nginx 按 env 渲染 | ✅ 已验证：`proxy_pass http://${BACKEND_HOST}:${BACKEND_PORT};` |
| A-5 | `request.js` 的 `baseURL` | ✅ 已验证：`baseURL: '/api'` |

> **A-2 的 bug（重要，C 已在 `7021841` 修掉）**：A 的 07 里写
> `BACKEND_PORT: "80"`，注释理由是"B 的 06 是 port 80 / targetPort 8080"。
> 但 B 的 06 实际是 `port: 8080 → targetPort: 8080`，Service 上**根本没有 80 端口** —— 照 A 的写法，nginx 会 `connect() failed` 到 `g12-backend:80`，**全站 `/api` 502**。
> C 已改成 `"8080"` 并修正注释。**要通知 A**，别让他下次 pull 之后又改回去。

> **A 没交镜像之前 C 能做什么**：MySQL + 后端可以先跑（`kubectl apply -f k8s/01-06 -f k8s/09`，`kubectl port-forward svc/g12-backend 8080:8080` 验 27 个接口），runbook §7.1 有完整步骤，`evidence/01-image`（后端 2 张）、`03-k8s-deploy` 的 5 张、`04-logs-events` 的 6 张都能先采。

### 2.3.1 已推送记录（2026-11-24）

| commit | 内容 |
|---|---|
| `8c59155` | feat(C)：k8s 01–05、docs 四份、evidence 全量（52 个文件） |
| `7021841` | fix(k8s)：07-frontend 的 `BACKEND_PORT` 80 → 8080（C 修 A 的 bug） |

远端 `main` = `7021841`。`k8s/` 现有 01–09 齐全（06/07/09 是 B/A 的，08 是 A 的）；`docs/` 12 份；`evidence/` 7 个目录 + README + `collect.sh`。

### 2.4 我自己要做的两个校验（不需要问人）

1. **两个 tar 是不是真的两个版本**：两个包都是 137.8MB，大小一样正常（同依赖、只多一个字段），但要用 image ID 确认：
   ```bash
   docker load -i g12-api-v1.tar; docker load -i g12-api-v2.tar
   docker image inspect g12-api:v1 --format '{{.Id}}'
   docker image inspect g12-api:v2 --format '{{.Id}}'      # 必须不同
   ```
   相同 = 包发错，找 B 重打；不同才做发布演练。
2. **证明 v2 的改动真的生效**：B 已确认改动 = `UserVO` 新增 `apiVersion` 字段，`/auth/login`、`/auth/me` 返回 `"apiVersion":"v2"`，v1 基线没有该字段；`docs/api.md` 已同步（`26ce9fc`）。
   做法：`set image` **之前**先打一次登录接口留基线（grep 无输出、退出码 1），`set image` **之后**再打一次（grep 命中）。命令已写进 `docs/operation-record.md` OP-01 的「变更前 / 验证」两段和 `evidence/05-release-rollback/03-rollout-v2.md`。
   **这条同时是回滚层级的判据**：有 `apiVersion` = v2，没有 = v1。

### 2.5 已按仓库现状核对过（2026-11-24，commit `13bd88a`）

直接从 GitHub 拉了 `main` 的完整文件清单 + 关键文件读了一遍，确认这几件事（不用再问人）：

| 核对项 | 实际值 | 对 C 的影响 |
|---|---|---|
| `k8s/06-backend.yaml` | Service `8080→8080`；requests `200m/512Mi`、limits `500m/1Gi`；双探针 `/api/actuator/health:8080`；`envFrom g12-config` + `DB_PASSWORD`/`JWT_SECRET` 取自 `g12-secret`；replicas 2 | ✅ 我的 02-configmap 的 key、03-secret 的三个 key 完全对得上 |
| `k8s/09-hpa.yaml` | min 2 / max 4，CPU 60%；**`behavior`：scaleUp 60s 窗口 + `Pods +1/30s`；scaleDown 60s 窗口 + `Pods -1/60s`** | ⚠️ 扩容 2→3→4 要 **1.5–2 分钟**（压 30 秒没用）；缩容 **1–2 分钟**就开始，不是默认 5 分钟。已按此改 `evidence/06-hpa/README.md` |
| `docs/api.md` | 有 3 处 `apiVersion` | ✅ B-8 确认关闭 |
| `docs/contribution-table.md` | 已有表头，C 行是 `<待 C 填>` | 要替换那一行（素材见 §6） |
| `docs/ai-usage-statement.md` | B 已把 C 的占位素材贴进去（和 §6 里我写的基本一致） | 只需核对 + 补 commit 数，不用重写 |
| `evidence/02-compose/` | B 已交 `01-compose-ps`、`02-health-db`、`03-api-check` | C 两份同名模板已删，避免覆盖 |
| `frontend/` | 只有 `.gitkeep` | A 确实什么都没交 |
| `k8s/` | 只有 `06`、`09`、`.gitkeep`，另有一个 `k8s/06-hpa/README.md` | `kubectl apply -f k8s/` 不递归子目录，**不会**被当成 yaml 执行；只是位置放错了，提醒 B 挪到 `docs/` 或 `evidence/06-hpa/` |

---

## 3. 08-ingress 归属（已解决 ✅）

| 出处 | 说法 |
|---|---|
| 执行手册 第一章 第 21 条 | **A**（A 的 commit 命令里也含 `k8s/08-ingress.yaml`） |
| B 发的《执行手册入口》 | **C**（"底座：01–05/08"） |

**最终结论（2026-11-24 A 的 `102f8d9` 已交付）：A 写，用 A 的。**
A 的版本与 C 的兜底版等价（`ingressClassName: nginx`、host `g12.course.local`、`/` Prefix → `g12-frontend:80`），另外多一条 `nginx.ingress.kubernetes.io/proxy-body-size: "10m"`，更完整。
**C 的 `k8s/08-ingress.yaml` 未推送**（推送时显式跳过），本地文件也已替换成 A 的版本，避免两份不同内容并存。

> 教训记一条：归属没拍板前，两边各写一份会互相覆盖 —— 这次靠"推送前先比对远端路径"躲过去了。

---

## 4. C 要发给 A、B 的信息（部署起来之后）

等集群和全栈起来，C 在群里发这一段（**不用等他们问**）：

```
【G12 访问方式】
1) 我在 VM 上挂着一条 port-forward（这个终端不能关）：
   kubectl port-forward -n ingress-nginx --address 0.0.0.0 svc/ingress-nginx-controller 8080:80
2) 你们的 Windows hosts 加一行（用 VM 的 IP，不是 minikube ip）：
   <VM 的 192.168.x.x>    g12.course.local
3) 浏览器开：http://g12.course.local:8080
4) 账号：seed.sql 里的员工/审核员/管理员，密码统一 123456
5) 我这边的验证结论：
   - kubectl get pods,svc,endpoints,pvc,ingress,hpa -n enterprise-g12 全 Running
   - g12-mysql-pvc = Bound，ctts_g12 里 6 张表 + visitor_apply ≥10 行
   - Ingress ADDRESS 有值，页面能登录
```

> 为什么不是直接给 `minikube ip`：minikube 的 `192.168.49.2` 在 VMware NAT 里，Windows 路由不到；必须用 VM 的对外 IP + port-forward。详见 `docs/runbook.md §8`。

---

## 5. 集群起来后 C 的执行顺序（照抄即可）

1. `docs/runbook.md` §2 → §5：起集群、建 namespace/configmap/secret、灌 SQL、起来 MySQL（验 PVC Bound + 6 张表）
2. §6：load 两个镜像（**先比 v1/v2 的 image ID**，见 §2.4）
3. §7.1：前端还没到就先 `apply` 01–06 + 09，用 `kubectl port-forward` 打后端接口 → `./evidence/collect.sh deploy` + `logs`
4. A 的前端到了：§7.2 全量 `apply` → §8 port-forward + hosts → 浏览器能登录
5. `evidence/03-k8s-deploy/` 的 7 张图（`./evidence/collect.sh deploy` 出文本，图自己截）
6. `evidence/04-logs-events/` 6 张（`./evidence/collect.sh logs`）
7. `docs/operation-record.md` OP-01～OP-04：每步前后跑 `./evidence/collect.sh release <tag>`，图进 `evidence/05-release-rollback/`
8. `evidence/06-hpa/` 5 张（`./evidence/collect.sh hpa` + 手工压测与 `-w` 截图）
9. `docs/incident-record.md` INC-G12-001：注入后 `./evidence/collect.sh incident`
10. `git add k8s/ evidence/ docs/ && git commit -m "feat(C): k8s底座、部署、发布回滚、HPA、F2故障、证据" && git push`
11. 打开 B 建好的 `docs/contribution-table.md`、`docs/ai-usage-statement.md`，**只替换 C 的占位段**（素材见 §6，别另建文件）

---

## 6. 最后两张表的填报素材

> **分工已定（B 2026-11-20 确认）**：`docs/contribution-table.md` 和 `docs/ai-usage-statement.md` **由 B 建框架**，B 填自己那行，A、C 各留占位。
> **现状（2026-11-24 已核对仓库）**：
> - `contribution-table.md`：C 那行是 `<待 C 填>` 占位 → **整行替换**成下面这段；
> - `ai-usage-statement.md`：B 已经把 C 的占位素材贴进去了 → **不用重写，只核对 + 补 commit 数**。
> **不要另建文件**（会和新仓库里的同名文件冲突）。

**contribution-table.md 里 C 的行**（注意 `evidence/01~07` 里 **02 是 B 的**，别写成自己的）

```markdown
| C | <commit 数> | `k8s/01-namespace.yaml`、`k8s/02-configmap.yaml`、`k8s/03-secret.example.yaml`、`k8s/04-pv-pvc.yaml`、`k8s/05-mysql.yaml`、`k8s/08-ingress.yaml`（兜底）、`docs/runbook.md`、`docs/operation-record.md`、`docs/incident-record.md`、`evidence/01-image/`、`evidence/03-k8s-deploy/`~`evidence/07-incident/`、`evidence/collect.sh` | K8s 底座与 MySQL 持久化、全栈部署、发布回滚与扩缩容演练、HPA 压测、F2 故障复盘、证据采集 |
```

查自己的 commit 数（在 clone 好的仓库里执行，作者名要和 `git config user.name` 一致）：

```bash
git log --author="$(git config user.name)" --oneline | wc -l
```

**ai-usage-statement.md 里 C 的行**（按实际用过的工具改）

```markdown
### C（K8s 底座与运维）
- 用的 AI：DeepSeek（DSH 终端 Agent）
- 让它做的：k8s 底座 5 个 yaml 的初稿与注释、runbook/operation-record/incident-record 的骨架、
  evidence 36 份截图模板与采集脚本 evidence/collect.sh；以及排障思路（PVC Pending、initdb 不执行、
  selector 与 Endpoints、HPA 依赖 requests.cpu）
- 我自己改的：StorageClass 与 PVC 的 volumeName（AI 初稿没考虑 minikube 没有 g12-local 类）、
  Service 端口 8080→8080 的实际值、v2 的 apiVersion 判据、所有命令都在自己 VM 上跑过并按真实输出回填
- 能讲清楚的：Endpoints 是 selector 算出来的（F2 十秒定位）、Recreate 与 RWO 的关系、
  rollout undo 是退一层不是退到 v1、HPA 阈值 = requests × 60% = 120m
```

---

## 7. 这些文件怎么进仓库（桌面 → VM → GitHub）

**为什么现在 GitHub 上看不到 C 的东西**：桌面那份 `G12-project` 是 B 当初发的 zip，**没有 `.git`**，它只是文件副本。GitHub 只认"从 clone 出来的仓库里 commit 并 push 上去"的内容，本地文件夹放着不动，远端永远不会变。

**千万别做的三件事**：
1. 别在桌面这份里 `git init` 再 `git push --force` —— 会造出一条和组员毫无关系的历史，把 B 的 9 个 commit 冲掉。
2. 别用 GitHub 网页的 "Upload files" 传整个目录 —— 中文文件名、嵌套目录、`.sh` 执行位都会出问题，而且会生成一堆零散提交。
3. 别 `rsync --delete` 覆盖 —— 会删掉 A/B 已提交的文件。

**正确做法（3 步，在 VM 里做）**：

```bash
# ① 把桌面的 G12-C-交付.zip 用 FinalShell 拖到 VM 的 ~/ 下，然后解开覆盖到 clone 里
cd ~
unzip -o G12-C-交付.zip -d G12-project     # 解出来是 k8s/ docs/ evidence/ 三个目录，覆盖同名文件
cd G12-project

# ② 先看清单，确认只有"新增"，没有"修改别人的文件"
git status --short
git diff --stat
```

预期 `git status` 里是这些（**全是 `??` 新增**，没有 ` M` 修改）：

```
?? docs/runbook.md
?? docs/operation-record.md
?? docs/incident-record.md
?? docs/handoff-C.md
?? k8s/01-namespace.yaml
?? k8s/02-configmap.yaml
?? k8s/03-secret.example.yaml
?? k8s/04-pv-pvc.yaml
?? k8s/05-mysql.yaml
?? k8s/08-ingress.yaml
?? evidence/README.md
?? evidence/collect.sh
?? evidence/01-image/ ...
?? evidence/03-k8s-deploy/ ...
...
```

> **只要看到 `M docs/api.md`、`M docs/contribution-table.md`、`D evidence/02-compose/01-compose-ps.png` 这种行，立刻 `git checkout -- <该文件>` 撤回**，说明覆盖错了。
> C 的 zip 里**故意不含** `docs/api.md`、`contribution-table.md`、`ai-usage-statement.md`、`evidence/02-compose/*.md`（B 的资产），也不会覆盖 `k8s/06`、`k8s/09`（B 的）。

```bash
# ③ 提交并推送
chmod +x evidence/collect.sh          # Windows 传过来会丢执行位
git add k8s docs evidence
git commit -m "feat(C): k8s底座（01-05/08）、runbook、发布回滚与故障复盘文档、证据目录与采集脚本"
git push
```

推完在 GitHub 网页刷新确认：`k8s/` 里应有 01–09（07 等 A），`docs/` 里能看到 runbook.md，`evidence/` 下 7 个目录。

> `evidence/_raw/` 里的 txt 是采集产物，一起提交（文本副本比截图好查）。
> 后续每次采完证据，重复 ①③ 的 `git add evidence/ && git commit` 即可。
