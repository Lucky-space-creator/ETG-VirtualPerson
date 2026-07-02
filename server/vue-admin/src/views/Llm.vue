<template>
  <div class="llm-page">
    <el-card>
      <div class="page-header">
        <span>LLM 大模型配置管理</span>
        <el-button type="primary" @click="$router.push('/llm/edit')">新增配置</el-button>
      </div>
    </el-card>

    <el-card style="margin-top:16px">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="configName" label="配置名称" width="150" />
        <el-table-column prop="provider" label="提供商" width="100" />
        <el-table-column prop="modelName" label="模型" width="150" />
        <el-table-column prop="temperature" label="温度" width="70" />
        <el-table-column prop="maxTokens" label="最大Token" width="100" />
        <el-table-column prop="isDefault" label="默认" width="70">
          <template #default="{ row }">
            <el-tag v-if="row.isDefault" type="success" size="small">默认</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="connectStatus" label="连通" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.connectStatus === 1" type="success" size="small">正常</el-tag>
            <el-tag v-else-if="row.connectStatus === 2" type="danger" size="small">失败</el-tag>
            <el-tag v-else type="info" size="small">未知</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="$router.push(`/llm/edit/${row.id}`)">编辑</el-button>
            <el-button size="small" type="warning" @click="handleTest(row.id)">测试</el-button>
            <el-button size="small" type="success" @click="handleDefault(row.id)">设默认</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top:16px;display:flex;justify-content:flex-end">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @change="fetchData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/llm/page', { params: { pageNum: pageNum.value, pageSize: pageSize.value } })
    tableData.value = res.data.data.records
    total.value = res.data.data.total
  } finally {
    loading.value = false
  }
}

const handleTest = async (id) => {
  const res = await request.post(`/llm/${id}/test`)
  ElMessage.success(res.data.data.message + ` (${res.data.data.elapsed}ms)`)
}

const handleDefault = async (id) => {
  await request.put(`/llm/${id}/default`)
  ElMessage.success('已设为默认')
  fetchData()
}

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' })
  await request.delete(`/llm/${id}`)
  ElMessage.success('删除成功')
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
