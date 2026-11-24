# G12 部署 Runbook（C · K8s 底座 → 全栈上线）

> 用途：换一台机器，照着本文从上往下敲，能把 `enterprise-g12` 命名空间从零跑到浏览器能登录。
> 负责人：C　　目标环境：VMware 中 Ubuntu 24.04 虚拟机（8 vCPU / 10 GB），minikube（docker driver）
> 最后更新：2026-11-12　　（对应《G12 执行手册-优化版》第三章 1–21 条）

---

## 0. 参数总表（全文都用这几个名字，改一处要全文改）

| 项 | 值 | 谁定 |
|---|---|---|
| 命名空间 | `enterprise-g12` | C |
| 集群 | minikube，docker driver，4 核 / 6 GiB | C |
| MySQL | `mysql:8.0`，Deployment/Service `g12-mysql`，3306 | C |
| 后端镜像 | `g12-api:v1`（v2 用于发布回滚） | B 构建 |
| 前端镜像 | `g12-web:v1` | A 构建 |
| 后端 Service | `g12-backend`，port **8080** → targetPort **8080** | B（2026-11-13 确认，**不是手册原文的 80**） |
| 前端 Service | `g12-frontend`，port **80** → targetPort **80** | A |
| Ingress | `g12-ingress`，host `g12.course.local`，pathType Prefix → `g12-frontend:80` | A/C（见 §9 归属说明） |
| 健康检查路径 | `/api/actuator/health`（后端 `context-path: /api`） | B |
| 后端 resources | requests **200m / 512Mi**，limits **500m / 1Gi** | B（HPA 60% × 200m = **120m/副本** 才触发，压测要加并发，见 `evidence/06-hpa/README.md`） |
| v2 的可见改动 | `/auth/login`、`/auth/me` 的返回里多 `apiVersion:"v2"` | B（发布/回滚的验证判据） |
| 库 / 账号 | `ctts_g12` / `root`；密码只在 Secret 里 | C |

**三条契约**（见 `docs/env-contract.md`，key 名一个字都不能改）：
`g12-config` 提供 `DB_HOST/DB_PORT/DB_NAME/DB_USER`，`g12-secret` 提供 `MYSQL_ROOT_PASSWORD/DB_PASSWORD/JWT_SECRET`，探针必须是 `/api/actuator/health`。

---

## 1. 装环境（一次性）

```bash
# 1.1 docker
sudo apt update
sudo apt install -y docker.io conntrack
sudo systemctl enable --now docker
sudo usermod -aG docker $USER
newgrp docker          # 或者退出 SSH 重连
docker run --rm hello-world | head -3      # 看到 Hello from Docker! 才算过

# 1.2 kubectl
sudo snap install kubectl --classic
kubectl version --client

# 1.3 minikube（下不动就 Windows 浏览器下载 minikube-linux-amd64，用 FinalShell 拖进 ~/ 再 install）
curl -LO https://github.com/kubernetes/minikube/releases/latest/download/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube
minikube version

# 1.4 git
sudo apt install -y git
git config --global user.name  "你的名字"
git config --global user.email "你的邮箱"
```

**验收**：`docker run --rm hello-world` 有输出；`kubectl version --client` 打印 Client Version；`minikube version` 打印版本号。

---

## 2. 起集群 + 两个 addon

```bash
minikube start --driver=docker --cpus=4 --memory=6144
kubectl get nodes                 # STATUS = Ready

minikube addons enable ingress
minikube addons enable metrics-server

kubectl get pods -n ingress-nginx                 # ingress-nginx-controller  1/1 Running
kubectl get pods -n kube-system | grep metrics    # metrics-server  1/1 Running
minikube ip                                       # 记下来，通常是 192.168.49.2
```

- VM 只有 10 GB 内存，`--memory=8192` 会把系统挤死；**用 6144**，手册写的 8192 在这台机器上不安全。
- 拉 kicbase / k8s 镜像卡住（国内常见）：`minikube delete && minikube start --driver=docker --cpus=4 --memory=6144 --image-mirror-country=cn`
- metrics-server 起来后要等 1–2 分钟 `kubectl top pods` 才有数，`error: Metrics API not available` 先别慌。

