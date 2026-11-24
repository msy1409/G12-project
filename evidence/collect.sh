#!/usr/bin/env bash
# 证据文本采集器：把 kubectl 的输出按「截图文件名」落成 txt，配图时直接抄，不用手打。
#
# 第一次在 VM 上用之前：chmod +x evidence/collect.sh
#
# 用法（在仓库根目录、VM 里执行）：
#   ./evidence/collect.sh deploy      # 集群/addon、dry-run、PVC、MySQL 初始化、全栈一览
#   ./evidence/collect.sh logs        # 三个工作负载日志 + describe + events
#   ./evidence/collect.sh hpa         # HPA 初始、describe、top
#   ./evidence/collect.sh release before-v2     # 发布前快照（tag 自定义）
#   ./evidence/collect.sh release after-v2      # 发布后再跑一次，两份对比就是三态记录
#   ./evidence/collect.sh incident    # F2 故障的定位证据（注入后跑）
#   ./evidence/collect.sh all         # = deploy + logs + hpa
#
# 产物：evidence/_raw/<目录>_<截图名>.txt
#       文件头带「采集时间 + namespace + 命令 + 退出码」，可直接贴进同名 .md 的「实际输出」
set -uo pipefail

NS="${NS:-enterprise-g12}"
OUT="evidence/_raw"
mkdir -p "$OUT"

now(){ date '+%F %T'; }

# run <子目录> <截图名> <命令...>
run(){
  local dir="$1" name="$2"; shift 2
  local f="$OUT/${dir}_${name}.txt"
  local rc
  {
    echo "# 采集时间：$(now)  (Asia/Shanghai)"
    echo "# namespace：$NS"
    echo "# 命令：$*"
    echo "# ------------------------------------------------------------"
  } > "$f"
  "$@" >> "$f" 2>&1
  rc=$?
  {
    echo "# ------------------------------------------------------------"
    echo "# 退出码：$rc"
  } >> "$f"
  [ "$rc" -eq 0 ] && echo "  ok   $f" || echo "  FAIL $f (退出码 $rc)"
}

do_deploy(){
  echo "[deploy] 底座与部署证据"
  run 03-k8s-deploy 01-cluster-nodes bash -c "
    minikube version 2>/dev/null | head -2
    echo '--- nodes ---'; kubectl get nodes -o wide
    echo '--- ingress-nginx ---'; kubectl get pods -n ingress-nginx
    echo '--- metrics-server ---'; kubectl get pods -n kube-system | grep metrics
    echo '--- minikube ip ---'; minikube ip"
  run 03-k8s-deploy 02-dry-run kubectl apply --dry-run=client -f k8s/
  run 03-k8s-deploy 03-pvc-bound bash -c "
    kubectl get pvc -n $NS
    echo '--- pv ---'; kubectl get pv
    echo '--- describe ---'; kubectl describe pvc g12-mysql-pvc -n $NS | tail -15"
  run 03-k8s-deploy 04-mysql-init-tables \
    kubectl exec deploy/g12-mysql -n "$NS" -- sh -c \
    'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "use ctts_g12; show tables; select count(*) from visitor_apply;"'
  run 03-k8s-deploy 05-get-all kubectl get pods,svc,endpoints,pvc,ingress,hpa -n "$NS" -o wide
}

do_logs(){
  echo "[logs] 日志与事件"
  run 04-logs-events 01-backend-logs  kubectl logs -l app=g12-backend  -n "$NS" --timestamps --tail=50
  run 04-logs-events 02-frontend-logs kubectl logs -l app=g12-frontend -n "$NS" --timestamps --tail=50
  run 04-logs-events 03-mysql-logs    kubectl logs -l app=g12-mysql    -n "$NS" --timestamps --tail=60
  local pod
  pod=$(kubectl get pods -n "$NS" -l app=g12-backend -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)
  if [ -n "${pod:-}" ]; then
    run 04-logs-events 04-describe-pod kubectl describe pod "$pod" -n "$NS"
    run 04-logs-events 04-previous-logs kubectl logs "$pod" -n "$NS" --previous --tail=30
    echo "  (04-previous-logs 若报 'previous terminated container not found' 是正常的：容器没重启过，"
    echo "   在 md 里写明这一点，别当失败)"
  else
    echo "  !! 没找到 g12-backend 的 Pod，跳过 describe（后端还没起来？）"
  fi
  run 04-logs-events 05-events bash -c "kubectl get events -n $NS --sort-by=.lastTimestamp | tail -25"
}

