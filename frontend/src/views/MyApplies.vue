<template>
  <div class="page">
    <PageHeader title="我的预约" description="查看本人提交的访客预约，跟踪审核进度与核验码，未开始的预约可取消。">
      <template #actions>
        <PermissionButton :roles="['EMPLOYEE']" type="primary" @click="router.push('/apply/new')">
          新建预约
        </PermissionButton>
      </template>
    </PageHeader>

    <div class="card card-pad">
      <div class="toolbar">
        <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 160px" @change="onFilterChange">
          <el-option v-for="item in STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button :loading="loading" @click="load">刷新</el-button>
        <div class="spacer"></div>
        <span class="muted">共 {{ total }} 条预约</span>
      </div>

      <DataTable
        v-model:page="page"
        v-model:size="size"
        :columns="columns"
        :data="rows"
        :loading="loading"
        :total="total"
        empty-text="还没有预约，点右上角「新建预约」开始"
        @change="load"
      >
        <template #status="{ row }">
          <StatusTag :status="row.status" />
        </template>

        <template #actions="{ row }">
          <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
          <el-button
            v-if="[0, 1, 2].includes(row.status)"
            link
            type="danger"
            size="small"
            @click="cancelApply(row)"
          >
            取消
          </el-button>
        </template>
      </DataTable>
    </div>

    <el-drawer v-model="drawerVisible" title="预约详情" size="620px" destroy-on-close>
      <div v-loading="detailLoading" class="detail">
        <template v-if="detail">
          <div class="detail-head">
            <div>
              <div class="detail-name">{{ detail.visitorName }}</div>
              <div class="muted mono">{{ detail.applyNo }}</div>
            </div>
            <StatusTag :status="detail.status" />
          </div>

          <div v-if="detail.verifyCode" class="code-banner">
            <div>
              <div class="code-label">访客核验码</div>
              <div class="verify-code-box">
                <span class="verify-code">{{ detail.verifyCode }}</span>
                <el-button link type="primary" @click="copyCode(detail.verifyCode)">复制</el-button>
              </div>
            </div>
            <div class="code-hint">访客到场后向门岗出示，凭码登记入场</div>
          </div>

          <h4 class="section-title">基本信息</h4>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="访客手机">{{ detail.visitorPhone }}</el-descriptions-item>
            <el-descriptions-item label="访客单位">{{ detail.visitorCompany || '—' }}</el-descriptions-item>
            <el-descriptions-item label="被访部门">{{ detail.deptName }}</el-descriptions-item>
            <el-descriptions-item label="被访人">{{ detail.hostName }}</el-descriptions-item>
            <el-descriptions-item label="预计到访">{{ detail.visitTime }}</el-descriptions-item>
            <el-descriptions-item label="预计离开">{{ detail.leaveTime }}</el-descriptions-item>
            <el-descriptions-item label="来访事由" :span="2">{{ detail.visitReason || '—' }}</el-descriptions-item>
            <el-descriptions-item label="提交时间" :span="2">{{ detail.createTime }}</el-descriptions-item>
          </el-descriptions>

          <h4 class="section-title">处理记录</h4>
          <el-timeline class="timeline">
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

          <h4 class="section-title">状态变更审计</h4>
          <el-table :data="detail.statusLogList" size="small" border>
            <el-table-column label="变更" width="120">
              <template #default="{ row }">
                <span class="mono">{{ row.fromStatus === -1 ? '∅' : row.fromStatus }} → {{ row.toStatus }}</span>
              </template>
            </el-table-column>
            <el-table-column label="动作" width="110">
              <template #default="{ row }">{{ ACTION_LABEL[row.action] || row.action }}</template>
            </el-table-column>
            <el-table-column prop="operatorName" label="操作人" width="100" />
            <el-table-column prop="createTime" label="时间" min-width="150" />
          </el-table>
        </template>
      </div>

      <template #footer>
        <div class="drawer-footer">
          <el-button @click="drawerVisible = false">关闭</el-button>
          <el-button
            v-if="detail && [0, 1, 2].includes(detail.status)"
            type="danger"
            plain
            @click="cancelApply(detail)"
          >
            取消预约
          </el-button>
        </div>
      </template>
    </el-drawer>

    <ConfirmDialog ref="confirmRef" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import DataTable from '@/components/DataTable.vue'
import StatusTag from '@/components/StatusTag.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import PermissionButton from '@/components/PermissionButton.vue'
import { applyApi } from '@/api'
import { ACTION_LABEL, ROLE_LABEL, STATUS_OPTIONS } from '@/constants'

const router = useRouter()

const rows = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const statusFilter = ref('')

const drawerVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)
const confirmRef = ref(null)

const columns = [
  { prop: 'applyNo', label: '预约单号', minWidth: 170 },
  { prop: 'visitorName', label: '访客', minWidth: 90 },
  { prop: 'visitorPhone', label: '手机号', width: 130 },
  { prop: 'deptName', label: '被访部门', minWidth: 110 },
  { prop: 'hostName', label: '被访人', width: 90 },
  { prop: 'visitTime', label: '预计到访', minWidth: 160 },
  { prop: 'status', label: '状态', width: 110, slot: 'status' },
  { prop: 'createTime', label: '提交时间', minWidth: 160 },
  { prop: 'actions', label: '操作', width: 130, slot: 'actions', fixed: 'right', tooltip: false }
]

const ACTION_COLORS = {
  CREATE: '#94a3b8',
  APPROVE: '#2563eb',
  VERIFY_CODE: '#8b5cf6',
  CHECKIN: '#10b981',
  CHECKOUT: '#64748b',
  REJECT: '#ef4444',
  CANCEL: '#ef4444'
}

function actionColor(action) {
  return ACTION_COLORS[action] || '#94a3b8'
}

async function load() {
  loading.value = true
  try {
    const data = await applyApi.mine({ status: statusFilter.value, page: page.value, size: size.value })
    rows.value = data.list || []
    total.value = data.total || 0
  } catch {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function onFilterChange() {
  page.value = 1
  load()
}

async function openDetail(row) {
  drawerVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await applyApi.detail(row.id)
  } finally {
    detailLoading.value = false
  }
}

async function cancelApply(row) {
  const confirmed = await confirmRef.value.open({
    title: '取消预约',
    message: `确定取消预约 ${row.applyNo}（访客 ${row.visitorName}）吗？取消后状态变为「驳回/取消」，不可再入场。`,
    confirmText: '确认取消',
    danger: true
  })
  if (!confirmed) return

  await applyApi.cancel(row.id, { reason: '员工取消行程' })
  ElMessage.success('预约已取消')
  await load()
  if (drawerVisible.value && detail.value?.id === row.id) {
    detail.value = await applyApi.detail(row.id)
  }
}

async function copyCode(code) {
  try {
    await navigator.clipboard.writeText(code)
    ElMessage.success('核验码已复制')
  } catch {
    ElMessage.warning(`核验码：${code}`)
  }
}

onMounted(load)
</script>

<style scoped>
.detail {
  min-height: 200px;
}

.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.detail-name {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
}

.code-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 16px 18px;
  margin-bottom: 20px;
  border-radius: 12px;
  background: linear-gradient(135deg, #eff6ff, #f5f3ff);
  border: 1px solid #dbeafe;
}

.code-label {
  margin-bottom: 8px;
  color: #475569;
  font-size: 12px;
}

.code-hint {
  max-width: 210px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.7;
}

.timeline {
  padding-left: 4px;
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

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
