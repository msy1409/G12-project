<template>
  <div class="bar-chart">
    <div class="legend">
      <span class="legend-item"><i class="apply"></i>预约数</span>
      <span class="legend-item"><i class="checkin"></i>到访数</span>
    </div>

    <div class="plot">
      <div class="grid-lines">
        <span v-for="i in 4" :key="i"></span>
      </div>
      <div class="groups">
        <div v-for="item in data" :key="item.label" class="group">
          <div class="pair">
            <div class="bar apply" :style="{ height: heightOf(item.applyCount) }" :title="`预约数 ${item.applyCount}`">
              <span class="bar-value">{{ item.applyCount }}</span>
            </div>
            <div class="bar checkin" :style="{ height: heightOf(item.checkinCount) }" :title="`到访数 ${item.checkinCount}`">
              <span class="bar-value">{{ item.checkinCount }}</span>
            </div>
          </div>
          <div class="group-label" :title="item.label">{{ item.label }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  data: { type: Array, default: () => [] }
})

const maxValue = computed(() => {
  const values = props.data.flatMap((d) => [Number(d.applyCount) || 0, Number(d.checkinCount) || 0])
  return Math.max(1, ...values)
})

function heightOf(value) {
  const v = Number(value) || 0
  if (v === 0) return '2px'
  return `${Math.max(8, (v / maxValue.value) * 100)}%`
}
</script>

<style scoped>
.bar-chart {
  width: 100%;
}

.legend {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  font-size: 12px;
  color: var(--g12-text-secondary);
  padding-bottom: 10px;
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.legend-item i {
  width: 10px;
  height: 10px;
  border-radius: 3px;
}

.legend-item i.apply {
  background: #2563eb;
}

.legend-item i.checkin {
  background: #10b981;
}

.plot {
  position: relative;
  height: 230px;
  padding: 8px 10px 0;
  overflow-x: auto;
}

.grid-lines {
  position: absolute;
  inset: 8px 10px 34px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  pointer-events: none;
}

.grid-lines span {
  display: block;
  width: 100%;
  border-top: 1px dashed #eef2f7;
}

.groups {
  position: relative;
  display: flex;
  align-items: flex-end;
  gap: 12px;
  height: 100%;
  min-width: 100%;
  width: max-content;
  padding: 0 4px;
}

.group {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  flex: 1 0 72px;
  height: 100%;
}

.pair {
  display: flex;
  align-items: flex-end;
  gap: 5px;
  width: 100%;
  height: calc(100% - 30px);
}

.bar {
  position: relative;
  flex: 1;
  border-radius: 5px 5px 0 0;
  min-height: 2px;
  transition: height 0.35s ease;
}

.bar.apply {
  background: linear-gradient(180deg, #3b82f6, #2563eb);
}

.bar.checkin {
  background: linear-gradient(180deg, #34d399, #10b981);
}

.bar-value {
  position: absolute;
  top: -17px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 10px;
  color: #64748b;
}

.group-label {
  height: 30px;
  padding-top: 8px;
  max-width: 84px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  color: var(--g12-text-secondary);
}
</style>
