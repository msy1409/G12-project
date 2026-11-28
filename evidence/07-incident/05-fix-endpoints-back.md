# 修复：selector 改回，Endpoints 恢复

| 项 | 内容 |
|---|---|
| 图片 | `05-fix-endpoints-back.png` |
| 所属目录 | `evidence/07-incident/` |
| 采集人 | B（k3s 兜底环境代采） |
| 采集时间 | 2026-11-28 17:46（Asia/Shanghai） |
| 环境 | k3s v1.28.2+k3s1 单节点 / namespace `enterprise-g12` |
| 关联文档 | `docs/incident-record.md §5` |

## 对应命令

```bash
# 最小修复：只把 selector 改回正确值，不重启、不重建、不重新发布镜像
kubectl patch svc g12-backend -n enterprise-g12 -p '{"spec":{"selector":{"app":"g12-backend"}}}'
kubectl get endpoints g12-backend -n enterprise-g12
```

## 图上应当看得出

patch 后 Endpoints 由 `<none>` 重新出现两个后端 `IP:8080`，说明 Service 与 Pod 的选中关系恢复，Endpoints 由控制器自动重算完成。

## 实际输出（截取，raw：`_raw/07-incident/05-fix-endpoints-back.txt`）

```text
NAME          ENDPOINTS                           AGE
g12-backend   10.42.0.173:8080,10.42.0.174:8080   5h16m
```

## 观察结论

一条 `patch` 把 selector 改回 `app=g12-backend`，无需重启任何 Pod，Endpoints 即重新登记两个后端地址。排障改动面保持最小：未用 `apply -f` 覆盖整份 Deployment，也未 `delete pod`（Pod 一直是对的）。

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
