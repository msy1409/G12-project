<template>
  <div class="page">
    <PageHeader title="门岗核验" description="凭 6 位核验码登记入场（2→3），按预约单登记离场（3→4）；审核员只能核验本部门访客。">
      <template #actions>
        <el-button :loading="recordsLoading" @click="loadRecords">刷新记录</el-button>
      </template>
    </PageHeader>

    <div class="gate-grid">
      <div class="card card-pad gate-card">
        <div class="gate-head">
          <div>
            <h3 class="section-title">入场登记</h3>
            <p class="muted">输入访客出示的 6 位核验码，系统校验状态与部门后写入到访记录。</p>
          </div>
          <span class="gate-badge in">CHECK-IN</span>
        </div>

        <div class="code-input-wrap">
          <el-input
            v-model="checkinForm.verifyCode"
            maxlength="6"
            placeholder="000000"
            class="code-input"
            @input="onlyDigits"
            @keyup.enter="doCheckin"
          />
        </div>

        <div class="field-row">
          <el-select v-model="checkinForm.gate" filterable allow-create default-first-option placeholder="选择门岗" style="width: 100%">
            <el-option v-for="gate in GATE_OPTIONS" :key="gate" :label="gate" :value="gate" />
          </el-select>
          <el-button type="primary" :loading="checkinLoading" @click="doCheckin">核验入场</el-button>
        </div>

        <div v-if="checkinResult" class="result-strip success">
          <div class="result-main">
            <strong>{{ checkinResult.visitorName }}</strong>
            <span>入场成功 · {{ checkinResult.checkinTime }}</span>
          </div>
          <StatusTag :status="checkinResult.status" />
        </div>

        <div v-if="waitingList.length" class="quick-list">
          <div class="quick-title">当前待到访（点一下填入核验码）</div>
          <div class="chips">
            <button
              v-for="item in waitingList"
              :key="item.id"
              type="button"
              class="chip"
              @click="checkinForm.verifyCode = item.verifyCode"
            >
              <span>{{ item.visitorName }}</span>
              <strong class="mono">{{ item.verifyCode }}</strong>
            </button>
          </div>
        </div>
      </div>

      <div class="card card-pad gate-card">
        <div class="gate-head">
          <div>
            <h3 class="section-title">离场登记</h3>
            <p class="muted">只有状态 3 且存在未离场记录的预约才能登记离场，系统自动计算停留时长。</p>
          </div>
          <span class="gate-badge out">CHECK-OUT</span>
        </div>

        <div class="field-col">
          <el-input
            v-model="checkoutForm.applyId"
            placeholder="预约单 ID，例如 101"
            class="apply-id-input"
            @input="onlyNumber"
            @keyup.enter="doCheckout"
          >
            <template #prefix><span class="prefix-text">预约 ID</span></template>
          </el-input>

          <el-select v-model="checkoutForm.gate" filterable allow-create default-first-option placeholder="选择门岗" style="width: 100%">
            <el-option v-for="gate in GATE_OPTIONS" :key="gate" :label="gate" :value="gate" />
          </el-select>

          <el-button type="primary" :loading="checkoutLoading" @click="doCheckout">登记离场</el-button>
        </div>

        <div v-if="checkoutResult" class="result-strip success">
          <div class="result-main">
            <strong>离场成功</strong>
            <span>{{ checkoutResult.checkoutTime }} · 停留 {{ formatMinutes(checkoutResult.stayMinutes) }}</span>
          </div>
          <StatusTag :status="checkoutResult.status" />
        </div>

        <div v-if="inVenueList.length" class="quick-list">
          <div class="quick-title">当前在场（点一下填入预约 ID）</div>
          <div class="chips">
            <button
              v-for="item in inVenueList"
              :key="item.id"
              type="button"
              class="chip"
              @click="checkoutForm.applyId = String(item.id)"
            >
              <span>{{ item.visitorName }}</span>
              <strong class="mono">#{{ item.id }}</strong>
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="card card-pad">
      <div class="toolbar filters">
        <el-date-picker
          v-model="dateFilter"
          type="date"
          placeholder="按入场日期筛选"
          value-format="YYYY-MM-DD"
          clearable
          style="width: 180px"
          @change="onFilterChange"
        />
        <el-input
          v-model="keyword"
          placeholder="访客姓名 / 单号 / 门岗"
          clearable
          style="width: 220px"
          @keyup.enter="onFilterChange"
          @clear="onFilterChange"
        />
        <el-select
          v-if="isAdmin"
          v-model="deptFilter"
          placeholder="全部部门"
          clearable
          filterable
          style="width: 160px"
          @change="onFilterChange"
        >
          <el-option v-for="dept in departments" :key="dept.id" :label="dept.deptName" :value="dept.id" />
        </el-select>
        <el-button type="primary" plain @click="onFilterChange">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
        <div class="spacer"></div>
        <span class="muted">共 {{ total }} 条到访记录</span>
      </div>

      <DataTable
        v-model:page="page"
        v-model:size="size"
        :columns="columns"
        :data="rows"
        :loading="recordsLoading"
        :total="total"
        empty-text="没有符合条件的到访记录"
        @change="loadRecords"
      >
        <template #status="{ row }">
          <StatusTag :status="row.status" />
        </template>
        <template #stayMinutes="{ row }">
          <span v-if="row.stayMinutes !== null && row.stayMinutes !== undefined">{{ formatMinutes(row.stayMinutes) }}</span>
          <el-tag v-else size="small" type="warning" effect="plain">在场</el-tag>
        </template>
        <template #checkoutTime="{ row }">
          <span v-if="row.checkoutTime">{{ row.checkoutTime }}</span>
          <span v-else class="muted">未离场</span>
        </template>
      </DataTable>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import DataTable from '@/components/DataTable.vue'
