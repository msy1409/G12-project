<template>
  <div class="page">
    <PageHeader title="数据统计" description="总览卡片、近 N 天趋势与按部门分布。审核员看到本部门口径，管理员看到全局口径。">
      <template #actions>
        <el-select v-model="days" style="width: 130px" @change="loadTrend">
          <el-option :value="7" label="近 7 天" />
          <el-option :value="14" label="近 14 天" />
          <el-option :value="30" label="近 30 天" />
        </el-select>
        <el-button :loading="loading" @click="loadAll">刷新</el-button>
      </template>
    </PageHeader>

    <div class="stat-grid" v-loading="loading">
      <div
        v-for="card in cards"
        :key="card.label"
        class="stat-card"
        :style="{ '--stat-accent': card.accent }"
      >
        <div class="stat-label">{{ card.label }}</div>
        <div class="stat-value">
          {{ card.value }}<span v-if="card.unit" class="unit">{{ card.unit }}</span>
        </div>
        <div class="stat-foot">{{ card.foot }}</div>
      </div>
    </div>

    <div class="chart-grid">
      <div class="card card-pad">
        <div class="card-title">
          <span>预约与到访趋势</span>
          <span class="muted">近 {{ days }} 天 · {{ scopeLabel }}</span>
        </div>
        <SimpleLineChart :labels="trendLabels" :series="trendSeries" />
      </div>

      <div v-if="isAdmin" class="card card-pad">
        <div class="card-title">
          <span>部门分布</span>
          <span class="muted">按预约单数量</span>
        </div>
        <SimpleBarChart :data="deptChartData" />
      </div>

      <div v-else class="card card-pad side-info">
        <div class="card-title">
          <span>统计口径</span>
          <el-tag size="small" effect="plain" round>本部门</el-tag>
        </div>
        <p class="muted">
          审核员调用 <code>/stats/overview</code> 与 <code>/stats/trend</code> 时，后端自动附加
          <code>dept_id = {{ store.user?.deptId }}</code>（{{ store.department }}）过滤。
        </p>
        <p class="muted">按部门分布接口 <code>/stats/department</code> 仅管理员可用，所以这里不展示。</p>

        <div class="quick-actions">
          <el-button type="primary" plain @click="router.push('/audit/list')">去处理待审核</el-button>
          <el-button plain @click="router.push('/gate')">去门岗核验</el-button>
        </div>

        <div class="mini-metrics">
          <div class="mini-item">
            <span>本部门待审核</span>
            <strong>{{ overview.pendingCount ?? 0 }}</strong>
          </div>
          <div class="mini-item">
            <span>本月到访</span>
            <strong>{{ overview.monthCheckinCount ?? 0 }}</strong>
          </div>
        </div>
      </div>
    </div>

    <div v-if="isAdmin" class="card card-pad">
      <div class="card-title">
        <span>部门统计明细</span>
        <span class="muted">共 {{ departmentStats.length }} 个部门</span>
      </div>
      <el-table :data="departmentStats" size="small" border>
        <el-table-column prop="deptId" label="部门 ID" width="90" />
        <el-table-column prop="deptName" label="部门名称" min-width="160" />
        <el-table-column prop="applyCount" label="预约数" width="110" sortable />
        <el-table-column prop="checkinCount" label="到访数" width="110" sortable />
        <el-table-column label="到访转化率" min-width="180">
          <template #default="{ row }">
            <div class="rate">
              <el-progress :percentage="rateOf(row)" :stroke-width="8" :show-text="false" />
              <span class="rate-text">{{ rateOf(row) }}%</span>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import SimpleLineChart from '@/components/SimpleLineChart.vue'
import SimpleBarChart from '@/components/SimpleBarChart.vue'
import { statsApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ROLES } from '@/constants'
import { formatMinutes } from '@/utils/date'

const router = useRouter()
const store = useUserStore()

const isAdmin = computed(() => store.role === ROLES.ADMIN)
const scopeLabel = computed(() => (isAdmin.value ? '全局口径' : `${store.department}口径`))

const loading = ref(false)
const days = ref(7)
const overview = reactive({
  todayApplyCount: 0,
  pendingCount: 0,
  todayCheckinCount: 0,
  monthCheckinCount: 0,
  avgStayMinutes: 0
})
const trend = ref([])
const departmentStats = ref([])

const cards = computed(() => [
  {
    label: '今日预约',
    value: overview.todayApplyCount,
    unit: '单',
    foot: '按 create_time 落在今天统计',
    accent: '#dbeafe'
  },
  {
    label: '待审核',
    value: overview.pendingCount,
    unit: '单',
    foot: 'status = 0，审核员核心待办',
    accent: '#fef3c7'
  },
  {
    label: '今日到访',
    value: overview.todayCheckinCount,
    unit: '人',
    foot: '按 checkin_time 落在今天统计',
    accent: '#d1fae5'
  },
  {
    label: '本月到访',
    value: overview.monthCheckinCount,
    unit: '人',
    foot: '本月已登记入场的访客总数',
    accent: '#e0e7ff'
  },
  {
    label: '平均停留',
    value: formatMinutes(overview.avgStayMinutes),
    foot: '只统计已离场的到访记录',
    accent: '#fce7f3'
  }
])

const trendLabels = computed(() => trend.value.map((x) => x.date))
const trendSeries = computed(() => [
  { name: '预约数', color: '#2563eb', data: trend.value.map((x) => x.applyCount) },
  { name: '到访数', color: '#10b981', data: trend.value.map((x) => x.checkinCount) }
])

const deptChartData = computed(() =>
  departmentStats.value.map((x) => ({
    label: x.deptName,
    applyCount: x.applyCount,
    checkinCount: x.checkinCount
  }))
)

function rateOf(row) {
  if (!row.applyCount) return 0
  return Math.round((row.checkinCount / row.applyCount) * 100)
}

async function loadOverview() {
  const data = await statsApi.overview()
  Object.assign(overview, data)
}

async function loadTrend() {
  const data = await statsApi.trend(days.value)
  trend.value = data || []
}

async function loadDepartments() {
  if (!isAdmin.value) return
  departmentStats.value = (await statsApi.department()) || []
}

async function loadAll() {
  loading.value = true
  try {
    await Promise.all([loadOverview(), loadTrend(), loadDepartments()])
  } finally {
    loading.value = false
  }
}

onMounted(loadAll)
</script>

<style scoped>
.chart-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(300px, 0.9fr);
  gap: 16px;
  align-items: start;
}

.side-info p {
  margin: 0 0 12px;
  line-height: 1.8;
}

.side-info code {
  padding: 1px 5px;
  border-radius: 4px;
  background: #f1f5f9;
  color: #334155;
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 11.5px;
}

.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 16px 0 18px;
}

.mini-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.mini-item {
  padding: 12px;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px solid #eef2f7;
}

.mini-item span {
  display: block;
  color: var(--g12-text-secondary);
  font-size: 11.5px;
}

.mini-item strong {
  display: block;
  margin-top: 6px;
  font-size: 20px;
  color: #1f2937;
}

.rate {
  display: flex;
  align-items: center;
  gap: 10px;
}

.rate :deep(.el-progress) {
  flex: 1;
}

.rate-text {
  width: 40px;
  color: var(--g12-text-secondary);
  font-size: 12px;
  text-align: right;
}

@media (max-width: 1080px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }
}
</style>
