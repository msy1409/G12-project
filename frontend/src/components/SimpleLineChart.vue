<template>
  <div class="line-chart">
    <div class="legend">
      <span v-for="s in series" :key="s.name" class="legend-item">
        <i :style="{ background: s.color }"></i>{{ s.name }}
      </span>
    </div>

    <svg :viewBox="`0 0 ${W} ${H}`" role="img" aria-label="趋势折线图">
      <g v-for="tick in yTicks" :key="tick">
        <line :x1="PAD.left" :x2="W - PAD.right" :y1="yOf(tick)" :y2="yOf(tick)" class="grid" />
        <text :x="PAD.left - 8" :y="yOf(tick) + 4" text-anchor="end" class="axis">{{ tick }}</text>
      </g>

      <g v-for="(label, i) in labels" :key="label">
        <text v-if="showLabel(i)" :x="xOf(i)" :y="H - 8" text-anchor="middle" class="axis">{{ shortLabel(label) }}</text>
      </g>

      <g v-for="s in series" :key="s.name">
        <polyline :points="pointsOf(s.data)" fill="none" :stroke="s.color" stroke-width="2.6" stroke-linejoin="round" stroke-linecap="round" />
        <circle
          v-for="(v, i) in s.data"
          :key="i"
          :cx="xOf(i)"
          :cy="yOf(v)"
          r="3.6"
          :fill="s.color"
          stroke="#fff"
          stroke-width="1.5"
        >
          <title>{{ labels[i] }} · {{ s.name }}：{{ v }}</title>
        </circle>
      </g>
    </svg>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  labels: { type: Array, default: () => [] },
  series: { type: Array, default: () => [] }
})

const W = 720
const H = 240
const PAD = { top: 16, right: 18, bottom: 34, left: 42 }

const maxValue = computed(() => {
  const values = props.series.flatMap((s) => s.data || [])
  const max = values.length ? Math.max(...values) : 0
  if (max <= 4) return 4
  return Math.ceil(max / 4) * 4
})

const yTicks = computed(() => {
  const max = maxValue.value
  return [0, max / 4, max / 2, (max * 3) / 4, max].map((v) => Math.round(v))
})

const plotWidth = W - PAD.left - PAD.right
const plotHeight = H - PAD.top - PAD.bottom

function xOf(index) {
  const n = props.labels.length
  if (n <= 1) return PAD.left + plotWidth / 2
  return PAD.left + (index / (n - 1)) * plotWidth
}

function yOf(value) {
  return PAD.top + plotHeight - (Number(value) / maxValue.value) * plotHeight
}

function pointsOf(data) {
  return (data || []).map((v, i) => `${xOf(i)},${yOf(v)}`).join(' ')
}

function showLabel(index) {
  const n = props.labels.length
  if (n <= 8) return true
  return index % Math.ceil(n / 8) === 0 || index === n - 1
}

function shortLabel(label) {
  return String(label || '').slice(5)
}
</script>

<style scoped>
.line-chart {
  width: 100%;
}

.legend {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  padding: 0 4px 6px;
  font-size: 12px;
  color: var(--g12-text-secondary);
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

svg {
  width: 100%;
  height: auto;
  display: block;
}

.grid {
  stroke: #eef2f7;
  stroke-width: 1;
}

.axis {
  fill: #94a3b8;
  font-size: 11px;
}
</style>
