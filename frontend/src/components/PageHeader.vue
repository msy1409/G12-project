<template>
  <div class="page-header">
    <div class="heading">
      <div class="title-row">
        <h2 class="title">{{ title }}</h2>
        <slot name="badge"></slot>
      </div>
      <p v-if="description" class="desc">{{ description }}</p>
    </div>
    <div v-if="$slots.actions" class="actions">
      <slot name="actions"></slot>
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  description: { type: String, default: '' }
})
</script>

<style scoped>
.page-header {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.2px;
  color: #111827;
}

.desc {
  margin: 6px 0 0;
  color: var(--g12-text-secondary);
  font-size: 13px;
}

.actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