**验收**：`kubectl get nodes` 有 Ready 节点；两个 addon 的 Pod 都是 `1/1 Running`；`minikube ip` 有值。
**这一步截图 → `evidence/03-k8s-deploy/`（集群与 addon 三张）**。

---

## 3. 拿代码

```bash
cd ~
git clone https://github.com/msy1409/G12-project.git
cd G12-project
git log --oneline -5
ls k8s/ docs/ database/
```

- 私有库要密码时：**填 GitHub PAT**（Settings → Developer settings → Personal access tokens，勾 `repo`），不是登录密码。
- `k8s/` 里应该有：`01-namespace.yaml`、`02-configmap.yaml`、`03-secret.example.yaml`、`04-pv-pvc.yaml`、`05-mysql.yaml`（C 的）、`06-backend.yaml`、`09-hpa.yaml`（B 的）、`07-frontend.yaml`（A 的）、`08-ingress.yaml`。
- **缺谁的文件就等谁 push，不要自己造 06/07/09**，字段和镜像名对不上会白折腾。

---

## 4. C 交付的 6 个底座文件分别干什么

| 文件 | 内容 | 关键点 |
|---|---|---|
| `01-namespace.yaml` | `enterprise-g12` 命名空间 | 后面所有命令都带 `-n enterprise-g12` |
| `02-configmap.yaml` | `g12-config`：4 个 DB 变量 + `SERVER_PORT` + `JWT_EXPIRE_SECONDS` | key 名和 env-contract 一字不差 |
| `03-secret.example.yaml` | `g12-secret` 的结构，值全 `CHANGE_ME` | **只有这个进仓库**，真值 §5 现场敲 |
| `04-pv-pvc.yaml` | StorageClass `g12-local` + PV `g12-mysql-pv`(5Gi, hostPath) + PVC `g12-mysql-pvc` | 多了 StorageClass，理由见 §5 注 |
| `05-mysql.yaml` | Deployment `g12-mysql` + Service `g12-mysql:3306` | `strategy: Recreate`；挂 `g12-mysql-init` |
| `08-ingress.yaml` | Ingress `g12.course.local` → `g12-frontend:80` | 归属见 §9 |

> **为什么 `04` 里要自己建 StorageClass**：minikube 默认只有 `standard` 类，`g12-local` 不存在时 PVC 没有 provisioner，会一直 `Pending`。补一个 `no-provisioner` 的类，PVC 才能去绑手写的静态 PV。另外 PVC 写了 `volumeName`，保证只绑 `g12-mysql-pv`。

---

## 5. 建 Secret + 灌 SQL（顺序不能反）

`05-mysql.yaml` 挂了两样东西：PVC，和 ConfigMap `g12-mysql-init`。**ConfigMap 没建就 apply，Pod 会一直 `ContainerCreating`。**

```bash
# 5.0 命名空间 + 配置
kubectl apply -f k8s/01-namespace.yaml
kubectl apply -f k8s/02-configmap.yaml

# 5.1 真实 Secret（只在本机敲；值走环境变量，**不要把明文写进任何文件**）
#     这两个变量只活在这个 shell 里，别 export 到 ~/.bashrc，别贴群里
read -rsp 'MySQL/DB 密码: ' DB_PASS && echo
read -rsp 'JWT 密钥（≥32位随机串）: ' JWT_S && echo
kubectl create secret generic g12-secret -n enterprise-g12 \
  --from-literal=MYSQL_ROOT_PASSWORD="$DB_PASS" \
  --from-literal=DB_PASSWORD="$DB_PASS" \
  --from-literal=JWT_SECRET="$JWT_S" \
  --dry-run=client -o yaml | kubectl apply -f -
unset DB_PASS JWT_S        # 用完立刻从 shell 里清掉

# 5.2 B 的建表 + 种子 SQL 灌进 ConfigMap（文件必须在 database/ 下）
kubectl create configmap g12-mysql-init -n enterprise-g12 \
  --from-file=schema.sql=database/schema.sql \
  --from-file=seed.sql=database/seed.sql

# 5.3 存储 + MySQL
minikube ssh -- sudo mkdir -p /data/g12-mysql
minikube ssh -- sudo chmod 777 /data/g12-mysql     # mysql 容器以 uid 999 写 datadir，755 会 permission denied
kubectl apply -f k8s/04-pv-pvc.yaml
kubectl apply -f k8s/05-mysql.yaml
```

