# 07-incident —— F2 故障演练（INC-G12-001）

**证明**：A9（`INC-G12-001` 七段完整）。文字复盘在 `docs/incident-record.md`，本目录放“现象 → 定位 → 修复 → 恢复”的图。

| 顺序 | 文件 | 对应复盘章节 | 判据 |
|---|---|---|---|
| 1 | `01-symptom-502.png` | §1 现象 | 浏览器 Network 里 `/api` 全 502；nginx 日志 `connect() failed` |
| 2 | `02-endpoints-empty.png` | §2 状态检查 | **一张图同框**：Endpoints `<none>` + Pod Running + selector 错 |
| 3 | `03-describe-svc.png` | §3 证据 | `Selector: app=wrong-label`、`Subsets: <none>` |
| 4 | `04-pods-labels.png` | §3 排除法 | Pod 标签正确、RESTARTS=0、日志连续 |
| 5 | `05-fix-endpoints-back.png` | §5 最小修复 | patch 后 Endpoints 重新出现 IP:8080 |
| 6 | `06-recovered.png` | §6 恢复验证 | `{"status":"UP"}` + 浏览器能登录 |

## 注入与修复（各一条）

```bash
# 注入
kubectl patch svc g12-backend -n enterprise-g12 -p '{"spec":{"selector":{"app":"wrong-label"}}}'
# 修复
kubectl patch svc g12-backend -n enterprise-g12 -p '{"spec":{"selector":{"app":"g12-backend"}}}'
```

## 这个目录的价值全在“同框”

评委看的不是“你修好了”，而是**你能不能一条命令定位**。所以 2 号图必须把三件事放一个屏幕里：

```bash
kubectl get endpoints g12-backend -n enterprise-g12
kubectl get pods -n enterprise-g12 --show-labels
kubectl get svc g12-backend -n enterprise-g12 -o yaml | sed -n '/selector/,+4p'
```

## 注意

- 演练前**先登出再重进**一次页面，确认基线正常（否则恢复后分不清是修好了还是本来就没坏）。
- 注入后浏览器要**硬刷新**（Ctrl+F5），别被 302 缓存误导。
- 修复后一定要截 `Endpoints` 里那行 `10.x.x.x:8080`，只有它才是“选中关系恢复了”的硬证据。
- 整个演练 5 分钟内完成，别让页面 502 挂太久（答辩演示时其他组员可能正在用）。
