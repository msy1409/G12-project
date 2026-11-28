# 集群与 addon 就绪

| 项 | 内容 |
|---|---|
| 图片 | `01-cluster-nodes.png` |
| 所属目录 | `evidence/03-k8s-deploy/` |
| 采集人 | B（k3s 兜底环境代采） |
| 采集时间 | 2026-11-28 17:55（Asia/Shanghai） |
| 环境 | k3s v1.28.2+k3s1 单节点 / namespace `enterprise-g12` |
| 关联文档 | `docs/runbook.md §2` |

## 对应命令

```bash
kubectl get nodes -o wide
kubectl get pods -n kube-system | grep -i metrics
kubectl get pods -n enterprise-g12 -o wide
```

## 图上应当看得出

节点 STATUS=Ready、版本 `v1.28.2+k3s1`；`metrics-server` 为 `1/1 Running`；业务 5 个 Pod（MySQL 1、前端 2、后端 2）全部 `1/1 Running`。

## 实际输出（截取，raw：`_raw/03-k8s-deploy/01-cluster-nodes.txt`）

### 节点

```text
NAME   STATUS   ROLES                  AGE   VERSION        INTERNAL-IP   OS-IMAGE             KERNEL-VERSION      CONTAINER-RUNTIME
s      Ready    control-plane,master   23d   v1.28.2+k3s1   10.0.2.15     Ubuntu 22.04.5 LTS   6.8.0-138-generic   containerd://1.7.6-k3s1
```

### metrics-server

```text
NAMESPACE      NAME                              READY   STATUS    RESTARTS   AGE
kube-system    metrics-server-87dcdc995-g5ht9    1/1     Running   0          91m
```

### 业务 Pod

```text
NAME                            READY   STATUS    RESTARTS       AGE     IP
g12-mysql-5cb9987d-t4h5v        1/1     Running   6 (166m ago)   5h35m   10.42.0.151
g12-frontend-74c48b9d66-vhmb4   1/1     Running   8 (166m ago)   4h44m   10.42.0.146
g12-frontend-74c48b9d66-tznxw   1/1     Running   8 (166m ago)   4h44m   10.42.0.147
g12-backend-7585dd6b86-pj429    1/1     Running   0              24m     10.42.0.173
g12-backend-7585dd6b86-9kzqb    1/1     Running   0              23m     10.42.0.174
```

## 观察结论

节点 `s` 为 Ready 状态、单节点 k3s v1.28.2+k3s1，可被调度；metrics-server 正常出数，业务 Pod 全部 Running，集群具备承载业务的条件。

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
