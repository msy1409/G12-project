# Service 与 Endpoints 详情

| 项 | 内容 |
|---|---|
| 图片 | `03-describe-svc.png` |
| 所属目录 | `evidence/07-incident/` |
| 采集人 | B（k3s 兜底环境代采） |
| 采集时间 | 2026-11-28 17:45（Asia/Shanghai） |
| 环境 | k3s v1.28.2+k3s1 单节点 / namespace `enterprise-g12` |
| 关联文档 | `docs/incident-record.md §3` |

## 对应命令

```bash
kubectl describe svc g12-backend -n enterprise-g12
kubectl describe endpoints g12-backend -n enterprise-g12
```

## 图上应当看得出

`describe svc` 里 `Selector: app=wrong-label`、`Endpoints: <none>`；`describe endpoints` 里 `Subsets:` 下无任何 Addresses/Ports 子集。对应 §3 的排除法。

## 实际输出（截取）

### describe svc（raw：`_raw/07-incident/03-describe-svc.txt`）

```text
Name:              g12-backend
Namespace:         enterprise-g12
Labels:            app=g12-backend
Annotations:       <none>
Selector:          app=wrong-label
Type:              ClusterIP
IP Family Policy:  SingleStack
IP Families:       IPv4
IP:                10.43.180.101
IPs:               10.43.180.101
Port:              http  8080/TCP
TargetPort:        8080/TCP
Endpoints:         <none>
Session Affinity:  None
Events:            <none>
```

### describe endpoints（raw：`_raw/07-incident/03-describe-endpoints.txt`）

```text
Name:         g12-backend
Namespace:    enterprise-g12
Labels:       app=g12-backend
Annotations:  <none>
Subsets:
Events:  <none>
```

## 观察结论

Service 端口（8080→8080）和 ClusterIP 都正常，问题不在端口定义；`Selector` 与 Pod 标签不匹配导致 Endpoints 的 Subsets 为空，没有任何后端地址被登记。

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
