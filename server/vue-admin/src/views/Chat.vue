<template>
  <div class="chat-page">
    <el-card>
      <el-form :inline="true" :model="query" size="default">
        <el-form-item label="用户ID">
          <el-input v-model="query.userId" placeholder="用户ID" clearable />
        </el-form-item>
        <el-form-item label="消息类型">
          <el-select v-model="query.messageType" placeholder="全部" clearable style="width:120px">
            <el-option label="用户" value="text" />
            <el-option label="AI" value="ai_reply" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="搜索内容" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="handleExport">导出Excel</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top:16px">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column label="ID" width="80" :index="idIndex" type="index" />
        <el-table-column prop="userId" label="用户ID" width="80" />
        <el-table-column prop="sessionId" label="会话ID" width="120">
          <template #default="{ row }">
            <el-link type="primary" @click="$router.push(`/chat/${row.sessionId}`)">
              {{ row.sessionId?.substring(0, 12) }}...
            </el-link>
          </template>
        </el-table-column>
        <el-table-column prop="avatarName" label="数字人" width="100" />
        <el-table-column prop="messageType" label="类型" width="70">
          <template #default="{ row }">
            <el-tag :type="row.messageType === 'text' ? 'success' : 'warning'" size="small">
              {{ row.messageType === 'text' ? '用户' : 'AI' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="300" show-overflow-tooltip />
        <el-table-column prop="emotion" label="情感" width="80" />
        <el-table-column prop="tokenCount" label="Token" width="80" />
        <el-table-column prop="createTime" label="时间" width="170" />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" size="small" link @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top:16px;display:flex;justify-content:flex-end">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="fetchData"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const query = ref({ pageNum: 1, pageSize: 10, userId: '', messageType: '', keyword: '' })
const tableData = ref([])
const total = ref(0)
const loading = ref(false)

const idIndex = (index) => {
  return (query.value.pageNum - 1) * query.value.pageSize + index + 1
}

const handleSizeChange = () => {
  query.value.pageNum = 1
  fetchData()
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/chat/page', { params: query.value })
    const data = res.data.data
    tableData.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确定删除该记录？', '提示', { type: 'warning' })
  await request.delete(`/chat/${id}`)
  ElMessage.success('删除成功')
  fetchData()
}

const handleExport = () => {
  const token = localStorage.getItem('token')
  const params = new URLSearchParams(query.value).toString()
  window.open(`/api/admin/chat/export?${params}`, '_blank')
}

onMounted(fetchData)
</script>