**验收三条（缺一条不算过）**：

```bash
kubectl get pvc -n enterprise-g12                  # g12-mysql-pvc   Bound
kubectl get pods -n enterprise-g12                 # g12-mysql-xxx   1/1 Running（约 40–90s）
kubectl get endpoints g12-mysql -n enterprise-g12  # ENDPOINTS 有 10.x.x.x:3306
```

**初始化到底跑没跑（最关键的一步）**：

```bash
# 密码不写在命令行上，直接读容器自己的环境变量（截图时也不会泄露）
kubectl exec -it deploy/g12-mysql -n enterprise-g12 -- \
  sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "use ctts_g12; show tables; select count(*) from visitor_apply;"'
```

期望：`sys_department / sys_user / visitor_apply / visitor_record / apply_process_record / biz_status_change_log` **6 张表**，`visitor_apply` **≥10 行**。

> **表是空的怎么办**：mysql 镜像只在 `/var/lib/mysql` 为空时执行 `/docker-entrypoint-initdb.d/*.sql`。之前失败留下的空库也算“不空”。清空重来：
> ```bash
> kubectl delete -f k8s/05-mysql.yaml
> minikube ssh -- sudo find /data/g12-mysql -mindepth 1 -delete   # 连隐藏文件一起删
> kubectl apply -f k8s/05-mysql.yaml
> ```
> 还不行先看日志：`kubectl logs deploy/g12-mysql -n enterprise-g12 | head -60`，找 `ERROR` 和 `running /docker-entrypoint-initdb.d`。

**这一步截图 → `evidence/03-k8s-deploy/`（PVC Bound、6 张表、endpoints 三张）**。

可以提前 push 一次，别攒到最后：

```bash
git add k8s/ && git commit -m "feat(C): k8s 底座 namespace/configmap/secret/pv-pvc/mysql" && git push
```

---

## 6. 加载 A、B 交来的镜像

拿到 `g12-api-v1.tar`、`g12-web-v1.tar`（FinalShell 拖到 `~/G12-project/`）：

```bash
docker load -i g12-api-v1.tar        # 期望最后一行 Loaded image: g12-api:v1
docker load -i g12-web-v1.tar        # 期望 Loaded image: g12-web:v1

minikube image load g12-api:v1
minikube image load g12-web:v1

minikube image ls | grep g12         # docker.io/library/g12-api:v1 和 g12-web:v1 都在
```

- `minikube image load` 走的是 `docker save | minikube ssh docker load`，两个镜像各要几十秒到几分钟，别 Ctrl+C。
- 只 `docker load` 不 `minikube image load`，集群里 Pod 会 `ErrImageNeverPull` / `ImagePullBackOff`。

**截图 → `evidence/01-image/`**。

---

## 7. 部署业务层（06/07/08/09）

### 7.1 如果 A 的前端还没交（2026-11-13 的现状）—— 先分两批部署

后端和库不依赖前端，可以先上，边等 A 边把能验的都验掉：

```bash
git pull
ls k8s/                                  # 应有 01–06、09；07/08 可能还没来

kubectl apply --dry-run=client -f k8s/01-namespace.yaml -f k8s/02-configmap.yaml \
  -f k8s/04-pv-pvc.yaml -f k8s/05-mysql.yaml -f k8s/06-backend.yaml -f k8s/09-hpa.yaml
kubectl apply -f k8s/06-backend.yaml -f k8s/09-hpa.yaml
kubectl get pods -n enterprise-g12 -w          # g12-backend-* 两个变 1/1 Running 后 Ctrl+C
```

**没有前端也能验后端**（三种方式，用第二种最省事）：

```bash
# 1) 直接把 Service 端口转到 VM 上，然后用 curl
kubectl port-forward -n enterprise-g12 --address 0.0.0.0 svc/g12-backend 8080:8080
# 另开终端：
curl -s http://127.0.0.1:8080/api/actuator/health          # {"status":"UP"}
curl -s -X POST http://127.0.0.1:8080/api/auth/login \
  -H 'Content-Type: application/json' -d '{"username":"zhangsan","password":"123456"}'
# 拿 data.token 再验业务接口
curl -s http://127.0.0.1:8080/api/auth/me -H "Authorization: Bearer <token>"

# 2) 集群内临时 Pod（不占 VM 端口，适合边压测边测）
kubectl run probe-check --rm -it --image=busybox:1.36 -n enterprise-g12 -- \
  wget -qO- http://g12-backend:8080/api/actuator/health
```