import StatusTag from '@/components/StatusTag.vue'
import { auditApi, deptApi, recordApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { GATE_OPTIONS, ROLES } from '@/constants'
import { formatMinutes } from '@/utils/date'

const route = useRoute()
const store = useUserStore()
const isAdmin = computed(() => store.role === ROLES.ADMIN)

const checkinForm = reactive({ verifyCode: '', gate: GATE_OPTIONS[0] })
const checkoutForm = reactive({ applyId: '', gate: GATE_OPTIONS[0] })
const checkinLoading = ref(false)
const checkoutLoading = ref(false)
const checkinResult = ref(null)
const checkoutResult = ref(null)
const waitingList = ref([])
const inVenueList = ref([])

const rows = ref([])
const recordsLoading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const dateFilter = ref('')
const keyword = ref('')
const deptFilter = ref('')
const departments = ref([])

const columns = [
  { prop: 'applyNo', label: '预约单号', minWidth: 170 },
  { prop: 'visitorName', label: '访客', minWidth: 90 },
  { prop: 'deptName', label: '被访部门', minWidth: 110 },
  { prop: 'gate', label: '门岗', width: 100 },
  { prop: 'checkinTime', label: '入场时间', minWidth: 160 },
  { prop: 'checkoutTime', label: '离场时间', minWidth: 160, slot: 'checkoutTime' },
  { prop: 'stayMinutes', label: '停留时长', width: 130, slot: 'stayMinutes' },
  { prop: 'status', label: '状态', width: 105, slot: 'status' }
]

function onlyDigits(value) {
  checkinForm.verifyCode = String(value || '').replace(/\D/g, '').slice(0, 6)
}

function onlyNumber(value) {
  checkoutForm.applyId = String(value || '').replace(/\D/g, '')
}

async function doCheckin() {
  if (checkinLoading.value) return
  const code = checkinForm.verifyCode.trim()
  if (!/^\d{6}$/.test(code)) {
    ElMessage.warning('请输入 6 位数字核验码')
    return
  }
  checkinLoading.value = true
  try {
    const data = await recordApi.checkin({ verifyCode: code, gate: checkinForm.gate })
    checkinResult.value = data
    checkinForm.verifyCode = ''
    ElMessage.success(`${data.visitorName} 入场登记成功`)
    await Promise.all([loadRecords(), loadQuickLists()])
  } finally {
    checkinLoading.value = false
  }
}

async function doCheckout() {
  if (checkoutLoading.value) return
  const applyId = Number(checkoutForm.applyId)
  if (!applyId) {
    ElMessage.warning('请输入预约单 ID')
    return
  }
  checkoutLoading.value = true
  try {
    const data = await recordApi.checkout({ applyId, gate: checkoutForm.gate })
    checkoutResult.value = data
    checkoutForm.applyId = ''
    ElMessage.success(`离场登记成功，停留 ${formatMinutes(data.stayMinutes)}`)
    await Promise.all([loadRecords(), loadQuickLists()])
  } finally {
    checkoutLoading.value = false
  }
}

async function loadRecords() {
  recordsLoading.value = true
  try {
    const data = await recordApi.list({
      page: page.value,
      size: size.value,
      date: dateFilter.value || undefined,
      keyword: keyword.value || undefined,
      deptId: isAdmin.value ? deptFilter.value || undefined : undefined
    })
    rows.value = data.list || []
    total.value = data.total || 0
  } catch {
    rows.value = []
    total.value = 0
  } finally {
    recordsLoading.value = false
  }
}

async function loadQuickLists() {
  try {
    const [waiting, inVenue] = await Promise.all([
      auditApi.list({ status: 2, page: 1, size: 6 }),
      auditApi.list({ status: 3, page: 1, size: 6 })
    ])
    waitingList.value = (waiting.list || []).filter((x) => x.verifyCode)
    inVenueList.value = inVenue.list || []
  } catch {
    waitingList.value = []
    inVenueList.value = []
  }
}

async function loadDepartments() {
  if (!isAdmin.value) return
  try {
    departments.value = await deptApi.list({ status: 1 })
  } catch {
    departments.value = []
  }
}

function onFilterChange() {
  page.value = 1
  loadRecords()
}

function resetFilters() {
  dateFilter.value = ''
  keyword.value = ''
  deptFilter.value = ''
  page.value = 1
  loadRecords()
}

onMounted(() => {
  if (typeof route.query.code === 'string') checkinForm.verifyCode = route.query.code.slice(0, 6)
  if (typeof route.query.applyId === 'string') checkoutForm.applyId = route.query.applyId
  loadDepartments()
  loadRecords()
  loadQuickLists()
})
</script>

<style scoped>
.gate-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.gate-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.gate-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.gate-head .muted {
  margin: 0;
  line-height: 1.7;
}

.gate-badge {
  flex: none;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 1px;
}

.gate-badge.in {
  background: #ecfdf5;
  color: #059669;
}

.gate-badge.out {
  background: #eff6ff;
  color: #2563eb;
}

.code-input :deep(.el-input__wrapper) {
  padding: 8px 16px;
}

.code-input :deep(.el-input__inner) {
  height: 54px;
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 32px;
  font-weight: 700;
  letter-spacing: 14px;
  text-align: center;
  color: #1d4ed8;
}

.field-row {
  display: flex;
  gap: 10px;
}

.field-col {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.prefix-text {
  color: #94a3b8;
  font-size: 12px;
}

.result-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 10px;
  border: 1px solid #d1fae5;
  background: #f0fdf4;
}

.result-main {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.result-main strong {
  font-size: 13px;
  color: #065f46;
}

.result-main span {
  color: #047857;
  font-size: 12px;
}

.quick-list {
  padding-top: 4px;
  border-top: 1px dashed #eef2f7;
}

.quick-title {
  margin-bottom: 8px;
  color: #94a3b8;
  font-size: 11.5px;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  font-size: 12px;
  color: #475569;
  transition: all 0.15s ease;
}

.chip:hover {
  border-color: #93c5fd;
  background: #eff6ff;
}

.chip strong {
  color: #1d4ed8;
  letter-spacing: 1px;
}

.filters {
  margin-bottom: 14px;
}

@media (max-width: 960px) {
  .gate-grid {
    grid-template-columns: 1fr;
  }
}
</style>
