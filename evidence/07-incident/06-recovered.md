# 恢复验证（全栈健康 + 接口 200）

| 项 | 内容 |
|---|---|
| 图片 | `06-recovered.png` |
| 所属目录 | `evidence/07-incident/` |
| 采集人 | B（k3s 兜底环境代采） |
| 采集时间 | 2026-11-28 17:46（Asia/Shanghai） |
| 环境 | k3s v1.28.2+k3s1 单节点 / namespace `enterprise-g12` |
| 关联文档 | `docs/incident-record.md §6` |

## 对应命令

```bash
kubectl get endpoints g12-backend -n enterprise-g12
kubectl describe svc g12-backend -n enterprise-g12
kubectl get pods,svc,endpoints,ingress,hpa -n enterprise-g12 -o wide
# 经 Ingress（带 Host 头，等价浏览器访问）
curl -o /dev/null -w 'health HTTP %{http_code}\n' -H 'Host: g12.course.local' \
  http://127.0.0.1/api/actuator/health
curl -o /dev/null -w 'login HTTP %{http_code}\n' -X POST http://127.0.0.1/api/auth/login \
  -H 'Host: g12.course.local' -H 'Content-Type: application/json' -d @/tmp/b.json
```

## 图上应当看得出

Endpoints 回补两个后端；`describe svc` 的 `Selector: app=g12-backend`、`Endpoints` 有值；全栈 5 个 Pod `1/1 Running`、三条 Endpoints 齐全、Ingress 路由 `g12.course.local` 在、HPA 2 副本；health 与 login 均 **HTTP 200**。

## 实际输出（截取）

### describe svc 恢复后（raw：`_raw/07-incident/06-recovered-describe-svc.txt`）

```text
Name:              g12-backend
Namespace:         enterprise-g12
Selector:          app=g12-backend
IP:                10.43.180.101
Port:              http  8080/TCP
TargetPort:        8080/TCP
Endpoints:         10.42.0.173:8080,10.42.0.174:8080
```

### 全栈快照（raw：`_raw/07-incident/06-recovered-get-all.txt`）

```text
NAME                            READY   STATUS    RESTARTS       AGE     IP
pod/g12-mysql-5cb9987d-t4h5v    1/1     Running   6 (155m ago)  5h25m   10.42.0.151
pod/g12-frontend-74c48b9d66-vhmb4 1/1   Running   8 (155m ago)  4h34m   10.42.0.146
pod/g12-frontend-74c48b9d66-tznxw 1/1   Running   8 (155m ago)  4h34m   10.42.0.147
pod/g12-backend-7585dd6b86-pj429  1/1     Running   0             14m     10.42.0.173
pod/g12-backend-7585dd6b86-9kzqb  1/1     Running   0             13m     10.42.0.174

NAME                     ENDPOINTS                           AGE
endpoints/g12-mysql      10.42.0.151:3306                    17h
endpoints/g12-frontend   10.42.0.146:80,10.42.0.147:80       4h34m
endpoints/g12-backend    10.42.0.173:8080,10.42.0.174:8080   5h16m

NAME                                    CLASS   HOSTS              ADDRESS   PORTS
ingress.networking.k8s.io/g12-ingress   nginx   g12.course.local   10.0.2.15  80

NAME                                                  REFERENCE                TARGETS   MINPODS   MAXPODS   REPLICAS
horizontalpodautoscaler.autoscaling/g12-backend-hpa   Deployment/g12-backend   4%/60%    2         4         2
```

### 接口恢复验证

```text
health HTTP 200
login  HTTP 200
```

## 观察结论

修复后四层链路全部通过：Endpoints 回补、Service 选中关系恢复、经前端反代到后端、经 Ingress 带 Host 头访问 health/login 均 200，全栈 Pod 与 HPA 状态正常。数据（MySQL、PVC）未受影响，恢复后无需重新登录。

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图；接口响应里的 JWT 已脱敏。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
