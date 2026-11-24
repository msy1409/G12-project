# evidence/ 证据目录总说明

> 负责人：C。每个子目录对应一类评分点，**每张截图必须有一个同名 `.md`**（手写或从模板补）。
> 状态（2026-11-24 已对过仓库）：C 的模板 34 份就位，图片待部署后采集；`02-compose/` 已由 B 交付 3 份，C 不再写该目录。

## 1. 目录与评分点对照

| 目录 | 证明什么 | 对应评分点 | 图数 | 采集人 |
|---|---|---|---|---|
| `01-image/` | 两个镜像构建完、非 latest、被 load 进集群节点 | A1 | 3 | C |
| `02-compose/` | Compose 编排、healthy、库表自检 | A2 | 3 | **B（已完成，C 只归档）** |
| `03-k8s-deploy/` | 集群/addon 就绪、dry-run 全过、PVC Bound、6 表初始化、全栈 Running、Ingress 能访问 | A3 / A4 / A6 | 7 | C |
| `04-logs-events/` | 三个工作负载日志、Pod 详情、事件流 | A9 前置 | 6 | C |
| `05-release-rollback/` | 发布 v2、异常发布、回滚、手动 scale 三态 | A7 | 8 | C |
| `06-hpa/` | HPA 初始、metrics 有数、2→3→4、回落 | A8 | 5 | C |
| `07-incident/` | F2 故障现象→定位→修复→恢复 | A9 | 6 | C |

> **`02-compose/` 的现状**：B 已按自己的编号交付 `01-compose-ps`、`02-health-db`、`03-api-check`（含 2 张 png），
> 与 C 模板的命名不同，**C 那两份 `01-compose-ps.md` / `02-compose-mysql-recreate.md` 已删除**，避免覆盖 B 的成品。
> 缺的只有"前端第三个服务 Up"，等 A 交前端后由 B 补一张即可。

## 2. 命名规范（改了会乱）

```
evidence/<目录两位编号>-<英文短名>/<两位序号>-<英文短名>.png
evidence/<目录两位编号>-<英文短名>/<两位序号>-<英文短名>.md
```

- 图片和 md **同名不同后缀**，这是硬要求（“每张截图配一个同名 .md”）。
- 失败过的步骤留一张 `-fail` 后缀的图 + md，复盘对比价值比成功图更高。
- 不出现中文文件名，避免 `git` 在 Windows/Linux 之间换来换去时乱码。

## 3. 每份 `.md` 必须有的六项

| 字段 | 要求 |
|---|---|
| 图片 | 文件名，和本 md 同名 |
| 采集时间 | `YYYY-MM-DD HH:MM`（Asia/Shanghai），和截图里的时间戳对得上 |
| 环境 | 集群名 + namespace |
| 关联文档 | 指向 `docs/runbook.md §x` 或 `docs/operation-record.md OP-0x` |
| 对应命令 | **原样**贴，别人能复制复现 |
| 观察结论 | 一句话：看到什么现象 + 说明什么问题。禁止“运行成功”“一切正常” |

## 4. 打码规则（A5 只看这一条）

- 密码：`MYSQL_ROOT_PASSWORD`、`DB_PASSWORD`、`-p'xxx'` 里的值，一律遮掉。
  **更省事的办法**：命令里用 `-p` 交互输入，或 `-p"$MYSQL_ROOT_PASSWORD"` 用环境变量，输出里就不会出现明文。
- token：浏览器 DevTools 里的 `Authorization: Bearer eyJ...` 不要截进去；必须截就遮住 `eyJ` 之后。
- kubeconfig / `~/.kube/config`、GitHub PAT、SSH 私钥：**永远不出现**。
- `kubectl get secret -o yaml` 不要截（哪怕 base64，也是能解出来的）。

## 5. 采集手法（少走弯路）

**先跑 `./evidence/collect.sh`，别手敲。** 它按截图文件名把输出落成 `evidence/_raw/<目录>_<截图名>.txt`，文件头自带采集时间、namespace、命令、退出码，配图时整段贴进同名 `.md` 的「实际输出」即可：

```bash
chmod +x evidence/collect.sh          # 第一次
./evidence/collect.sh deploy          # 03-k8s-deploy 的 5 份文本
./evidence/collect.sh logs            # 04-logs-events 的 6 份
./evidence/collect.sh hpa             # 06-hpa 的 3 份
./evidence/collect.sh release before-v2   # 发布前快照（tag 自己起）
./evidence/collect.sh release after-v2    # 发布后再跑，两份对比 = 三态记录
./evidence/collect.sh incident        # F2 注入后的定位证据
```

脚本只读不写集群（除了 `mysql -e` 的 SELECT），跑多少遍都安全；某一步 FAIL 会打印退出码，不会中断后面的采集。

需要手工截图的只剩：**浏览器页（登录页/主界面/502 现场）**、**`kubectl get hpa -w` 的实时滚动**、**`rollout status` 的滚动过程**、**F2 修复后的恢复验证**。

```bash
# 不想用脚本时，至少把输出同时存文本
kubectl get pods,svc,endpoints,pvc,ingress,hpa -n enterprise-g12 -o wide | tee /tmp/e3-05.txt
kubectl describe pod <pod> -n enterprise-g12 | tee /tmp/e4-04.txt
kubectl get events -n enterprise-g12 --sort-by=.lastTimestamp | tail -25 | tee /tmp/e4-05.txt
```

- 截图工具：FinalShell 自带截图、Windows `Win+Shift+S`；**终端字号调大**，一行不要折行。
- 一张图只讲一件事。`get pods,svc,endpoints,pvc,ingress,hpa` 是个例外，那张必须完整。
- 时间戳要和 md 里的“采集时间”对得上（评委真的会看）。

## 6. 提交前自查

```bash
ls evidence/*/ | wc -l                       # 图片 + md 各 36 左右
find evidence -name '*.png' | wc -l          # 和 .md 数一致（模板 md 允许暂时没有 png）
git status --short evidence/
grep -rniE 'password|pass2026|eyJ' evidence/ | grep -v CHANGE_ME   # 必须无输出
```

最后一条命令**必须无输出**，否则就是泄露了密码或 token。
