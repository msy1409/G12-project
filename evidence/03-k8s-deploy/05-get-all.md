# 全栈对象一览（一把抓）

| 项 | 内容 |
|---|---|
| 图片 | `05-get-all.png` |
| 所属目录 | `evidence/03-k8s-deploy/` |
| 采集人 | B（k3s 兜底环境代采） |
| 采集时间 | 2026-11-28 18:00（Asia/Shanghai） |
| 环境 | k3s v1.28.2+k3s1 单节点 / namespace `enterprise-g12` |
| 关联文档 | `docs/runbook.md §7` |

## 对应命令

```bash
kubectl get pods,svc,endpoints,ingress,hpa -n enterprise-g12 -o wide
kubectl get pods -n kube-system -l k8s-app=metrics-server
kubectl top node
kubectl top pod -n enterprise-g12
```

## 图上应当看得出

Pod 全 `Running` 且 READY 满格；三个 Service 的 **ENDPOINTS 都非空**；PVC `Bound`；Ingress 有 ADDRESS；HPA 有 TARGETS；metrics-server `1/1 Running`。
这张是 A4 评分点的证据，**必须完整不裁切**。

## 实际输出（截取，raw：`_raw/03-k8s-deploy/05-get-all.txt`）

```text
NAME                            READY   STATUS    RESTARTS       IP
pod/g12-mysql-5cb9987d-t4h5v    1/1     Running   6             10.42.0.151
pod/g12-frontend-74c48b9d66-vhmb4 1/1   Running   8             10.42.0.146
pod/g12-frontend-74c48b9d66-tznxw 1/1   Running   8             10.42.0.147
pod/g12-backend-7585dd6b86-pj429    1/1   Running   0             10.42.0.173
pod/g12-backend-7585dd6b86-9kzqb    1/1   Running   0             10.42.0.174

NAME                   TYPE        CLUSTER-IP      PORT(S)    SELECTOR
service/g12-mysql      ClusterIP   10.43.95.160    3306/TCP   app=g12-mysql
service/g12-backend    ClusterIP   10.43.180.101   8080/TCP   app=g12-backend
service/g12-frontend   ClusterIP   10.43.6.182     80/TCP     app=g12-frontend

NAME                     ENDPOINTS
endpoints/g12-mysql      10.42.0.151:3306
endpoints/g12-frontend   10.42.0.146:80,10.42.0.147:80
endpoints/g12-backend    10.42.0.173:8080,10.42.0.174:8080

NAME                                    CLASS   HOSTS              ADDRESS
ingress.networking.k8s.io/g12-ingress   nginx   g12.course.local   10.0.2.15

NAME                                                  REFERENCE                TARGETS   MINPODS   MAXPODS   REPLICAS
horizontalpodautoscaler.autoscaling/g12-backend-hpa   Deployment/g12-backend   4%/60%    2         4         2
--- metrics-server ---
metrics-server-87dcdc995-g5ht9   1/1     Running   0
--- top node ---
s      529m   17%   5297Mi   66%
--- top pod ---
g12-backend-7585dd6b86-9kzqb    8m   195Mi
g12-backend-7585dd6b86-pj429    7m   186Mi
g12-frontend-74c48b9d66-tznxw   1m   3Mi
g12-frontend-74c48b9d66-vhmb4   1m   3Mi
g12-mysql-5cb9987d-t4h5v       40m  465Mi
```

## 观察结论

mysql / backend×2 / frontend×2 五类 Pod 全部 Running，三个 Service 的 Endpoints 均非空；Ingress `g12.course.local` 已回填 ADDRESS 10.0.2.15；HPA 出数 4%/60%、副本 2；metrics-server 就绪并提供节点/Pod 指标，命名空间内业务栈与集群 addon 完整可用。

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- PVC Bound 见 `03-pvc-bound.md`；MySQL 6 表初始化见 `04-mysql-init-tables.md`。
