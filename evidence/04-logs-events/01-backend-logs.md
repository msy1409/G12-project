# 后端启动与访问日志

| 项 | 内容 |
|---|---|
| 图片 | `01-backend-logs.png` |
| 所属目录 | `evidence/04-logs-events/` |
| 采集人 | C |
| 采集时间 | 【填：YYYY-MM-DD HH:MM（Asia/Shanghai）】 |
| 环境 | minikube / namespace `enterprise-g12` |
| 关联文档 | `docs/runbook.md §7` |

## 对应命令

```bash
kubectl logs -l app=g12-backend -n enterprise-g12 --timestamps --tail=50
```

## 图上应当看得出

能看到 Spring Boot 启动完成（`Started ...Application`）、Tomcat 8080、以及业务请求日志（登录/列表）。时间戳连续、无异常堆栈。
若容器从未成功启动（ImagePullBackOff），`--previous` 无内容是正常现象，**在结论里写明原因**，不要交空图。

## 实际输出（截取）

【贴：与图一致的文本输出；长输出只留首行 + 结论行，中间用 ... 省略】

## 观察结论

【填：一句话结论，写“什么现象 + 说明什么”，不要写“运行成功”这种空话】

## 备注

- 密码、token、kubeconfig 内容一律打码后再截图。
- 若这一步失败过：把失败时的报错也留一张（命名加 `-fail` 后缀），复盘的对比价值更高。
