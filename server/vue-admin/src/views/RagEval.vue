<template>
  <div class="rag-eval">
    <el-page-header @back="$router.push('/rag')" title="返回">
      <template #content>RAG 评测管理</template>
    </el-page-header>

    <el-card style="margin-top:16px">
      <div class="page-header">
        <span>评测问答对管理</span>
        <div>
          <el-button type="primary" @click="showQaDialog(null)">新增问答对</el-button>
          <el-button type="success" @click="handleRunEval" :loading="evalRunning">运行评测</el-button>
        </div>
      </div>
    </el-card>

    <!-- 评测指标概览 -->
    <el-row :gutter="20" style="margin-top:16px" v-if="evalResult">
      <el-col :span="4">
        <el-statistic title="Recall@5" :value="evalResult.recall_at_5" :precision="4" />
      </el-col>
      <el-col :span="4">
        <el-statistic title="Recall@10" :value="evalResult.recall_at_10" :precision="4" />
      </el-col>
      <el-col :span="4">
        <el-statistic title="MRR" :value="evalResult.mrr" :precision="4" />
      </el-col>
      <el-col :span="4">
        <el-statistic title="NDCG@5" :value="evalResult.ndcg_at_5" :precision="4" />
      </el-col>
    </el-row>

    <el-card style="margin-top:16px">
      <el-table :data="qaPairs" v-loading="loading" stripe>
        <el-table-column prop="question" label="问题" min-width="300" show-overflow-tooltip />
        <el-table-column prop="difficulty" label="难度" width="80" />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" @click="showQaDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- QA Dialog -->
    <el-dialog v-model="dialogVisible" :title="editingQa?.id ? '编辑' : '新增'" width="500px">
      <el-form :model="qaForm" label-width="80px">
        <el-form-item label="问题"><el-input v-model="qaForm.question" /></el-form-item>
        <el-form-item label="难度">
          <el-select v-model="qaForm.difficulty">
            <el-option :value="1" label="简单" />
            <el-option :value="2" label="中等" />
            <el-option :value="3" label="困难" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类"><el-input v-model="qaForm.category" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const qaPairs = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const editingQa = ref(null)
const evalRunning = ref(false)
const evalResult = ref(null)

const qaForm = ref({ question: '', difficulty: 1, category: '' })

onMounted(fetchData)

async function fetchData() {
  loading.value = true
  try {
    const res = await request.get('/rag/qa/page', { params: { pageNum: 1, pageSize: 50 } })
    qaPairs.value = res.data.data.records || []
  } catch {}
  loading.value = false
}

function showQaDialog(row) {
  editingQa.value = row
  qaForm.value = row ? { ...row } : { question: '', difficulty: 1, category: '' }
  dialogVisible.value = true
}

async function handleSave() {
  if (editingQa.value?.id) {
    await request.put(`/rag/qa/${editingQa.value.id}`, qaForm.value)
  } else {
    await request.post('/rag/qa', qaForm.value)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  fetchData()
}

async function handleDelete(id) {
  await ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' })
  await request.delete(`/rag/qa/batch`, { data: [id] })
  ElMessage.success('删除成功')
  fetchData()
}

async function handleRunEval() {
  evalRunning.value = true
  try {
    const res = await request.post('/rag/eval/run')
    evalResult.value = res.data.data
    ElMessage.success('评测完成')
  } catch (e) {
    ElMessage.error('评测失败')
  }
  evalRunning.value = false
}
</script>

<style scoped>
.page-header { display: flex; align-items: center; justify-content: space-between; }
</style>
