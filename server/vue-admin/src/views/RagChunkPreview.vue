<template>
  <div class="chunk-preview">
    <el-page-header @back="$router.push('/rag')" title="返回">
      <template #content>切割预览</template>
    </el-page-header>

    <el-row :gutter="20" style="margin-top:16px">
      <el-col :span="12">
        <el-card header="原始文本">
          <el-input v-model="text" type="textarea" :rows="18" placeholder="粘贴待切割的文本..." />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card header="切割参数">
          <el-form label-width="100px" size="default">
            <el-form-item label="切割策略">
              <el-select v-model="strategy" style="width:100%">
                <el-option label="递归语义切割" value="recursive" />
                <el-option label="固定大小切割" value="fixed" />
                <el-option label="语义切割" value="semantic" />
              </el-select>
            </el-form-item>
            <el-form-item label="chunk大小">
              <el-input-number v-model="chunkSize" :min="64" :max="4096" style="width:100%" />
            </el-form-item>
            <el-form-item label="重叠大小">
              <el-input-number v-model="chunkOverlap" :min="0" :max="512" style="width:100%" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handlePreview" :loading="loading">实时预览</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <el-card v-if="chunks.length > 0" style="margin-top:16px">
      <template #header>
        <span>切割结果：{{ chunks.length }} 个chunk</span>
      </template>
      <div v-for="(chunk, idx) in chunks" :key="idx" class="chunk-block">
        <div class="chunk-header">
          <el-tag type="primary" size="small">Chunk #{{ idx + 1 }}</el-tag>
          <span class="chunk-size">{{ String(chunk).length }} 字符</span>
        </div>
        <div class="chunk-text">{{ chunk }}</div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const text = ref('')
const strategy = ref('recursive')
const chunkSize = ref(512)
const chunkOverlap = ref(50)
const loading = ref(false)
const chunks = ref([])

const handlePreview = async () => {
  if (!text.value) return
  loading.value = true
  try {
    const res = await request.post('/rag/chunk/preview', {
      text: text.value,
      chunkStrategy: strategy.value,
      chunkSize: chunkSize.value,
      chunkOverlap: chunkOverlap.value,
    })
    const data = res.data.data
    chunks.value = data.chunks || []
  } catch (e) {
    ElMessage.error('切割预览失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.chunk-block { background: #f5f7fa; padding: 12px; border-radius: 8px; margin-bottom: 12px; }
.chunk-header { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.chunk-size { font-size: 12px; color: #909399; }
.chunk-text { line-height: 1.8; white-space: pre-wrap; }
</style>
