<template>
  <div class="scenic-page">
    <div class="page-header">
      <h3>景区管理</h3>
      <el-button type="primary" @click="openDialog()">新增景区</el-button>
    </div>

    <el-table :data="list" stripe v-loading="loading">
      <el-table-column label="#" width="60" align="center">
        <template #default="{ $index }">{{ $index + 1 }}</template>
      </el-table-column>
      <el-table-column prop="spotName" label="景区名称" min-width="120" />
      <el-table-column prop="spotCode" label="编码" width="100" />
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" align="center">
        <template #default="{ row }">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" :title="form.id ? '编辑景区' : '新增景区'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="景区名称">
          <el-input v-model="form.spotName" placeholder="如：灵山胜境" />
        </el-form-item>
        <el-form-item label="编码">
          <el-input v-model="form.spotCode" placeholder="如：lingshan" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="简要描述" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const list = ref([])
const loading = ref(false)
const visible = ref(false)
const form = ref({ spotName: '', spotCode: '', description: '', status: 1 })

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/scenic/page', { params: { pageNum: 1, pageSize: 50 } })
    list.value = res.data?.data?.records || []
  } finally {
    loading.value = false
  }
}

const openDialog = (row) => {
  form.value = row ? { ...row } : { spotName: '', spotCode: '', description: '', status: 1 }
  visible.value = true
}

const handleSave = async () => {
  try {
    const api = form.value.id ? request.put('/scenic', form.value) : request.post('/scenic', form.value)
    await api
    visible.value = false
    fetchList()
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.response?.data?.message || e.message))
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该景区吗？', '提示', { type: 'warning' })
    await request.delete(`/scenic/${row.id}`)
    fetchList()
  } catch (e) { }
}

onMounted(fetchList)
</script>

<style scoped>
.scenic-page {
  max-width: 1000px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.page-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}
</style>
