<template>
  <div class="rag-upload">
    <el-page-header @back="$router.push('/rag')" title="返回">
      <template #content>上传文档</template>
    </el-page-header>

    <el-card style="margin-top:20px;max-width:600px">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
        <el-form-item label="知识库" prop="kbId">
          <el-select v-model="formData.kbId" placeholder="请选择知识库" style="width:100%">
            <el-option v-for="kb in kbList" :key="kb.id" :label="kb.kbName" :value="kb.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="切割策略" prop="chunkStrategy">
          <el-select v-model="formData.chunkStrategy" placeholder="请选择切割策略" style="width:100%">
            <el-option label="固定大小" value="fixed" />
            <el-option label="按文档结构切割" value="recursive" />
            <el-option label="语义分割" value="semantic" />
          </el-select>
          <div style="color:#909399;font-size:12px;margin-top:4px">
            按文档结构切割：根据标题层级（一级标题、二级标题等）自动分割，适合MD等结构化文档
          </div>
        </el-form-item>
        <el-form-item label="块大小">
          <el-input-number v-model="formData.chunkSize" :min="64" :max="2048" :step="64" style="width:100%" />
          <span style="color:#909399;font-size:12px;margin-left:8px">字符 (默认512)</span>
        </el-form-item>
        <el-form-item label="重叠大小">
          <el-input-number v-model="formData.chunkOverlap" :min="0" :max="200" :step="10" style="width:100%" />
          <span style="color:#909399;font-size:12px;margin-left:8px">字符 (默认50)</span>
        </el-form-item>
        <el-form-item label="上传文件">
          <el-upload
            ref="uploadRef"
            drag
            :auto-upload="false"
            :show-file-list="true"
            :limit="1"
            :on-change="handleFileChange"
            :on-exceed="handleExceed"
          >
            <el-icon :size="48"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽文档到此处 或 <em>点击选择文件</em></div>
            <template #tip>
              <div class="el-upload__tip">支持 PDF / DOCX / TXT / MD / HTML</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="uploading" :disabled="!selectedFile">
            确认上传
          </el-button>
          <el-button @click="$router.push('/rag')">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const router = useRouter()
const formRef = ref(null)
const uploadRef = ref(null)
const kbList = ref([])
const selectedFile = ref(null)
const uploading = ref(false)

const formData = reactive({
  kbId: null,
  chunkStrategy: null,
  chunkSize: 512,
  chunkOverlap: 50
})

const rules = {
  kbId: [
    { required: true, message: '请选择知识库', trigger: 'change' }
  ],
  chunkStrategy: [
    { required: true, message: '请选择切割策略', trigger: 'change' }
  ]
}

onMounted(async () => {
  const res = await request.get('/kb/page', { params: { pageNum: 1, pageSize: 100 } })
  kbList.value = res.data.data.records
})

const handleFileChange = (file) => {
  selectedFile.value = file.raw
}

const handleExceed = () => {
  ElMessage.warning('只能上传一个文件，请先删除已选文件')
}

const handleSubmit = async () => {
  // 验证表单
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }

  if (!selectedFile.value) {
    ElMessage.warning('请先选择要上传的文件')
    return
  }

  uploading.value = true
  try {
    const formDataObj = new FormData()
    formDataObj.append('file', selectedFile.value)
    formDataObj.append('kbId', formData.kbId)
    formDataObj.append('chunkStrategy', formData.chunkStrategy)
    formDataObj.append('chunkSize', formData.chunkSize)
    formDataObj.append('chunkOverlap', formData.chunkOverlap)

    await request.post('/rag/upload', formDataObj, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    ElMessage.success('上传成功')
    router.push('/rag')
  } catch (error) {
    ElMessage.error('上传失败: ' + (error.message || '未知错误'))
  } finally {
    uploading.value = false
  }
}
</script>
