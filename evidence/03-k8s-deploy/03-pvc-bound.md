# PVC 绑定成功

| 项 | 内容 |
|---|---|
| 图片 | `03-pvc-bound.png` |
| 所属目录 | `evidence/03-k8s-deploy/` |
| 采集人 | B（k3s 兜底环境代采） |
| 采集时间 | 2026-11-28 17:56（Asia/Shanghai） |
| 环境 | k3s v1.28.2+k3s1 单节点 / namespace `enterprise-g12` |
| 关联文档 | `docs/runbook.md §5` |

## 对应命令

```bash
kubectl get pvc -n enterprise-g12
kubectl get pv
kubectl describe pvc g12-mysql-pvc -n enterprise-g12 | tail -15
```

## 图上应当看得出

`g12-mysql-pvc` 的 STATUS 必须是 **Bound**（不能是 Pending），CAPACITY 5Gi，VOLUME 是 `g12-mysql-pv`。
Pending 时看 `describe` 的 Events：多半是 `storageClassName` 不一致，或集群里没有 `g12-local` 这个类（见 k8s/04 的注释）。

## 实际输出（截取，raw：`_raw/03-k8s-deploy/03-pvc-bound.txt`）

### PVC

```text
NAME            STATUS   VOLUME         CAPACITY   ACCESS MODES   STORAGECLASS   AGE
g12-mysql-pvc   Bound    g12-mysql-pv   5Gi        RWO            g12-local      17h
```

### PV

```text
NAME           CAPACITY   ACCESS MODES   RECLAIM POLICY   STATUS   CLAIM                          STORAGECLASS   AGE
g12-mysql-pv   5Gi        RWO            Retain           Bound    enterprise-g12/g12-mysql-pvc   g12-local      17h
```

### describe

```text
Name:          g12-mysql-pvc
Namespace:     enterprise-g12
StorageClass:  g12-local
Status:        Bound
Volume:        g12-mysql-pv
Labels:        app=g12-mysql
Annotations:   pv.kubernetes.io/bind-completed: yes
Finalizers:    [kubernetes.io/pvc-protection]
Capacity:      5Gi
Access Modes:  RWO
VolumeMode:    Filesystem
Used By:       g12-mysql-5cb9987d-t4h5v
Events:        <none>
```

## 观察结论

g12-mysql-pvc 状态 Bound、容量 5Gi，绑定到 hostPath 持久卷 g12-mysql-pv，MySQL 数据落盘持久化可用。

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
