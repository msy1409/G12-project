<template>
  <div class="data-table">
    <el-table
      v-loading="loading"
      :data="data"
      :row-key="rowKey"
      :stripe="stripe"
      :highlight-current-row="highlight"
      header-cell-class-name="table-head"
      style="width: 100%"
      @row-click="onRowClick"
    >
      <el-table-column
        v-for="col in columns"
        :key="col.prop || col.label"
        :prop="col.prop"
        :label="col.label"
        :width="col.width"
        :min-width="col.minWidth"
        :fixed="col.fixed"
        :align="col.align || 'left'"
        :show-overflow-tooltip="col.tooltip !== false"
      >
        <template #default="scope">
          <slot v-if="col.slot" :name="col.slot" v-bind="scope" />
          <span v-else>{{ formatCell(scope.row, col) }}</span>
        </template>
      </el-table-column>

      <template #empty>
        <el-empty :description="emptyText" :image-size="72" />
      </template>
    </el-table>

    <div v-if="showPagination" class="pager">
      <el-pagination
        background
        :current-page="page"
        :page-size="size"
        :page-sizes="pageSizes"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="onPageChange"
        @size-change="onSizeChange"
      />
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  columns: { type: Array, default: () => [] },
  data: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  total: { type: Number, default: 0 },
  page: { type: Number, default: 1 },
  size: { type: Number, default: 10 },
  pageSizes: { type: Array, default: () => [10, 20, 50] },
  rowKey: { type: String, default: 'id' },
  showPagination: { type: Boolean, default: true },
  stripe: { type: Boolean, default: true },
  highlight: { type: Boolean, default: false },
  emptyText: { type: String, default: '暂无数据' },
  rowClickable: { type: Boolean, default: false }
})

const emit = defineEmits(['update:page', 'update:size', 'change', 'row-click'])

function formatCell(row, col) {
  const value = row[col.prop]
  if (col.formatter) return col.formatter(value, row)
  if (value === null || value === undefined || value === '') return '—'
  return value
}

function onPageChange(page) {
  emit('update:page', page)
  emit('change', { page, size: props.size })
}

function onSizeChange(size) {
  emit('update:size', size)
  emit('update:page', 1)
  emit('change', { page: 1, size })
}

function onRowClick(row) {
  if (props.rowClickable) emit('row-click', row)
}
</script>

<style scoped>
.data-table {
  width: 100%;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}
</style>
