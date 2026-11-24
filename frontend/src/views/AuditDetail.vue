<template>
  <div class="page" v-loading="loading">
    <PageHeader :title="detail ? `预约 ${detail.applyNo}` : '预约详情'" description="查看访客信息、处理记录与状态审计，并完成审核、驳回或生成核验码。">
      <template #actions>
        <el-button @click="router.push('/audit/list')">返回列表</el-button>
      </template>
    </PageHeader>

    <template v-if="detail">
      <div class="card card-pad summary">
        <div class="summary-main">
          <div class="visitor">
            <div class="avatar">{{ detail.visitorName.slice(-1) }}</div>
            <div>
              <div class="name-row">
                <h2>{{ detail.visitorName }}</h2>
                <StatusTag :status="detail.status" />
              </div>
              <div class="muted">
                {{ detail.visitorCompany || '未填写单位' }} · {{ detail.visitorPhone }} · 被访人 {{ detail.hostName }}
              </div>
            </div>
          </div>

          <div class="summary-actions">
            <template v-if="detail.status === 0">
              <el-button type="success" @click="approve">审核通过</el-button>
              <el-button type="danger" plain @click="reject">驳回</el-button>
            </template>
            <el-button v-if="detail.status === 1" type="primary" @click="generateCode">生成核验码</el-button>
            <el-button v-if="detail.status === 2" @click="goGate('code')">去门岗核验</el-button>
            <el-button v-if="detail.status === 3" @click="goGate('out')">登记离场</el-button>
          </div>
        </div>

        <el-alert
          v-if="detail.status === 9"
          class="closed-alert"
          type="error"
          :closable="false"
          show-icon
          :title="`该预约已终结（驳回/取消）：${closeRemark}`"
        />

        <div class="steps">
          <el-steps :active="activeStep" finish-status="success" align-center>
            <el-step title="提交预约" description="状态 0" />
            <el-step title="审核通过" description="状态 1" />
            <el-step title="待到访" description="状态 2" />
            <el-step title="已到访" description="状态 3" />
            <el-step title="已离开" description="状态 4" />
          </el-steps>
        </div>
      </div>

      <div class="detail-grid">
        <div class="main-col">
          <div class="card card-pad">
            <h3 class="section-title">访客与来访信息</h3>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="预约单号">{{ detail.applyNo }}</el-descriptions-item>
              <el-descriptions-item label="访客姓名">{{ detail.visitorName }}</el-descriptions-item>
              <el-descriptions-item label="访客手机">{{ detail.visitorPhone }}</el-descriptions-item>
              <el-descriptions-item label="访客单位">{{ detail.visitorCompany || '—' }}</el-descriptions-item>
              <el-descriptions-item label="被访部门">{{ detail.deptName }}</el-descriptions-item>
              <el-descriptions-item label="被访人">{{ detail.hostName }}</el-descriptions-item>
              <el-descriptions-item label="预计到访">{{ detail.visitTime }}</el-descriptions-item>
              <el-descriptions-item label="预计离开">{{ detail.leaveTime }}</el-descriptions-item>
              <el-descriptions-item label="来访事由" :span="2">{{ detail.visitReason || '—' }}</el-descriptions-item>
              <el-descriptions-item label="提交时间" :span="2">{{ detail.createTime }}</el-descriptions-item>
            </el-descriptions>
          </div>

          <div class="card card-pad">
            <h3 class="section-title">处理记录</h3>
            <el-timeline>
              <el-timeline-item
                v-for="item in detail.processList"
                :key="`${item.action}-${item.createTime}`"
                :timestamp="item.createTime"
                placement="top"
                :color="actionColor(item.action)"
              >
                <div class="tl-title">
                  {{ ACTION_LABEL[item.action] || item.action }}
                  <span class="tl-operator">{{ item.operatorName }} · {{ ROLE_LABEL[item.operatorRole] || item.operatorRole }}</span>
                </div>
                <div v-if="item.remark" class="tl-remark">{{ item.remark }}</div>
              </el-timeline-item>
            </el-timeline>
          </div>

          <div class="card card-pad">
            <h3 class="section-title">状态变更审计</h3>
            <el-table :data="detail.statusLogList" size="small" border>
              <el-table-column label="状态变更" width="130">
                <template #default="{ row }">
                  <span class="mono">{{ statusText(row.fromStatus) }} → {{ statusText(row.toStatus) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="动作" width="110">
                <template #default="{ row }">{{ ACTION_LABEL[row.action] || row.action }}</template>
              </el-table-column>
              <el-table-column prop="operatorName" label="操作人" width="100" />
              <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
              <el-table-column prop="createTime" label="时间" min-width="160" />
            </el-table>
          </div>
        </div>

        <aside class="side-col">
          <div class="card card-pad code-card">
            <h3 class="section-title">核验码</h3>
            <template v-if="detail.verifyCode">
              <div class="verify-code-box">
                <span class="verify-code">{{ detail.verifyCode }}</span>
              </div>
              <p class="muted code-tip">访客到场后由门岗输入这 6 位数字完成入场；已入场的码不能重复使用。</p>
              <el-button class="copy-btn" @click="copyCode(detail.verifyCode)">复制核验码</el-button>
            </template>
            <template v-else>
              <div class="empty-code">尚未生成</div>
              <p class="muted code-tip">{{ codeHint }}</p>
            </template>
          </div>

          <div class="card card-pad">
            <h3 class="section-title">关键时间</h3>
            <div class="time-list">
              <div class="time-item">
                <span>提交</span>
                <strong>{{ detail.createTime }}</strong>
              </div>
              <div class="time-item">
                <span>预计到访</span>
                <strong>{{ detail.visitTime }}</strong>
              </div>
              <div class="time-item">
                <span>预计离开</span>
                <strong>{{ detail.leaveTime }}</strong>
              </div>
            </div>
          </div>

          <div class="card card-pad permission-note">
            <h3 class="section-title">权限说明</h3>
            <p>审核员只能操作本部门预约，跨部门请求返回 403；管理员可查看全部。前端按钮隐藏只是体验，最终以后端校验为准。</p>
          </div>
        </aside>
      </div>
    </template>

    <ConfirmDialog ref="confirmRef" />

    <el-dialog v-model="codeDialogVisible" title="核验码已生成" width="420px" align-center>
      <div class="code-dialog">
        <p class="muted">请把核验码告知访客，门岗凭码登记入场。</p>
        <div class="verify-code-box">
          <span class="verify-code">{{ latestCode }}</span>
        </div>
        <div class="code-dialog-actions">
          <el-button @click="copyCode(latestCode)">复制</el-button>
          <el-button type="primary" @click="codeDialogVisible = false">知道了</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import { applyApi, auditApi } from '@/api'
import { ACTION_LABEL, ROLE_LABEL, STATUS } from '@/constants'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const detail = ref(null)
const confirmRef = ref(null)
const codeDialogVisible = ref(false)
const latestCode = ref('')

const ACTION_COLORS = {
  CREATE: '#94a3b8',
  APPROVE: '#2563eb',
  VERIFY_CODE: '#8b5cf6',
  CHECKIN: '#10b981',
  CHECKOUT: '#64748b',
  REJECT: '#ef4444',
  CANCEL: '#ef4444'
}

const activeStep = computed(() => {
  const status = detail.value?.status
  if (status === 0) return 1
  if (status === 1) return 2
  if (status === 2) return 3
  if (status === 3) return 4
  if (status === 4) return 5
  return 0
})

const codeHint = computed(() => {
  const status = detail.value?.status
  if (status === 0) return '预约还在待审核，审核通过后才能生成核验码。'
  if (status === 1) return '审核已通过，点右上角「生成核验码」完成 1→2。'
  if (status === 9) return '预约已驳回或取消，不会生成核验码。'
  return '该预约没有可用的核验码。'
})

const closeRemark = computed(() => {
  const log = [...(detail.value?.statusLogList || [])].reverse().find((x) => x.action === 'REJECT' || x.action === 'CANCEL')
  return log?.remark || '未填写原因'
})

function actionColor(action) {
  return ACTION_COLORS[action] || '#94a3b8'
}

function statusText(status) {
  if (status === -1) return '新建'
  return STATUS[status]?.label || String(status)
}

async function load() {
  loading.value = true
  try {
    detail.value = await applyApi.detail(route.params.id)
  } catch {
    detail.value = null
  } finally {
    loading.value = false
  }
}

async function approve() {
  const confirmed = await confirmRef.value.open({
    title: '审核通过',
    message: `确认通过预约 ${detail.value.applyNo}（访客 ${detail.value.visitorName}）吗？通过后状态 0→1，还需再生成核验码。`,
    confirmText: '通过'
  })
  if (!confirmed) return
  await auditApi.approve(detail.value.id, { remark: '同意来访' })
  ElMessage.success('已审核通过，接下来请生成核验码')
  await load()
}

async function reject() {
  let reason = ''
  try {
    const result = await ElMessageBox.prompt('请输入驳回原因（必填，最多 255 字）', '驳回预约', {
      confirmButtonText: '确认驳回',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputPlaceholder: '如：来访事由不明确',
      inputValidator: (value) => {
        if (!value || !value.trim()) return '驳回原因不能为空'
        if (value.length > 255) return '驳回原因不能超过 255 字'
        return true
      },
      type: 'warning'
    })
    reason = result.value.trim()
  } catch {
    return
  }
  await auditApi.reject(detail.value.id, { reason })
  ElMessage.success('已驳回该预约')
  await load()
}

async function generateCode() {
  const confirmed = await confirmRef.value.open({
    title: '生成核验码',
    message: `将为预约 ${detail.value.applyNo} 生成 6 位数字核验码，状态 1→2。同一预约只能生成一次。`,
    confirmText: '生成'
  })
  if (!confirmed) return
  const data = await auditApi.verifyCode(detail.value.id)
  latestCode.value = data.verifyCode
  codeDialogVisible.value = true
  ElMessage.success('核验码生成成功')
  await load()
}

function goGate(mode) {
  if (mode === 'code') {
    router.push({ path: '/gate', query: { code: detail.value.verifyCode } })
  } else {
    router.push({ path: '/gate', query: { applyId: detail.value.id } })
  }
}

async function copyCode(code) {
  try {
    await navigator.clipboard.writeText(String(code))
    ElMessage.success('核验码已复制')
  } catch {
    ElMessage.warning(`核验码：${code}`)
  }
}

onMounted(load)
</script>

<style scoped>
.summary {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.summary-main {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.visitor {
  display: flex;
  align-items: center;
  gap: 14px;
}

.avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: linear-gradient(135deg, #dbeafe, #e0e7ff);
  color: #1e40af;
  font-size: 18px;
  font-weight: 700;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.name-row h2 {
  margin: 0;
  font-size: 20px;
  color: #111827;
}

.visitor .muted {
  margin-top: 5px;
}

.summary-actions {
  display: flex;
  gap: 10px;
}

.closed-alert {
  border-radius: 10px;
}

.steps {
  padding: 6px 0 2px;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(280px, 0.8fr);
  gap: 16px;
  align-items: start;
}

.main-col,
.side-col {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.tl-title {
  font-size: 13px;
  font-weight: 600;
  color: #1f2937;
}

.tl-operator {
  margin-left: 8px;
  color: #94a3b8;
  font-size: 12px;
  font-weight: 400;
}

.tl-remark {
  margin-top: 4px;
  color: var(--g12-text-secondary);
  font-size: 12px;
}

.code-card {
  text-align: center;
}

.code-card .section-title {
  justify-content: center;
}

.code-tip {
  margin: 12px 0 14px;
  line-height: 1.7;
  text-align: left;
}

.copy-btn {
  width: 100%;
}

.empty-code {
  padding: 18px 0 6px;
  color: #94a3b8;
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 2px;
}

.time-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.time-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 10px;
  border-bottom: 1px dashed #eef2f7;
  font-size: 12px;
}

.time-item:last-child {
  padding-bottom: 0;
  border-bottom: none;
}

.time-item span {
  color: var(--g12-text-secondary);
}

.time-item strong {
  color: #1f2937;
  font-weight: 600;
}

.permission-note p {
  margin: 0;
  color: var(--g12-text-secondary);
  font-size: 12px;
  line-height: 1.8;
}

.code-dialog {
  text-align: center;
}

.code-dialog .verify-code-box {
  margin: 16px 0 22px;
}

.code-dialog-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
}

@media (max-width: 1080px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
