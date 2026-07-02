<template>
  <div class="knowledge-page">
    <el-card>
      <div class="page-header">
        <span>知识库管理</span>
        <el-button type="primary" @click="showDialog(null)">新建知识库</el-button>
      </div>
    </el-card>

    <el-card style="margin-top:16px">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="kbName" label="名称" width="180" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="embeddingModel" label="嵌入模型" width="140" />
        <el-table-column prop="vectorDbType" label="向量库" width="100" />
        <el-table-column prop="itemCount" label="条目数" width="80" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="$router.push(`/knowledge/${row.id}/items`)">条目</el-button>
            <el-button size="small" @click="showDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top:16px;display:flex;justify-content:flex-end">
        <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total"
          layout="total, prev, pager, next" @change="fetchData" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingKb?.id ? '编辑知识库' : '新建知识库'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="名称" required><el-input v-model="form.kbName" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" /></el-form-item>
        <el-form-item label="嵌入模型"><el-input v-model="form.embeddingModel" placeholder="如 text-embedding-ada-002" /></el-form-item>
        <el-form-item label="向量库类型">
          <el-select v-model="form.vectorDbType" style="width:100%">
            <el-option label="Milvus" value="Milvus" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const dialogVisible = ref(false)
const editingKb = ref(null)

const form = reactive({
  kbName: '', description: '', embeddingModel: '', vectorDbType: 'Milvus', status: 1,
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/kb/page', { params: { pageNum: pageNum.value, pageSize: pageSize.value } })
    tableData.value = res.data.data.records
    total.value = res.data.data.total
  } finally { loading.value = false }
}

const showDialog = (row) => {
  editingKb.value = row
  if (row) {
    Object.assign(form, row)
  } else {
    Object.assign(form, { kbName: '', description: '', embeddingModel: '', vectorDbType: 'Milvus', status: 1 })
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  if (editingKb.value?.id) {
    await request.put(`/kb/${editingKb.value.id}`, form)
  } else {
    await request.post('/kb', form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  fetchData()
}

const handleDelete = async (id) => {
  await ElMessageBox.confirm('删除知识库将同时删除所有条目，确定？', '警告', { type: 'warning' })
  await request.delete(`/kb/${id}`)
  ElMessage.success('删除成功')
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.page-header { display: flex; align-items: center; justify-content: space-between; }
</style>
