<template>
  <div class="page">
    <PageHeader title="新建访客预约" description="填写访客与被访信息，提交后进入待审核状态，由本部门审核员审批。">
      <template #actions>
        <el-button @click="router.push('/apply/mine')">我的预约</el-button>
      </template>
    </PageHeader>

    <div class="apply-grid">
      <div class="card card-pad">
        <template v-if="!created">
          <h3 class="section-title">访客信息</h3>
          <el-form ref="formRef" :model="form" :rules="rules" label-width="104px" size="large">
            <div class="form-grid">
              <el-form-item label="访客姓名" prop="visitorName">
                <el-input v-model="form.visitorName" placeholder="请输入访客真实姓名" maxlength="30" />
              </el-form-item>
              <el-form-item label="访客手机" prop="visitorPhone">
                <el-input v-model="form.visitorPhone" placeholder="11 位手机号" maxlength="11" />
              </el-form-item>
              <el-form-item label="访客单位" prop="visitorCompany">
                <el-input v-model="form.visitorCompany" placeholder="访客所属公司 / 组织" maxlength="60" />
              </el-form-item>
              <el-form-item label="被访部门" prop="deptId">
                <el-select v-model="form.deptId" placeholder="选择被访部门" filterable style="width: 100%">
                  <el-option v-for="dept in departments" :key="dept.id" :label="dept.deptName" :value="dept.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="被访人" prop="hostName">
                <el-input v-model="form.hostName" placeholder="接待人姓名" maxlength="30" />
              </el-form-item>
              <el-form-item label="来访事由" prop="visitReason">
                <el-input v-model="form.visitReason" placeholder="如：项目对接、设备维修" maxlength="100" />
              </el-form-item>
              <el-form-item label="预计到访" prop="visitTime">
                <el-date-picker
                  v-model="form.visitTime"
                  type="datetime"
                  placeholder="选择到访时间"
                  format="YYYY-MM-DD HH:mm"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  :disabled-date="disabledPastDate"
                  style="width: 100%"
                />
              </el-form-item>
              <el-form-item label="预计离开" prop="leaveTime">
                <el-date-picker
                  v-model="form.leaveTime"
                  type="datetime"
                  placeholder="选择离开时间"
                  format="YYYY-MM-DD HH:mm"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  :disabled-date="disabledPastDate"
                  style="width: 100%"
                />
              </el-form-item>
            </div>

            <div class="form-actions">
              <el-button size="large" @click="resetForm">重置</el-button>
              <el-button size="large" type="primary" :loading="submitting" @click="submit">
                提交预约
              </el-button>
            </div>
          </el-form>
        </template>

        <template v-else>
          <div class="success-box">
            <div class="success-icon">✓</div>
            <h3>预约提交成功</h3>
            <p class="muted">预约单已进入待审核队列，审核员通过后会生成 6 位核验码。</p>

            <div class="success-meta">
              <div class="meta-item">
                <span>预约单号</span>
                <strong class="mono">{{ created.applyNo }}</strong>
              </div>
              <div class="meta-item">
                <span>当前状态</span>
                <StatusTag :status="created.status" />
              </div>
            </div>

            <div class="success-actions">
              <el-button @click="createAnother">继续新建</el-button>
              <el-button type="primary" @click="router.push('/apply/mine')">查看我的预约</el-button>
            </div>
          </div>
        </template>
      </div>

      <aside class="side">
        <div class="card card-pad">
          <h3 class="section-title">预约流程</h3>
          <ol class="flow">
            <li v-for="(step, index) in flow" :key="step.title" class="flow-item">
              <span class="num" :class="{ done: index === 0 }">{{ index + 1 }}</span>
              <div>
                <strong>{{ step.title }}</strong>
                <p>{{ step.desc }}</p>
              </div>
            </li>
          </ol>
        </div>

        <div class="card card-pad tips">
          <h3 class="section-title">填写提示</h3>
          <ul>
            <li>预计到访时间必须晚于当前时间，离开时间必须晚于到访时间。</li>
            <li>被访部门默认取你的部门，可改为跨部门拜访。</li>
            <li>提交成功后，在「我的预约」里可查看状态、核验码并取消未开始的预约。</li>
            <li>审批通过不会自动生成核验码，需要审核员再点一次「生成核验码」。</li>
          </ul>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { applyApi, deptApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { disabledPastDate, parseDate } from '@/utils/date'

const router = useRouter()
const store = useUserStore()

const formRef = ref(null)
const submitting = ref(false)
const created = ref(null)
const departments = ref([])

const form = reactive({
  visitorName: '',
  visitorPhone: '',
  visitorCompany: '',
  deptId: store.user?.deptId || null,
  hostName: store.user?.realName || '',
  visitReason: '',
  visitTime: '',
  leaveTime: ''
})

const flow = [
  { title: '提交预约 · 状态 0', desc: '员工替访客填写信息，写入预约单与 CREATE 处理记录' },
  { title: '审核通过 · 状态 1', desc: '本部门审核员确认来访事由，可填写备注' },
  { title: '生成核验码 · 状态 2', desc: '审核员生成 6 位数字码，访客凭码到场' },
  { title: '门岗入场 / 离场 · 状态 3 → 4', desc: '核验码入场，离场时登记 checkout_time' }
]

const rules = {
  visitorName: [{ required: true, message: '请输入访客姓名', trigger: 'blur' }],
  visitorPhone: [
    { required: true, message: '请输入访客手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  visitorCompany: [{ required: true, message: '请输入访客单位', trigger: 'blur' }],
  deptId: [{ required: true, message: '请选择被访部门', trigger: 'change' }],
  hostName: [{ required: true, message: '请输入被访人', trigger: 'blur' }],
  visitReason: [{ required: true, message: '请填写来访事由', trigger: 'blur' }],
  visitTime: [
    { required: true, message: '请选择预计到访时间', trigger: 'change' },
    {
      validator: (rule, value, callback) => {
        const date = parseDate(value)
        if (date && date.getTime() < Date.now()) callback(new Error('到访时间不能早于当前时间'))
        else callback()
      },
      trigger: 'change'
    }
  ],
  leaveTime: [
    { required: true, message: '请选择预计离开时间', trigger: 'change' },
    {
      validator: (rule, value, callback) => {
        const start = parseDate(form.visitTime)
        const end = parseDate(value)
        if (start && end && end.getTime() <= start.getTime()) callback(new Error('离开时间必须晚于到访时间'))
        else callback()
      },
      trigger: 'change'
    }
  ]
}

async function loadDepartments() {
  try {
    const data = await deptApi.list({ status: 1 })
    departments.value = data || []
    if (!form.deptId && departments.value.length) form.deptId = departments.value[0].id
  } catch {
    departments.value = []
  }
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const data = await applyApi.create({ ...form })
    created.value = data
    ElMessage.success('预约提交成功，等待审核')
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  formRef.value?.resetFields()
  form.deptId = store.user?.deptId || null
  form.hostName = store.user?.realName || ''
}

function createAnother() {
  created.value = null
  resetForm()
}

onMounted(loadDepartments)
</script>

<style scoped>
.apply-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.65fr) minmax(280px, 0.85fr);
  gap: 16px;
  align-items: start;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px dashed #e2e8f0;
}

.side {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.flow {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.flow-item {
  display: flex;
  gap: 12px;
}

.num {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #f1f5f9;
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
}

.num.done {
  background: #dbeafe;
  color: #1d4ed8;
}

.flow-item strong {
  display: block;
  font-size: 13px;
  color: #1f2937;
}

.flow-item p {
  margin: 4px 0 0;
  color: var(--g12-text-secondary);
  font-size: 12px;
  line-height: 1.6;
}

.tips ul {
  margin: 0;
  padding-left: 18px;
  color: var(--g12-text-secondary);
  font-size: 12.5px;
  line-height: 1.9;
}

.success-box {
  padding: 26px 8px 12px;
  text-align: center;
}

.success-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  margin: 0 auto 14px;
  border-radius: 50%;
  background: #ecfdf5;
  color: #059669;
  font-size: 26px;
  font-weight: 700;
}

.success-box h3 {
  margin: 0 0 8px;
  font-size: 19px;
  color: #111827;
}

.success-meta {
  display: flex;
  justify-content: center;
  gap: 16px;
  flex-wrap: wrap;
  margin: 22px 0 26px;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 180px;
  padding: 16px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #eef2f7;
}

.meta-item span {
  color: var(--g12-text-secondary);
  font-size: 12px;
}

.meta-item strong {
  font-size: 16px;
  color: #1d4ed8;
}

.success-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
}

@media (max-width: 1080px) {
  .apply-grid {
    grid-template-columns: 1fr;
  }
}
</style>
