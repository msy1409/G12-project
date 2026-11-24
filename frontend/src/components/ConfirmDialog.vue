<template>
  <el-dialog
    v-model="visible"
    :title="options.title"
    width="430px"
    align-center
    append-to-body
    :close-on-click-modal="false"
    @closed="onClosed"
  >
    <div class="confirm-body">
      <div class="icon-wrap" :class="{ danger: options.danger }">
        <span class="mark">{{ options.danger ? '!' : '?' }}</span>
      </div>
      <div class="text">
        <p class="message">{{ options.message }}</p>
        <p v-if="options.hint" class="hint">{{ options.hint }}</p>
      </div>
    </div>

    <template #footer>
      <el-button @click="cancel">{{ options.cancelText }}</el-button>
      <el-button :type="options.danger ? 'danger' : 'primary'" :loading="options.loading" @click="confirm">
        {{ options.confirmText }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref } from 'vue'

const visible = ref(false)
const options = reactive({
  title: '操作确认',
  message: '',
  hint: '',
  confirmText: '确定',
  cancelText: '取消',
  danger: false,
  loading: false
})

let resolver = null

function open(opts = {}) {
  Object.assign(options, {
    title: '操作确认',
    message: '',
    hint: '',
    confirmText: '确定',
    cancelText: '取消',
    danger: false,
    loading: false
  }, opts)
  visible.value = true
  return new Promise((resolve) => {
    resolver = resolve
  })
}

function close(result) {
  visible.value = false
  if (resolver) {
    resolver(result)
    resolver = null
  }
}

function confirm() {
  close(true)
}

function cancel() {
  close(false)
}

// 点右上角 × 或遮罩关闭时按“取消”处理
function onClosed() {
  if (resolver) {
    resolver(false)
    resolver = null
  }
}

function setLoading(loading) {
  options.loading = loading
}

defineExpose({ open, close, setLoading })
</script>

<style scoped>
.confirm-body {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  padding: 2px 0 6px;
}

.icon-wrap {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: #eff6ff;
  color: #2563eb;
}

.icon-wrap.danger {
  background: #fef2f2;
  color: #dc2626;
}

.mark {
  font-size: 20px;
  font-weight: 700;
  line-height: 1;
}

.message {
  margin: 2px 0 0;
  font-size: 14px;
  line-height: 1.6;
  color: #1f2937;
  word-break: break-word;
}

.hint {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--g12-text-secondary);
}
</style>
