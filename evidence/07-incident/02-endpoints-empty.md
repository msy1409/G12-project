# 定位关键：Endpoints 为空

| 项 | 内容 |
|---|---|
| 图片 | `02-endpoints-empty.png` |
| 所属目录 | `evidence/07-incident/` |
| 采集人 | B（k3s 兜底环境代采） |
| 采集时间 | 2026-11-28 17:48（Asia/Shanghai） |
| 环境 | k3s v1.28.2+k3s1 单节点 / namespace `enterprise-g12` |
| 关联文档 | `docs/incident-record.md §2` |

## 对应命令

```bash
kubectl get endpoints g12-backend -n enterprise-g12
kubectl get pods -n enterprise-g12 -o wide --show-labels
kubectl get svc g12-backend -n enterprise-g12 -o yaml | sed -n '/selector/,+4p'
```

## 图上应当看得出

**三个事实同框**：Endpoints `<none>`；Pod 全是 Running 且标签 `app=g12-backend`；Service selector 是 `app=wrong-label`。这张是整个复盘的“一图定案”，必须同屏。详见 `docs/incident-record.md` §2。

## 实际输出（截取，raw：`_raw/07-incident/02-endpoints-empty.txt`）

### endpoints

```text
NAME          ENDPOINTS   AGE
g12-backend   <none>      5h17m
```

### pods-labels

```text
NAME                            READY   STATUS    RESTARTS       AGE     IP            LABELS
g12-mysql-5cb9987d-t4h5v        1/1     Running   6 (157m ago)   5h26m   10.42.0.151  app=g12-mysql,pod-template-hash=5cb9987d,tier=db
g12-frontend-74c48b9d66-vhmb4   1/1     Running   8 (156m ago)   4h35m   10.42.0.146  app=g12-frontend,pod-template-hash=74c48b9d66
g12-frontend-74c48b9d66-tznxw   1/1     Running   8 (156m ago)   4h35m   10.42.0.147  app=g12-frontend,pod-template-hash=74c48b9d66
g12-backend-7585dd6b86-pj429    1/1     Running   0              15m     10.42.0.173  app=g12-backend,pod-template-hash=7585dd6b86,tier=backend
g12-backend-7585dd6b86-9kzqb    1/1     Running   0              14m     10.42.0.174  app=g12-backend,pod-template-hash=7585dd6b86,tier=backend
```

### svc-selector

```text
  labels:
    app: g12-backend
  name: g12-backend
  selector:
    app: wrong-label
  sessionAffinity: None
  type: ClusterIP
```

## 观察结论

Endpoints 为空，但两个后端 Pod 都 Running、标签仍是 `app=g12-backend`；唯独 Service selector 被改成 `app=wrong-label`，与 Pod 标签不匹配。K8s 的 Endpoints 由 selector 与 Pod 标签集合运算得出，selector 不匹配 → 选中集合为空 → 没有后端可转发。断点在 Service 选中关系，不在应用、镜像或探针。

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
