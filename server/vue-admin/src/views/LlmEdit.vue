<template>
  <div class="llm-edit">
    <el-page-header @back="$router.push('/llm')" title="返回">
      <template #content>{{ isEdit ? '编辑配置' : '新增配置' }}</template>
    </el-page-header>

    <el-card style="margin-top:20px;max-width:700px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="配置名称" prop="configName">
          <el-input v-model="form.configName" />
        </el-form-item>
        <el-form-item label="提供商" prop="provider">
          <el-select v-model="form.provider" style="width:100%">
            <el-option label="OpenAI" value="OpenAI" />
            <el-option label="Ollama" value="Ollama" />
            <el-option label="智谱AI" value="ZhipuAI" />
            <el-option label="Qwen-VL" value="Qwen-VL-Chat" />
            <el-option label="GLM-4V" value="GLM-4V" />
          </el-select>
        </el-form-item>
        <el-form-item label="API地址" prop="apiUrl">
          <el-input v-model="form.apiUrl" />
        </el-form-item>
        <el-form-item label="API Key" prop="apiKey">
          <el-input v-model="form.apiKey" type="password" show-password />
        </el-form-item>
        <el-form-item label="模型名称" prop="modelName">
          <el-input v-model="form.modelName" />
        </el-form-item>
        <el-form-item label="温度" prop="temperature">
          <el-slider v-model="form.temperature" :min="0" :max="2" :step="0.1" show-input />
        </el-form-item>
        <el-form-item label="最大Token" prop="maxTokens">
          <el-input-number v-model="form.maxTokens" :min="1" :max="8192" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
          <el-button @click="$router.push('/llm')">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const isEdit = ref(!!route.params.id)
const saving = ref(false)

const form = reactive({
  configName: '',
  provider: 'OpenAI',
  apiUrl: '',
  apiKey: '',
  modelName: '',
  temperature: 0.7,
  maxTokens: 2048,
})

const rules = {
  configName: [{ required: true, message: '请输入配置名称' }],
  provider: [{ required: true, message: '请选择提供商' }],
}

onMounted(async () => {
  if (isEdit.value) {
    const res = await request.get(`/llm/${route.params.id}`)
    Object.assign(form, res.data.data)
  }
})

const handleSave = async () => {
  saving.value = true
  try {
    if (isEdit.value) {
      await request.put(`/llm/${route.params.id}`, form)
    } else {
      await request.post('/llm', form)
    }
    ElMessage.success('保存成功')
    router.push('/llm')
  } finally {
    saving.value = false
  }
}
</script>
