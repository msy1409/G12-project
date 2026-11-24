<template>
  <el-tag :type="meta.type" :size="size" effect="light" round class="status-tag">
    <span class="dot" :style="{ background: meta.color }"></span>
    {{ meta.label }}
  </el-tag>
</template>

<script setup>
import { computed } from 'vue'
import { STATUS } from '@/constants'

const props = defineProps({
  status: { type: [Number, String], default: 0 },
  size: { type: String, default: 'default' }
})

const meta = computed(() => STATUS[Number(props.status)] || { label: '未知', type: 'info', color: '#94a3b8' })
</script>

<style scoped>
.status-tag {
  font-weight: 500;
}

.dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  margin-right: 5px;
  border-radius: 50%;
  vertical-align: 1px;
}
</style>
