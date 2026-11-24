<template>
  <el-button v-if="allowed" v-bind="$attrs" @click="$emit('click', $event)">
    <slot />
  </el-button>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'

defineOptions({ inheritAttrs: false })

const props = defineProps({
  roles: { type: Array, default: () => [] }
})

defineEmits(['click'])

const store = useUserStore()
const allowed = computed(() => store.hasRole(props.roles))
</script>
