<template>
  <div class="rag-search">
    <el-page-header @back="$router.push('/rag')" title="返回">
      <template #content>RAG 检索测试</template>
    </el-page-header>

    <el-card style="margin-top:16px">
      <el-form :inline="true" @submit.prevent="handleSearch">
        <el-form-item label="查询">
          <el-input v-model="query" placeholder="输入检索内容" style="width:350px" clearable />
        </el-form-item>
        <el-form-item label="知识库">
          <el-select v-model="kbId" style="width:120px">
            <el-option v-for="kb in kbList" :key="kb.id" :label="kb.kbName" :value="kb.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="TopK">
          <el-input-number v-model="topK" :min="1" :max="20" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="loading" icon="Search">检索</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="results.length > 0" style="margin-top:16px">
      <template #header>
        <span>检索结果（{{ results.length }} 条，耗时 {{ elapsed }}ms）</span>
      </template>
      <div v-for="(item, idx) in results" :key="idx" class="result-item">
        <div class="result-header">
          <el-tag type="success" size="small">#{{ idx + 1 }}</el-tag>
          <el-tag type="info" size="small">score: {{ item.score?.toFixed(4) }}</el-tag>
          <el-tag v-if="item.source" type="warning" size="small">{{ item.source }}</el-tag>
        </div>
        <div class="result-content">{{ item.content }}</div>
        <el-divider v-if="idx < results.length - 1" />
      </div>
    </el-card>

    <el-empty v-else-if="searched" description="无检索结果" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const query = ref('')
const kbId = ref(1)
const topK = ref(5)
const loading = ref(false)
const searched = ref(false)
const results = ref([])
const elapsed = ref(0)
const kbList = ref([])

onMounted(async () => {
  const res = await request.get('/kb/page', { params: { pageNum: 1, pageSize: 50 } })
  kbList.value = res.data.data.records
})

const handleSearch = async () => {
  if (!query.value) return
  loading.value = true
  searched.value = true
  try {
    const res = await request.post('/rag/search', { query: query.value, kbId: kbId.value, topK: topK.value })
    const data = res.data.data
    results.value = data.chunks || []
    elapsed.value = data.elapsed_ms || 0
  } catch (e) {
    ElMessage.error('检索失败: ' + (e.response?.data?.message || e.message))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.result-item { margin-bottom: 8px; }
.result-header { display: flex; gap: 8px; margin-bottom: 8px; }
.result-content { line-height: 1.7; white-space: pre-wrap; color: #303133; }
</style>