do_hpa(){
  echo "[hpa] HPA 证据"
  run 06-hpa 01-hpa-initial bash -c "kubectl get hpa -n $NS; echo '--- deploy ---'; kubectl get deploy g12-backend -n $NS"
  run 06-hpa 02-describe-hpa kubectl describe hpa g12-backend-hpa -n "$NS"
  run 06-hpa 03-top-pods bash -c "kubectl top pods -n $NS; echo '--- nodes ---'; kubectl top nodes"
  echo "  (04-hpa-scale-up / 05-hpa-cooldown 要另开终端压测、看 'kubectl get hpa -w'，截图手动存；"
  echo "   压测开 4–6 个并发 Pod，60% × 200m = 120m/副本才触发，见 06-hpa/README.md)"
}

do_release(){
  local tag="${1:-snapshot}"
  echo "[release] 快照 tag=$tag（每次操作前后各跑一次，两份对比就是三态）"
  run 05-release-rollback "${tag}-deploy"      kubectl get deploy g12-backend -n "$NS"
  run 05-release-rollback "${tag}-pods"        kubectl get pods -n "$NS" -l app=g12-backend -o wide --show-labels
  run 05-release-rollback "${tag}-replicasets" kubectl get rs -n "$NS" -l app=g12-backend
  run 05-release-rollback "${tag}-history"     kubectl rollout history deployment/g12-backend -n "$NS"
  run 05-release-rollback "${tag}-image" kubectl get pods -n "$NS" -l app=g12-backend \
    -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.status.containerStatuses[0].image}{"\n"}{end}'
}

do_incident(){
  echo "[incident] F2 定位证据（先注入故障再跑）"
  run 07-incident 02-endpoints-empty bash -c "
    echo '--- endpoints ---'; kubectl get endpoints g12-backend -n $NS
    echo '--- pods(label) ---'; kubectl get pods -n $NS --show-labels
    echo '--- svc selector ---'; kubectl get svc g12-backend -n $NS -o yaml | sed -n '/selector/,+4p'"
  run 07-incident 03-describe-svc bash -c "
    kubectl describe svc g12-backend -n $NS
    echo '--- endpoints ---'; kubectl describe endpoints g12-backend -n $NS"
  run 07-incident 04-pods-labels bash -c "
    kubectl get pods -n $NS -l app=g12-backend -o wide --show-labels
    echo '--- backend logs ---'; kubectl logs -l app=g12-backend -n $NS --timestamps --tail=20"
  run 07-incident 01-symptom-502-logs \
    kubectl logs -l app=g12-frontend -n "$NS" --timestamps --tail=30
  echo "  (01-symptom-502 的浏览器截图 + 修复后的 05/06 要人工做："
  echo "   patch 回去后跑 ./evidence/collect.sh deploy 复用 05-get-all 作恢复验证)"
}

case "${1:-}" in
  deploy)   do_deploy ;;
  logs)     do_logs ;;
  hpa)      do_hpa ;;
  release)  do_release "${2:-snapshot}" ;;
  incident) do_incident ;;
  all)      do_deploy; do_logs; do_hpa ;;
  *) echo "用法: $0 {deploy|logs|hpa|release [tag]|incident|all}"; exit 1 ;;
esac

echo
echo "完成，产物在 $OUT/"
echo "把 txt 内容贴进 evidence/<目录>/<同名>.md 的「实际输出」段，再对齐同名 png。"