**这一步能先采的证据**：`evidence/01-image/`（后端 2 张）、`evidence/03-k8s-deploy/` 的 01–05（把 `get-all` 那张换成“后端已 Running、frontend 还没有”的版本，等 A 到了再补一张完整的）、`evidence/04-logs-events/` 的 01/03/04/05。

> **不要**先 `kubectl apply -f k8s/` 整个目录：`08-ingress` 指向的 `g12-frontend` Service 还不存在，Ingress 会一直不生效、`get endpoints` 也看不到东西，容易误判成自己写错了 yaml。

### 7.2 A 的 `g12-web-v1.tar` + `07-frontend.yaml` 到了之后

```bash
docker load -i g12-web-v1.tar
minikube image load g12-web:v1
minikube image ls | grep g12-web

git pull
ls k8s/                                   # 确认 07-frontend、08-ingress 到位
kubectl apply --dry-run=client -f k8s/    # 全量 dry-run，逐个 created/configured (dry run)
kubectl apply -f k8s/
```

**验收（一条命令看全）**：

```bash
kubectl get pods,svc,endpoints,pvc,ingress,hpa -n enterprise-g12
```

| 对象 | 期望 |
|---|---|
| Pods | `g12-mysql` / `g12-backend`(×2) / `g12-frontend`(×2) 全 `Running`，READY 列全满 |
| Endpoints | `g12-mysql`、`g12-backend`、`g12-frontend` 都**非空**（空 = selector 对不上，见 `docs/incident-record.md`） |
| PVC | `g12-mysql-pvc` = `Bound` |
| Ingress | ADDRESS 列有值（minikube 里是 `192.168.49.2` 之类） |
| HPA | `g12-backend-hpa`，TARGETS 显示 `<x>%/60%`，MINPODS 2 |

后端起来后单独确认探针路径对（错了会一直 `CrashLoopBackOff`/不 Ready）。后端镜像里是 JRE，没有 curl/wget，**从集群里另起一个临时 Pod 去打**：

```bash
kubectl run probe-check --rm -it --image=busybox:1.36 -n enterprise-g12 -- \
  wget -qO- http://g12-backend:8080/api/actuator/health
# 期望 {"status":"UP"}
```

> **端口已定死为 8080（B 2026-11-13 确认）**：`k8s/06-backend.yaml` 里 Service 是 `port: 8080 → targetPort: 8080`，**不是**手册原文写的 `port: 80 → targetPort: 8080`。
> 所以集群内访问一律带 `:8080`：`http://g12-backend:8080/api/...`。
> 这条同时决定了 A 的前端模板必须 `BACKEND_HOST=g12-backend` + `BACKEND_PORT=8080`，手册第 27 条的压测 URL `http://g12-backend:8080/api/actuator/health` 也能原样用。**三处必须是同一个值，改成 80 会 502。**

`--previous` 没日志时的说明写法：容器若从未成功启动过（如 `ImagePullBackOff`），`kubectl logs --previous` 本来就无内容，**在 evidence 的 .md 里写明「容器未启动过，故无 previous 日志」并贴 describe 的 Events，不要截空图**。

---

## 8. 浏览器访问 `http://g12.course.local`

`minikube ip` 是集群内网 IP，Windows 的浏览器打不开，必须转一道。两种做法，**推荐 A 方案**。

### 方案 A：port-forward + Windows hosts（教程里用的就是这个）

1. **VM 上**（这个终端别关，关了链接就断）：
   ```bash
   kubectl port-forward -n ingress-nginx --address 0.0.0.0 svc/ingress-nginx-controller 8080:80
   ```
2. **VM 的对外 IP**：`ip -4 addr show | grep -w inet | grep 192.168`
3. **Windows**：管理员打开记事本 → `C:\Windows\System32\drivers\etc\hosts` 加一行
   ```
   192.168.x.x    g12.course.local
   ```
4. 浏览器开 **`http://g12.course.local:8080`**（端口要带，因为绑的是 8080）

