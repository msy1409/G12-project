<template>
  <div class="page">
    <PageHeader title="审核管理" description="审核本部门访客预约：通过只做 0→1，生成核验码单独操作；驳回为 0→9。">
      <template #actions>
        <el-button :loading="loading" @click="load">刷新</el-button>
      </template>
    </PageHeader>

    <div class="card card-pad">
      <el-tabs v-model="activeTab" class="audit-tabs" @tab-change="onTabChange">
        <el-tab-pane label="待审核" name="pending" />
        <el-tab-pane label="全部预约" name="all" />
      </el-tabs>

      <div class="toolbar filters">
        <el-input
          v-model="keyword"
          placeholder="访客姓名 / 单号 / 单位"
          clearable
          style="width: 230px"
          @keyup.enter="onSearch"
          @clear="onSearch"
        >
          <template #prefix><span class="search-ico">⌕</span></template>
        </el-input>

        <template v-if="activeTab === 'all'">
          <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 150px" @change="onSearch">
            <el-option v-for="item in STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>

          <el-select
            v-if="isAdmin"
            v-model="deptFilter"
            placeholder="全部部门"
            clearable
            filterable
            style="width: 160px"
            @change="onSearch"
          >
            <el-option v-for="dept in departments" :key="dept.id" :label="dept.deptName" :value="dept.id" />
          </el-select>

          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="提交开始"
            end-placeholder="提交结束"
            value-format="YYYY-MM-DD"
            style="width: 260px"
            @change="onSearch"
          />
        </template>

        <el-button type="primary" plain @click="onSearch">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>

        <div class="spacer"></div>
        <span class="muted">共 {{ total }} 条</span>
      </div>

      <DataTable
        v-model:page="page"
        v-model:size="size"
        :columns="columns"
        :data="rows"
        :loading="loading"
        :total="total"
        :empty-text="activeTab === 'pending' ? '本部门暂无待审核预约' : '没有符合条件的预约'"
        @change="load"
      >
        <template #status="{ row }">
          <StatusTag :status="row.status" />
        </template>

        <template #verifyCode="{ row }">
          <span v-if="row.verifyCode" class="code-cell mono">{{ row.verifyCode }}</span>
          <span v-else class="muted">—</span>
        </template>

        <template #actions="{ row }">
          <el-button link type="primary" size="small" @click="goDetail(row)">详情</el-button>
          <template v-if="activeTab === 'pending'">
            <el-button link type="success" size="small" @click="approve(row)">通过</el-button>
            <el-button link type="danger" size="small" @click="reject(row)">驳回</el-button>
          </template>
        </template>
      </DataTable>
    </div>

    <ConfirmDialog ref="confirmRef" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import DataTable from '@/components/DataTable.vue'
import StatusTag from '@/components/StatusTag.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import { auditApi, deptApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ROLES, STATUS_OPTIONS } from '@/constants'

const router = useRouter()
const store = useUserStore()
const isAdmin = computed(() => store.role === ROLES.ADMIN)

const activeTab = ref('pending')
const rows = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const keyword = ref('')
const statusFilter = ref('')
const deptFilter = ref('')
const dateRange = ref([])
const departments = ref([])
const confirmRef = ref(null)

const pendingColumns = [
  { prop: 'applyNo', label: '预约单号', minWidth: 170 },
  { prop: 'visitorName', label: '访客', minWidth: 90 },
  { prop: 'visitorCompany', label: '访客单位', minWidth: 120 },
  { prop: 'deptName', label: '被访部门', minWidth: 110 },
  { prop: 'hostName', label: '被访人', width: 90 },
  { prop: 'visitReason', label: '来访事由', minWidth: 130 },
  { prop: 'visitTime', label: '预计到访', minWidth: 160 },
  { prop: 'createTime', label: '提交时间', minWidth: 160 },
  { prop: 'actions', label: '操作', width: 160, fixed: 'right', slot: 'actions', tooltip: false }
]

const allColumns = [
  { prop: 'applyNo', label: '预约单号', minWidth: 170 },
  { prop: 'visitorName', label: '访客', minWidth: 90 },
  { prop: 'visitorPhone', label: '手机号', width: 130 },
  { prop: 'deptName', label: '被访部门', minWidth: 110 },
  { prop: 'hostName', label: '被访人', width: 90 },
  { prop: 'visitTime', label: '预计到访', minWidth: 160 },
  { prop: 'status', label: '状态', width: 105, slot: 'status' },
  { prop: 'verifyCode', label: '核验码', width: 100, slot: 'verifyCode' },
  { prop: 'createTime', label: '提交时间', minWidth: 160 },
  { prop: 'actions', label: '操作', width: 90, fixed: 'right', slot: 'actions', tooltip: false }
]

const columns = computed(() => (activeTab.value === 'pending' ? pendingColumns : allColumns))

async function load() {
  loading.value = true
  try {
    const params = {
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined
    }
    let data
    if (activeTab.value === 'pending') {
      data = await auditApi.pending(params)
    } else {
      data = await auditApi.list({
        ...params,
        status: statusFilter.value,
        deptId: isAdmin.value ? deptFilter.value || undefined : undefined,
        startDate: dateRange.value?.[0] || undefined,
        endDate: dateRange.value?.[1] || undefined
      })
    }
    rows.value = data.list || []
    total.value = data.total || 0
  } catch {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function onTabChange() {
  page.value = 1
  load()
}

function onSearch() {
  page.value = 1
  load()
}

function resetFilters() {
  keyword.value = ''
  statusFilter.value = ''
  deptFilter.value = ''
  dateRange.value = []
  page.value = 1
  load()
}

function goDetail(row) {
  router.push(`/audit/${row.id}`)
}

async function approve(row) {
  const confirmed = await confirmRef.value.open({
    title: '审核通过',
    message: `确认通过预约 ${row.applyNo}（访客 ${row.visitorName}）吗？通过后状态变为「审核通过」，还需单独生成核验码。`,
    confirmText: '通过'
  })
  if (!confirmed) return
  await auditApi.approve(row.id, { remark: '同意来访' })
  ElMessage.success('已审核通过，接下来可生成核验码')
  await load()
}

async function reject(row) {
  let reason = ''
  try {
    const result = await ElMessageBox.prompt(
      `请输入驳回预约 ${row.applyNo}（访客 ${row.visitorName}）的原因，必填。`,
      '驳回预约',
      {
        confirmButtonText: '确认驳回',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPlaceholder: '如：访客信息不完整',
        inputValidator: (value) => {
          if (!value || !value.trim()) return '驳回原因不能为空'
          if (value.length > 255) return '驳回原因不能超过 255 字'
          return true
        },
        type: 'warning'
      }
    )
    reason = result.value.trim()
  } catch {
    return
  }
  await auditApi.reject(row.id, { reason })
  ElMessage.success('已驳回该预约')
  await load()
}

async function loadDepartments() {
  if (!isAdmin.value) return
  try {
    departments.value = await deptApi.list({ status: 1 })
  } catch {
    departments.value = []
  }
}

onMounted(() => {
  loadDepartments()
  load()
})
</script>

<style scoped>
.audit-tabs {
  margin-top: -6px;
}

.audit-tabs :deep(.el-tabs__header) {
  margin-bottom: 12px;
}

.filters {
  margin-bottom: 14px;
}

.search-ico {
  color: #94a3b8;
  font-size: 15px;
}

.code-cell {
  padding: 2px 8px;
  border-radius: 6px;
  background: #eff6ff;
  color: #1d4ed8;
  font-weight: 600;
  letter-spacing: 1px;
}
</style>
