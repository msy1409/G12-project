<template>
  <div class="page">
    <PageHeader title="用户管理" description="维护系统账号、所属部门与角色。停用立即禁止登录，删除按契约实现为停用，不做物理删除。">
      <template #actions>
        <el-button type="primary" @click="openCreate">新增用户</el-button>
      </template>
    </PageHeader>

    <div class="card card-pad">
      <div class="toolbar filters">
        <el-input
          v-model="filters.keyword"
          placeholder="用户名 / 姓名 / 手机号"
          clearable
          style="width: 220px"
          @keyup.enter="onSearch"
          @clear="onSearch"
        />
        <el-select v-model="filters.deptId" placeholder="全部部门" clearable filterable style="width: 160px" @change="onSearch">
          <el-option v-for="dept in departments" :key="dept.id" :label="dept.deptName" :value="dept.id" />
        </el-select>
        <el-select v-model="filters.role" placeholder="全部角色" clearable style="width: 140px" @change="onSearch">
          <el-option label="员工" value="EMPLOYEE" />
          <el-option label="审核员" value="AUDITOR" />
          <el-option label="管理员" value="ADMIN" />
        </el-select>
        <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 130px" @change="onSearch">
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
        <el-button type="primary" plain @click="onSearch">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
        <div class="spacer"></div>
        <span class="muted">共 {{ total }} 个账号</span>
      </div>

      <DataTable
        v-model:page="page"
        v-model:size="size"
        :columns="columns"
        :data="rows"
        :loading="loading"
        :total="total"
        empty-text="没有符合条件的用户"
        @change="load"
      >
        <template #role="{ row }">
          <el-tag :type="ROLE_TAG_TYPE[row.role]" effect="light" round size="small">
            {{ ROLE_LABEL[row.role] || row.role }}
          </el-tag>
        </template>

        <template #status="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="plain" size="small">
            {{ row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>

        <template #actions="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button
            v-if="row.status === 1"
            link
            type="warning"
            size="small"
            :disabled="isSelf(row)"
            @click="disableUser(row)"
          >
            停用
          </el-button>
          <el-button v-else link type="success" size="small" @click="enableUser(row)">启用</el-button>
          <el-button link type="danger" size="small" :disabled="isSelf(row)" @click="removeUser(row)">删除</el-button>
        </template>
      </DataTable>
    </div>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑用户' : '新增用户'" width="560px" align-center destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <div class="form-grid">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" :disabled="editing" placeholder="登录账号，唯一" maxlength="30" />
          </el-form-item>
          <el-form-item v-if="!editing" label="初始密码" prop="password">
            <el-input v-model="form.password" type="password" show-password placeholder="明文传入，后端 BCrypt" maxlength="30" />
          </el-form-item>
          <el-form-item label="姓名" prop="realName">
            <el-input v-model="form.realName" placeholder="真实姓名" maxlength="30" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="form.phone" placeholder="11 位手机号" maxlength="11" />
          </el-form-item>
          <el-form-item label="所属部门" prop="deptId">
            <el-select v-model="form.deptId" placeholder="选择部门" filterable style="width: 100%">
              <el-option v-for="dept in departments" :key="dept.id" :label="dept.deptName" :value="dept.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="角色" prop="role">
            <el-select v-model="form.role" placeholder="选择角色" style="width: 100%">
              <el-option label="员工 EMPLOYEE" value="EMPLOYEE" />
              <el-option label="审核员 AUDITOR" value="AUDITOR" />
              <el-option label="管理员 ADMIN" value="ADMIN" />
            </el-select>
          </el-form-item>
        </div>
        <p v-if="!editing" class="muted dialog-hint">密码由服务端 BCrypt 加密存储，任何接口响应都不会返回 password 字段。</p>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">{{ editing ? '保存修改' : '创建用户' }}</el-button>
      </template>
    </el-dialog>

    <ConfirmDialog ref="confirmRef" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import DataTable from '@/components/DataTable.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import { deptApi, userApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ROLE_LABEL, ROLE_TAG_TYPE } from '@/constants'

const store = useUserStore()

const rows = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const departments = ref([])
const confirmRef = ref(null)

const filters = reactive({ keyword: '', deptId: '', role: '', status: '' })

const columns = [
  { prop: 'id', label: 'ID', width: 70 },
  { prop: 'username', label: '用户名', minWidth: 120 },
  { prop: 'realName', label: '姓名', width: 100 },
  { prop: 'phone', label: '手机号', width: 130 },
  { prop: 'deptName', label: '部门', minWidth: 120 },
  { prop: 'role', label: '角色', width: 100, slot: 'role' },
  { prop: 'status', label: '状态', width: 90, slot: 'status' },
  { prop: 'createTime', label: '创建时间', minWidth: 160 },
  { prop: 'actions', label: '操作', width: 170, fixed: 'right', slot: 'actions', tooltip: false }
]

const dialogVisible = ref(false)
const editing = ref(null)
const saving = ref(false)
const formRef = ref(null)
const form = reactive({
  username: '',
  password: '',
  realName: '',
  phone: '',
  deptId: null,
  role: 'EMPLOYEE'
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{3,30}$/, message: '3–30 位字母、数字或下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 6, max: 30, message: '密码长度 6–30 位', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    {
      validator: (rule, value, callback) => {
        if (!value) callback()
        else if (!/^1[3-9]\d{9}$/.test(value)) callback(new Error('手机号格式不正确'))
        else callback()
      },
      trigger: 'blur'
    }
  ],
  deptId: [{ required: true, message: '请选择部门', trigger: 'change' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

function isSelf(row) {
  return row.id === store.user?.id
}

async function load() {
  loading.value = true
  try {
    const data = await userApi.list({
      page: page.value,
      size: size.value,
      keyword: filters.keyword || undefined,
      deptId: filters.deptId || undefined,
      role: filters.role || undefined,
      status: filters.status === '' ? undefined : filters.status
    })
    rows.value = data.list || []
    total.value = data.total || 0
  } catch {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function onSearch() {
  page.value = 1
  load()
}

function resetFilters() {
  Object.assign(filters, { keyword: '', deptId: '', role: '', status: '' })
  page.value = 1
  load()
}

async function loadDepartments() {
  try {
    departments.value = await deptApi.list()
  } catch {
    departments.value = []
  }
}

function openCreate() {
  editing.value = null
  Object.assign(form, {
    username: '',
    password: '123456',
    realName: '',
    phone: '',
    deptId: departments.value[0]?.id || null,
    role: 'EMPLOYEE'
  })
  dialogVisible.value = true
}

function openEdit(row) {
  editing.value = row
  Object.assign(form, {
    username: row.username,
    password: '',
    realName: row.realName,
    phone: row.phone || '',
    deptId: row.deptId,
    role: row.role
  })
  dialogVisible.value = true
}

async function save() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (editing.value) {
      await userApi.update(editing.value.id, {
        realName: form.realName,
        phone: form.phone,
        deptId: form.deptId,
        role: form.role
      })
      ElMessage.success('用户信息已更新')
    } else {
      await userApi.create({ ...form })
      ElMessage.success('用户创建成功')
    }
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function disableUser(row) {
  const confirmed = await confirmRef.value.open({
    title: '停用用户',
    message: `停用 ${row.realName}（${row.username}）后该账号立即无法登录，已签发的 token 也会失效。`,
    confirmText: '停用',
    danger: true
  })
  if (!confirmed) return
  await userApi.setStatus(row.id, 0)
  ElMessage.success('账号已停用')
  await load()
}

async function enableUser(row) {
  await userApi.setStatus(row.id, 1)
  ElMessage.success('账号已启用')
  await load()
}

async function removeUser(row) {
  const confirmed = await confirmRef.value.open({
    title: '删除用户',
    message: `确定删除 ${row.realName}（${row.username}）吗？按契约实现为停用（status=0），历史记录保留。`,
    confirmText: '删除',
    danger: true
  })
  if (!confirmed) return
  await userApi.remove(row.id)
  ElMessage.success('用户已停用（逻辑删除）')
  await load()
}

onMounted(() => {
  loadDepartments()
  load()
})
</script>

<style scoped>
.filters {
  margin-bottom: 14px;
}

.dialog-hint {
  margin: 4px 0 0;
  padding-left: 88px;
  line-height: 1.6;
}
</style>