> 想不带端口：VM 上用 root 绑 80 —— `sudo env KUBECONFIG=$HOME/.kube/config kubectl port-forward -n ingress-nginx --address 0.0.0.0 svc/ingress-nginx-controller 80:80`。

### 方案 B：直接用 minikube ip（仅 VM 内验证）

VM 里 `curl -H 'Host: g12.course.local' http://$(minikube ip)/` 能出 HTML，就说明 Ingress 链路是通的。

**验收**：浏览器出现登录页；用 `database/seed.sql` 里的账号（密码统一 `123456`）登录成功，能进主界面。
**截图 → `evidence/03-k8s-deploy/`（登录页 + 主界面）**。

> 答辩会问「Ingress 的地址怎么来的」：答**「minikube 的 ingress-nginx 控制器 Pod 拿到了 192.168.49.2 这个集群内 IP，宿主机通过 kubectl port-forward 把控制器的 80 映射到 VM 的 8080，Windows hosts 把域名指到 VM，所以浏览器带着 Host 头进来，由 Ingress 按 host+path 路由到 g12-frontend」**。

---

## 9. 08-ingress 的归属（先对一句再动）

- 《执行手册》第一章第 21 条把 `08-ingress.yaml` 写在 **A** 的清单里，A 的 commit 也包含它。
- B 发的《执行手册入口》写「C 底座：01–05/08」。
- **现状**：仓库里已放了一份 C 版本（`k8s/08-ingress.yaml`），内容就是 `g12.course.local` → `g12-frontend:80`。
- **规则**：谁先 push 谁留着，**另一个人不要改同一个文件**（改了会 git 冲突）。若 A 已写，C 这份直接删掉，内容本来就该一样。

---

## 10. 后续演练的入口

| 要做的事 | 看哪里 |
|---|---|
| 发布 v1→v2、异常发布 v999、rollout undo、手动 scale | `docs/operation-record.md` |
| F2 故障（改坏 Service selector → 502） | `docs/incident-record.md`（INC-G12-001） |
| 截图归档规则 | `evidence/README.md` |

---

## 11. 从零重来 / 清理

```bash
kubectl delete -f k8s/                        # 删业务层 + 底座
kubectl delete ns enterprise-g12              # 命名空间一起删（PVC/PV 里的数据在 hostPath，还在）
minikube ssh -- sudo rm -rf /data/g12-mysql   # 真要清库才执行
minikube delete                               # 连集群一起删（appendon 要重新 enable）
```

> `minikube delete` 会把节点容器连同 hostPath 数据一起删。演示前**不要**删集群。

---

## 12. 故障速查（现象 → 先查 → 多半是）

| 现象 | 先查 | 多半是 |
|---|---|---|
| Pod 一直 `ContainerCreating` | `kubectl describe pod -n enterprise-g12 <pod>` | `g12-mysql-init` / `g12-secret` 不存在；PVC 没 Bound |
| PVC 一直 `Pending` | `kubectl describe pvc g12-mysql-pvc -n enterprise-g12` | PV 的 class/capacity 与 PVC 不一致；`/data/g12-mysql` 没建 |
| MySQL `Permission denied` | `kubectl logs deploy/g12-mysql -n enterprise-g12` | `/data/g12-mysql` 不是 777 |
| 表是 0 张 | `kubectl logs deploy/g12-mysql -n enterprise-g12 \| head -60` | initdb 没执行（datadir 不空） |
| 后端不 Ready / 探针 404 | `kubectl describe pod g12-backend-xxx -n enterprise-g12` | 探针少了 `/api` 前缀 |
| 页面 502 | `kubectl get endpoints g12-backend -n enterprise-g12` | Service selector 与 Pod 标签不一致 |
| 页面 404 / 白屏 | `kubectl logs deploy/g12-frontend -n enterprise-g12` | nginx 模板没渲染，`BACKEND_HOST` 错 |
| `kubectl top` 无数 | `kubectl get pods -n kube-system \| grep metrics` | metrics-server 没 ready，等 1–2 分钟 |
| HPA `<unknown>/60%` | `kubectl describe hpa g12-backend-hpa -n enterprise-g12` | backend 没写 resources.requests.cpu（HPA 算不了百分比） |
