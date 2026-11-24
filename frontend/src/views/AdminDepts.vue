<template>
  <div class="page">
    <PageHeader title="部门管理" description="维护企业部门信息。部门下仍有启用用户或未完结预约时，停用会被后端拒绝（DEPT_IN_USE）。">
      <template #badge>
        <el-tag effect="plain" round size="small">启用 {{ activeCount }} / 共 {{ departments.length }}</el-tag>
      </template>
      <template #actions>
        <el-button type="primary" @click="openCreate">新增部门</el-button>
      </template>
    </PageHeader>

    <div class="card card-pad">
      <div class="toolbar filters">
        <el-input
          v-model="keyword"
          placeholder="部门编码 / 名称 / 负责人"
          clearable
          style="width: 260px"
        />
        <el-button :loading="loading" @click="load">刷新</el-button>
        <div class="spacer"></div>
        <span class="muted">该接口不分页，返回全部部门（含停用），前端按关键字过滤</span>
      </div>

      <DataTable
        :columns="columns"
        :data="filteredDepartments"
        :loading="loading"
        :show-pagination="false"
        empty-text="没有符合条件的部门"
      >
        <template #status="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="plain" size="small">
            {{ row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>

        <template #actions="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="row.status === 1" link type="warning" size="small" @click="disableDept(row)">停用</el-button>
          <span v-else class="muted">已停用</span>
        </template>
      </DataTable>
    </div>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑部门' : '新增部门'" width="520px" align-center destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-form-item label="部门编码" prop="deptCode">
          <el-input v-model="form.deptCode" :disabled="Boolean(editing)" placeholder="如 D011" maxlength="10" />
        </el-form-item>
        <el-form-item label="部门名称" prop="deptName">
          <el-input v-model="form.deptName" placeholder="如 法务部" maxlength="30" />
        </el-form-item>
        <el-form-item label="负责人" prop="managerName">
          <el-input v-model="form.managerName" placeholder="部门负责人姓名" maxlength="30" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" placeholder="座机或手机号" maxlength="20" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">{{ editing ? '保存修改' : '创建部门' }}</el-button>
      </template>
    </el-dialog>

    <ConfirmDialog ref="confirmRef" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import DataTable from '@/components/DataTable.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import { deptApi } from '@/api'

const departments = ref([])
const loading = ref(false)
const keyword = ref('')
const confirmRef = ref(null)

const columns = [
  { prop: 'deptCode', label: '部门编码', width: 120 },
  { prop: 'deptName', label: '部门名称', minWidth: 150 },
  { prop: 'managerName', label: '负责人', width: 110 },
  { prop: 'phone', label: '联系电话', minWidth: 140 },
  { prop: 'status', label: '状态', width: 90, slot: 'status' },
  { prop: 'createTime', label: '创建时间', minWidth: 160 },
  { prop: 'actions', label: '操作', width: 140, fixed: 'right', slot: 'actions', tooltip: false }
]

const activeCount = computed(() => departments.value.filter((x) => x.status === 1).length)

const filteredDepartments = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  if (!k) return departments.value
  return departments.value.filter((x) =>
    [x.deptCode, x.deptName, x.managerName].some((v) => String(v || '').toLowerCase().includes(k))
  )
})

const dialogVisible = ref(false)
const editing = ref(null)
const saving = ref(false)
const formRef = ref(null)
const form = reactive({
  deptCode: '',
  deptName: '',
  managerName: '',
  phone: ''
})

const rules = {
  deptCode: [
    { required: true, message: '请输入部门编码', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9-]{2,10}$/, message: '2–10 位字母、数字或短横线', trigger: 'blur' }
  ],
  deptName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
  managerName: [{ required: true, message: '请输入负责人', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    departments.value = (await deptApi.list()) || []
  } catch {
    departments.value = []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  Object.assign(form, { deptCode: '', deptName: '', managerName: '', phone: '' })
  dialogVisible.value = true
}

function openEdit(row) {
  editing.value = row
  Object.assign(form, {
    deptCode: row.deptCode,
    deptName: row.deptName,
    managerName: row.managerName,
    phone: row.phone
  })
  dialogVisible.value = true
}

async function save() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (editing.value) {
      await deptApi.update(editing.value.id, {
        deptName: form.deptName,
        managerName: form.managerName,
        phone: form.phone
      })
      ElMessage.success('部门信息已更新')
    } else {
      await deptApi.create({ ...form })
      ElMessage.success('部门创建成功')
    }
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function disableDept(row) {
  const confirmed = await confirmRef.value.open({
    title: '停用部门',
    message: `确定停用「${row.deptName}」吗？部门下有启用用户或未完结预约时，后端会拒绝并返回 DEPT_IN_USE。`,
    confirmText: '停用',
    danger: true
  })
  if (!confirmed) return
  try {
    await deptApi.remove(row.id)
    ElMessage.success('部门已停用')
    await load()
  } catch {
    // 错误提示由响应拦截器统一弹出
  }
}

onMounted(load)
</script>

<style scoped>
.filters {
  margin-bottom: 14px;
}
</style>
