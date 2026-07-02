<template>
  <div class="knowledge-items">
    <el-page-header @back="$router.push('/knowledge')" title="返回">
      <template #content>知识条目管理</template>
    </el-page-header>

    <el-card style="margin-top:16px">
      <div class="page-header">
        <span>知识库ID: {{ kbId }}</span>
        <div>
          <el-button type="primary" @click="showDialog(null)">新增条目</el-button>
          <el-button @click="showBatchImport = true">批量导入</el-button>
        </div>
      </div>
    </el-card>

    <el-card style="margin-top:16px">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="title" label="标题" width="200" />
        <el-table-column prop="content" label="内容" show-overflow-tooltip />
        <el-table-column prop="vectorStatus" label="向量化" width="80">
          <template #default="{ row }">
            <el-tag :type="row.vectorStatus === 1 ? 'success' : 'warning'" size="small">
              {{ row.vectorStatus === 1 ? '已完成' : '未处理' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
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

    <el-dialog v-model="dialogVisible" :title="editingItem?.id ? '编辑条目' : '新增条目'" width="600px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="form.content" type="textarea" :rows="6" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showBatchImport" title="批量导入" width="600px">
      <el-input v-model="batchJson" type="textarea" :rows="10" placeholder='[{"title":"问题1","content":"答案1"},{"title":"问题2","content":"答案2"}]' />
      <template #footer>
        <el-button @click="showBatchImport = false">取消</el-button>
        <el-button type="primary" @click="handleBatchImport">导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const kbId = ref(route.params.kbId)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const dialogVisible = ref(false)
const editingItem = ref(null)
const showBatchImport = ref(false)
const batchJson = ref('')

const form = reactive({ title: '', content: '' })

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get(`/kb/${kbId.value}/item/page`, { params: { pageNum: pageNum.value, pageSize: pageSize.value } })
    tableData.value = res.data.data.records
    total.value = res.data.data.total
  } finally { loading.value = false }
}

const showDialog = (row) => {
  editingItem.value = row
  if (row) { Object.assign(form, { title: row.title, content: row.content }) }
  else { Object.assign(form, { title: '', content: '' }) }
  dialogVisible.value = true
}

const handleSave = async () => {
  if (editingItem.value?.id) {
    await request.put(`/kb/${kbId.value}/item/${editingItem.value.id}`, form)
  } else {
    await request.post(`/kb/${kbId.value}/item`, form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  fetchData()
}

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' })
  await request.delete(`/kb/${kbId.value}/item/${id}`)
  ElMessage.success('删除成功')
  fetchData()
}

const handleBatchImport = async () => {
  try {
    const items = JSON.parse(batchJson.value)
    await request.post(`/kb/${kbId.value}/item/batch-import`, items)
    ElMessage.success('导入成功')
    showBatchImport.value = false
    batchJson.value = ''
    fetchData()
  } catch (e) {
    ElMessage.error('JSON格式错误')
  }
}

onMounted(fetchData)
</script>

<style scoped>
.page-header { display: flex; align-items: center; justify-content: space-between; }
</style>
