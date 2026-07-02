<template>
  <div class="rag-page">
    <el-card>
      <div class="page-header">
        <span>RAG 文档管理</span>
        <el-button type="primary" @click="$router.push('/rag/upload')">上传文档</el-button>
      </div>
    </el-card>

    <el-card style="margin-top:16px">
      <el-form :inline="true" :model="query" size="default">
        <el-form-item label="状态">
          <el-select v-model="query.processStatus" placeholder="全部" clearable style="width:120px">
            <el-option label="待处理" :value="0" />
            <el-option label="解析中" :value="1" />
            <el-option label="已完成" :value="4" />
            <el-option label="失败" :value="-1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 处理进度提示 -->
    <el-card v-if="processingId" style="margin-top:16px" :body-style="{ padding: '16px' }">
      <div style="display:flex;align-items:center;gap:12px">
        <el-icon :size="20" color="#409EFF"><Loading /></el-icon>
        <span style="font-weight:500">文档正在处理中...</span>
        <el-tag :type="statusTagType" size="small">{{ statusLabel }}</el-tag>
        <el-progress v-if="progressPercent > 0" :percentage="progressPercent" :stroke-width="8" style="flex:1;max-width:300px" />
        <el-button size="small" text @click="stopPolling">取消监控</el-button>
      </div>
    </el-card>

    <el-card style="margin-top:16px">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="docName" label="文档名" min-width="200" show-overflow-tooltip />
        <el-table-column prop="docType" label="格式" width="80" />
        <el-table-column prop="fileSize" label="大小" width="100" :formatter="formatSize" />
        <el-table-column prop="chunkCount" label="块数" width="70" />
        <el-table-column prop="processStatus" label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.processStatus===0" type="info" size="small">待处理</el-tag>
            <el-tag v-else-if="row.processStatus===1" type="" size="small" effect="dark">解析中</el-tag>
            <el-tag v-else-if="row.processStatus===2" type="warning" size="small" effect="dark">切割中</el-tag>
            <el-tag v-else-if="row.processStatus===3" type="warning" size="small" effect="dark">向量化中</el-tag>
            <el-tag v-else-if="row.processStatus===4" type="success" size="small">已完成</el-tag>
            <el-tag v-else type="danger" size="small">失败</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="上传时间" width="170" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" :loading="row.id===processingId" @click="handleProcess(row.id)">
              {{ row.processStatus===4 ? '重新处理' : '处理' }}
            </el-button>
            <el-button size="small" @click="$router.push(`/rag/${row.id}/chunks`)">块列表</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top:16px;display:flex;justify-content:flex-end">
        <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total"
          layout="total, prev, pager, next" @change="fetchData" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const query = ref({ processStatus: undefined })
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const processingId = ref(null)
const currentStatus = ref(0)
const progressPercent = ref(0)
let pollTimer = null

// 状态映射
const statusMap = { 0: '待处理', 1: '解析中', 2: '切割中', 3: '向量化中', 4: '已完成', '-1': '失败' }

const statusLabel = computed(() => statusMap[currentStatus.value] || '未知')
const statusTagType = computed(() => {
  if (currentStatus.value === 4) return 'success'
  if (currentStatus.value === -1) return 'danger'
  return 'warning'
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/rag/page', { params: { ...query.value, pageNum: pageNum.value, pageSize: pageSize.value } })
    tableData.value = res.data.data.records
    total.value = res.data.data.total
  } finally { loading.value = false }
}

const formatSize = (row) => {
  const size = row.fileSize
  if (!size) return '-'
  if (size < 1024) return size + 'B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + 'KB'
  return (size / (1024 * 1024)).toFixed(1) + 'MB'
}

const pollStatus = (docId) => {
  stopPolling()
  processingId.value = docId
  currentStatus.value = 0
  progressPercent.value = 0

  pollTimer = setInterval(async () => {
    try {
      const res = await request.get(`/rag/${docId}/status`)
      const status = res.data.data.processStatus
      const realPercent = res.data.data.progressPercent || 0
      currentStatus.value = status
      // 使用后端返回的真实进度百分比，不做硬编码映射
      progressPercent.value = realPercent

      // 完成或失败时停止轮询并刷新表格
      if (status === 4 || status === -1) {
        stopPolling()
        fetchData()
        if (status === 4) ElMessage.success(`文档处理完成，共 ${res.data.data.chunkCount} 个chunk`)
        else ElMessage.error('文档处理失败')
      }
    } catch (e) {
      stopPolling()
    }
  }, 1000)
}

const stopPolling = () => {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
  processingId.value = null
  currentStatus.value = 0
  progressPercent.value = 0
}

const handleProcess = async (id) => {
  try {
    await request.post(`/rag/${id}/process`)
    ElMessage.success('处理任务已触发')
    pollStatus(id)
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '触发失败')
  }
}

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' })
  await request.delete(`/rag/${id}`)
  ElMessage.success('删除成功')
  if (id === processingId.value) stopPolling()
  fetchData()
}

onMounted(fetchData)
onUnmounted(stopPolling)
</script>

<style scoped>
.page-header { display: flex; align-items: center; justify-content: space-between; }
</style>
