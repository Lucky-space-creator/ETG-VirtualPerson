<template>
  <div class="avatar-edit">
    <el-page-header @back="$router.push('/avatar')" title="返回">
      <template #content>{{ isEdit ? '编辑形象' : '新增形象' }}</template>
    </el-page-header>

    <div style="display:flex;gap:20px;margin-top:20px">
      <!-- 左侧：表单 -->
      <el-card style="flex:1;max-width:600px">
        <el-form ref="formRef" :model="form" label-width="100px">
        <el-divider content-position="left">基本信息</el-divider>
        <el-form-item label="名称" required><el-input v-model="form.avatarName" /></el-form-item>
        <el-form-item label="角色设定">
          <el-input v-model="form.persona" type="textarea" :rows="4" placeholder="System Prompt" />
        </el-form-item>
        <el-form-item label="性格描述">
          <el-input v-model="form.personality" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>

        <el-divider content-position="left">语音设置</el-divider>
        <el-form-item label="语音类型">
          <el-select v-model="form.voiceType" style="width:100%">
            <el-option label="晓晓 (女声，温柔)" value="zh-CN-XiaoxiaoNeural" />
            <el-option label="晓伊 (女声，活泼)" value="zh-CN-XiaoyiNeural" />
            <el-option label="云希 (男声，阳光)" value="zh-CN-YunxiNeural" />
            <el-option label="云健 (男声，沉稳)" value="zh-CN-YunjianNeural" />
            <el-option label="晓辰 (女声，知性)" value="zh-CN-XiaochenNeural" />
            <el-option label="云泽 (男声，磁性)" value="zh-CN-YunzeNeural" />
          </el-select>
        </el-form-item>
        <el-form-item label="说话速度">
          <el-slider v-model="form.speechRate" :min="0.5" :max="2.0" :step="0.1" show-input />
          <span style="font-size:12px;color:#909399">1.0为正常速度</span>
        </el-form-item>
        <el-form-item label="情感配置">
          <el-input v-model="form.emotionConfig" type="textarea" :rows="2" placeholder='{"happy":"happy","sad":"sad","default":"neutral"}' />
        </el-form-item>

        <el-divider content-position="left">外观设置</el-divider>
        <el-form-item label="VRM模型">
          <el-upload action="" :http-request="uploadVrm" :show-file-list="false" accept=".vrm">
            <el-button type="primary" plain>选择VRM模型文件</el-button>
          </el-upload>
          <div v-if="form.vrmModelUrl" style="margin-top:8px">
            <el-tag type="success" closable @close="form.vrmModelUrl = ''">{{ form.vrmModelUrl.split('/').pop() }}</el-tag>
          </div>
        </el-form-item>
        <el-form-item label="缩略图">
          <el-upload action="" :http-request="uploadThumbnail" :show-file-list="false" accept="image/*">
            <el-button>上传缩略图</el-button>
          </el-upload>
          <img v-if="thumbnailDisplayUrl" :src="thumbnailDisplayUrl" style="width:80px;height:80px;margin-top:8px;border-radius:4px" />
        </el-form-item>
        <el-form-item label="背景图片">
          <el-upload action="" :http-request="uploadBackground" :show-file-list="false" accept="image/*">
            <el-button>上传背景图片</el-button>
          </el-upload>
          <img v-if="backgroundDisplayUrl" :src="backgroundDisplayUrl" style="width:160px;height:90px;margin-top:8px;border-radius:8px;object-fit:cover" />
        </el-form-item>

        <el-divider content-position="left">服装管理</el-divider>
        <el-form-item label="衣服列表">
          <div style="width:100%">
            <div v-for="clothes in clothesList" :key="clothes.id" class="clothes-item">
              <div class="clothes-info">
                <img v-if="clothes.thumbnailDisplayUrl" :src="clothes.thumbnailDisplayUrl" class="clothes-thumb" />
                <div v-else class="clothes-thumb-placeholder">
                  <el-icon><UserFilled /></el-icon>
                </div>
                <div class="clothes-detail">
                  <div class="clothes-name">
                    {{ clothes.clothesName }}
                    <el-tag v-if="clothes.isDefault" type="success" size="small">默认</el-tag>
                  </div>
                  <div class="clothes-desc">{{ clothes.description || '暂无描述' }}</div>
                </div>
              </div>
              <div class="clothes-actions">
                <el-button size="small" @click="setDefaultClothes(clothes.id)" :disabled="clothes.isDefault">设为默认</el-button>
                <el-button size="small" type="danger" @click="deleteClothes(clothes.id)">删除</el-button>
              </div>
            </div>
            <div v-if="clothesList.length === 0" style="text-align:center;color:#909399;padding:20px">
              暂无衣服，请点击下方按钮添加
            </div>
            <el-button type="primary" plain style="width:100%;margin-top:12px" @click="showClothesDialog = true">
              <el-icon><Plus /></el-icon> 添加衣服
            </el-button>
          </div>
        </el-form-item>

        <el-divider content-position="left">动作设置</el-divider>
        <el-form-item label="手臂角度">
          <el-slider v-model="form.armAngle" :min="0" :max="90" :step="5" show-input />
          <span style="font-size:12px;color:#909399">度（0=贴身，90=水平）</span>
        </el-form-item>
        <el-form-item label="待机动作">
          <el-select v-model="form.idleAnimation" style="width:100%">
            <el-option label="呼吸+眨眼" value="idle" />
            <el-option label="轻微摇摆" value="sway" />
            <el-option label="静态站立" value="static" />
          </el-select>
        </el-form-item>
        <el-form-item label="说话动作">
          <el-select v-model="form.talkAnimation" style="width:100%">
            <el-option label="自然手势" value="natural" />
            <el-option label="简单点头" value="nod" />
            <el-option label="无动作" value="none" />
          </el-select>
        </el-form-item>
        <el-form-item label="动作幅度">
          <el-slider v-model="form.animationIntensity" :min="0.1" :max="1.0" :step="0.1" show-input />
          <span style="font-size:12px;color:#909399">1.0为最大幅度</span>
        </el-form-item>

        <el-divider content-position="left">保存</el-divider>
        <el-form-item>
          <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
          <el-button @click="$router.push('/avatar')">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>

      <!-- 右侧：VRM模型预览 -->
      <el-card style="width:600px;flex-shrink:0">
        <template #header>
          <div style="display:flex;justify-content:space-between;align-items:center">
            <span>模型预览</span>
            <el-button size="small" @click="refreshPreview" :loading="previewLoading">刷新预览</el-button>
          </div>
        </template>

        <!-- VRM 3D预览 -->
        <div class="vrm-preview-container">
          <iframe
            v-if="vrmPreviewUrl"
            :src="vrmPreviewUrl"
            class="vrm-preview-iframe"
            frameborder="0"
          ></iframe>
          <div v-else class="vrm-preview-placeholder">
            <el-icon :size="64"><UserFilled /></el-icon>
            <p>请先上传VRM模型</p>
            <p class="hint">支持 .vrm 格式的3D模型文件</p>
          </div>
        </div>

        <!-- 动作测试按钮 -->
        <div class="action-section">
          <h4>动作测试</h4>
          <div class="action-buttons">
            <el-button size="small" @click="testAction('nod')">点头</el-button>
            <el-button size="small" @click="testAction('wave')">招手</el-button>
            <el-button size="small" @click="testAction('bow')">鞠躬</el-button>
            <el-button size="small" @click="testAction('happy')">开心</el-button>
            <el-button size="small" @click="testAction('sad')">悲伤</el-button>
            <el-button size="small" @click="testAction('surprised')">惊讶</el-button>
          </div>
        </div>

        <!-- 形象配置信息 -->
        <div class="config-section">
          <h4>配置信息</h4>
          <div class="config-info">
            <div class="config-row">
              <span class="config-label">名称:</span>
              <span class="config-value">{{ form.avatarName || '未设置' }}</span>
            </div>
            <div class="config-row">
              <span class="config-label">语音:</span>
              <span class="config-value">{{ voiceLabel }}</span>
            </div>
            <div class="config-row">
              <span class="config-label">衣服:</span>
              <span class="config-value">
                <span class="color-dot" :style="{ background: form.clothesColor }"></span>
                {{ clothesLabel }}
              </span>
            </div>
            <div class="config-row">
              <span class="config-label">发型:</span>
              <span class="config-value">
                <span class="color-dot" :style="{ background: form.hairColor }"></span>
                {{ hairLabel }}
              </span>
            </div>
            <div class="config-row">
              <span class="config-label">手臂角度:</span>
              <span class="config-value">{{ form.armAngle }}度</span>
            </div>
            <div class="config-row">
              <span class="config-label">动作幅度:</span>
              <span class="config-value">{{ (form.animationIntensity * 100).toFixed(0) }}%</span>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 添加衣服弹窗 -->
    <el-dialog v-model="showClothesDialog" title="添加衣服" width="500px">
      <el-form :model="clothesForm" label-width="80px">
        <el-form-item label="衣服名称" required>
          <el-input v-model="clothesForm.clothesName" placeholder="如：休闲装、正装" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="clothesForm.description" type="textarea" :rows="2" placeholder="衣服描述" />
        </el-form-item>
        <el-form-item label="VRM模型" required>
          <el-upload action="" :http-request="uploadClothesVrm" :show-file-list="false" accept=".vrm">
            <el-button type="primary" plain>选择VRM文件</el-button>
          </el-upload>
          <div v-if="clothesForm.vrmModelUrl" style="margin-top:8px">
            <el-tag type="success">{{ clothesForm.vrmModelUrl.split('/').pop() }}</el-tag>
          </div>
        </el-form-item>
        <el-form-item label="缩略图">
          <el-upload action="" :http-request="uploadClothesThumbnail" :show-file-list="false" accept="image/*">
            <el-button>上传缩略图</el-button>
          </el-upload>
          <img v-if="clothesThumbnailUrl" :src="clothesThumbnailUrl" style="width:80px;height:80px;margin-top:8px;border-radius:4px" />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="clothesForm.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showClothesDialog = false">取消</el-button>
        <el-button type="primary" @click="saveClothes" :loading="savingClothes">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { UserFilled, Picture } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const isEdit = ref(!!route.params.id)
const saving = ref(false)
const previewLoading = ref(false)
const thumbnailDisplayUrl = ref('')
const backgroundDisplayUrl = ref('')

// 衣服管理
const clothesList = ref([])
const showClothesDialog = ref(false)
const savingClothes = ref(false)
const clothesThumbnailUrl = ref('')
const clothesForm = reactive({
  clothesName: '',
  description: '',
  vrmModelUrl: '',
  thumbnailUrl: '',
  isDefault: 0,
  sortOrder: 0
})

// 计算属性：VRM预览URL
const vrmPreviewUrl = computed(() => {
  if (!form.vrmModelUrl) return null
  // 构建完整的模型URL
  const minioBase = 'http://localhost:9000/virtual-knowledge/'
  const fullModelUrl = form.vrmModelUrl.startsWith('http')
    ? form.vrmModelUrl
    : minioBase + form.vrmModelUrl
  // 构建VRM预览页面URL
  const previewBase = 'http://localhost:5173'
  return `${previewBase}/vrm-preview.html?model=${encodeURIComponent(fullModelUrl)}`
})

// 计算属性：语音标签
const voiceLabel = computed(() => {
  const voices = {
    'zh-CN-XiaoxiaoNeural': '晓晓 (女声，温柔)',
    'zh-CN-XiaoyiNeural': '晓伊 (女声，活泼)',
    'zh-CN-YunxiNeural': '云希 (男声，阳光)',
    'zh-CN-YunjianNeural': '云健 (男声，沉稳)',
    'zh-CN-XiaochenNeural': '晓辰 (女声，知性)',
    'zh-CN-YunzeNeural': '云泽 (男声，磁性)',
  }
  return voices[form.voiceType] || '未设置'
})

// 计算属性：衣服标签
const clothesLabel = computed(() => {
  const styles = {
    'casual': '休闲装',
    'formal': '正装',
    'traditional': '传统服饰',
    'sporty': '运动装',
  }
  return styles[form.clothesStyle] || '未设置'
})

// 计算属性：发型标签
const hairLabel = computed(() => {
  const styles = {
    'long': '长发',
    'short': '短发',
    'ponytail': '马尾',
    'bun': '丸子头',
  }
  return styles[form.hairStyle] || '未设置'
})

// 刷新预览
const refreshPreview = () => {
  previewLoading.value = true
  setTimeout(() => {
    previewLoading.value = false
    ElMessage.success('预览已刷新')
  }, 500)
}

// 测试动作 - 发送到预览iframe
const testAction = (action) => {
  const iframe = document.querySelector('.vrm-preview-iframe')
  if (iframe && iframe.contentWindow) {
    iframe.contentWindow.postMessage({ type: 'playAction', action: action }, '*')
    ElMessage.success(`播放动作: ${action}`)
  } else {
    ElMessage.warning('模型预览未加载')
  }
}

// 衣服管理函数
const loadClothes = async () => {
  if (!isEdit.value) return
  try {
    const res = await request.get(`/avatar/clothes/list/${route.params.id}`)
    clothesList.value = res.data.data || []
  } catch (e) {
    console.error('加载衣服列表失败:', e)
  }
}

const uploadClothesVrm = async (options) => {
  const formData = new FormData()
  formData.append('file', options.file)
  const res = await request.post('/avatar/clothes/upload/vrm', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
  clothesForm.vrmModelUrl = res.data.data.path
  ElMessage.success('VRM模型上传成功')
}

const uploadClothesThumbnail = async (options) => {
  const formData = new FormData()
  formData.append('file', options.file)
  const res = await request.post('/avatar/clothes/upload/thumbnail', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
  clothesForm.thumbnailUrl = res.data.data.path
  clothesThumbnailUrl.value = res.data.data.displayUrl
  ElMessage.success('缩略图上传成功')
}

const saveClothes = async () => {
  if (!clothesForm.clothesName.trim()) {
    ElMessage.warning('请输入衣服名称')
    return
  }
  if (!clothesForm.vrmModelUrl) {
    ElMessage.warning('请上传VRM模型')
    return
  }
  savingClothes.value = true
  try {
    await request.post('/avatar/clothes', {
      avatarId: Number(route.params.id),
      clothesName: clothesForm.clothesName,
      description: clothesForm.description,
      vrmModelUrl: clothesForm.vrmModelUrl,
      thumbnailUrl: clothesForm.thumbnailUrl,
      isDefault: clothesForm.isDefault,
      sortOrder: clothesForm.sortOrder
    })
    ElMessage.success('衣服添加成功')
    showClothesDialog.value = false
    // 重置表单
    clothesForm.clothesName = ''
    clothesForm.description = ''
    clothesForm.vrmModelUrl = ''
    clothesForm.thumbnailUrl = ''
    clothesForm.isDefault = 0
    clothesThumbnailUrl.value = ''
    // 重新加载衣服列表
    await loadClothes()
  } catch (e) {
    ElMessage.error('添加失败: ' + (e.response?.data?.message || e.message))
  } finally {
    savingClothes.value = false
  }
}

const deleteClothes = async (id) => {
  try {
    await request.delete(`/avatar/clothes/${id}`)
    ElMessage.success('删除成功')
    await loadClothes()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

const setDefaultClothes = async (id) => {
  try {
    await request.put(`/avatar/clothes/${id}/default`)
    ElMessage.success('设置成功')
    await loadClothes()
  } catch (e) {
    ElMessage.error('设置失败')
  }
}

const form = reactive({
  avatarName: '', persona: '', personality: '', voiceType: '', sortOrder: 0,
  vrmModelUrl: '', thumbnailUrl: '', backgroundUrl: '', emotionConfig: '',
  speechRate: 1.0, armAngle: 50, idleAnimation: 'idle', talkAnimation: 'natural', animationIntensity: 0.7,
  clothesStyle: 'casual', clothesColor: '#4A90D9', hairStyle: 'long', hairColor: '#2C1810',
})

onMounted(async () => {
  if (isEdit.value) {
    const res = await request.get(`/avatar/${route.params.id}`)
    const data = res.data.data
    // 填充表单
    form.avatarName = data.avatarName || ''
    form.persona = data.persona || ''
    form.personality = data.personality || ''
    form.voiceType = data.voiceType || ''
    form.sortOrder = data.sortOrder || 0
    form.vrmModelUrl = data.vrmModelUrl || ''
    form.thumbnailUrl = data.thumbnailUrl || ''
    form.backgroundUrl = data.backgroundUrl || ''
    form.emotionConfig = data.emotionConfig || ''
    // 动作配置
    form.speechRate = data.speechRate || 1.0
    form.armAngle = data.armAngle || 50
    form.idleAnimation = data.idleAnimation || 'idle'
    form.talkAnimation = data.talkAnimation || 'natural'
    form.animationIntensity = data.animationIntensity || 0.7
    // 服装发型配置
    form.clothesStyle = data.clothesStyle || 'casual'
    form.clothesColor = data.clothesColor || '#4A90D9'
    form.hairStyle = data.hairStyle || 'long'
    form.hairColor = data.hairColor || '#2C1810'
    // 显示用presigned URL
    thumbnailDisplayUrl.value = data.thumbnailDisplayUrl || ''
    backgroundDisplayUrl.value = data.backgroundDisplayUrl || ''
    // 加载衣服列表
    await loadClothes()
  }
})

const uploadVrm = async (options) => {
  const formData = new FormData()
  formData.append('file', options.file)
  const res = await request.post('/avatar/upload/vrm', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
  const data = res.data.data
  form.vrmModelUrl = data.path  // 保存原始MinIO路径
  ElMessage.success('上传成功')
}

const uploadThumbnail = async (options) => {
  const formData = new FormData()
  formData.append('file', options.file)
  const res = await request.post('/avatar/upload/thumbnail', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
  const data = res.data.data
  form.thumbnailUrl = data.path  // 保存原始MinIO路径
  thumbnailDisplayUrl.value = data.displayUrl  // 显示presigned URL
  ElMessage.success('上传成功')
}

const uploadBackground = async (options) => {
  const formData = new FormData()
  formData.append('file', options.file)
  const res = await request.post('/avatar/upload/background', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
  const data = res.data.data
  form.backgroundUrl = data.path  // 保存原始MinIO路径
  backgroundDisplayUrl.value = data.displayUrl  // 显示presigned URL
  ElMessage.success('上传成功')
}

const handleSave = async () => {
  if (!form.avatarName.trim()) {
    ElMessage.warning('请输入名称')
    return
  }
  saving.value = true
  try {
    const payload = {
      avatarName: form.avatarName,
      persona: form.persona,
      personality: form.personality,
      voiceType: form.voiceType,
      sortOrder: form.sortOrder,
      vrmModelUrl: form.vrmModelUrl,
      thumbnailUrl: form.thumbnailUrl,
      backgroundUrl: form.backgroundUrl,
      emotionConfig: form.emotionConfig,
      // 动作配置
      speechRate: form.speechRate,
      armAngle: form.armAngle,
      idleAnimation: form.idleAnimation,
      talkAnimation: form.talkAnimation,
      animationIntensity: form.animationIntensity,
      // 服装发型配置
      clothesStyle: form.clothesStyle,
      clothesColor: form.clothesColor,
      hairStyle: form.hairStyle,
      hairColor: form.hairColor,
    }
    if (isEdit.value) {
      await request.put(`/avatar/${route.params.id}`, payload)
    } else {
      await request.post('/avatar', payload)
    }
    ElMessage.success('保存成功')
    router.push('/avatar')
  } finally { saving.value = false }
}
</script>

<style scoped>
.avatar-edit {
  padding: 0;
}

.clothes-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  margin-bottom: 8px;
  border: 1px solid var(--border-primary);
  border-radius: 8px;
  background: var(--bg-card);
}

.clothes-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.clothes-thumb {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  object-fit: cover;
}

.clothes-thumb-placeholder {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  background: var(--bg-hover);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
}

.clothes-detail {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.clothes-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
}

.clothes-desc {
  font-size: 12px;
  color: var(--text-muted);
}

.clothes-actions {
  display: flex;
  gap: 8px;
}

.vrm-preview-container {
  width: 100%;
  height: 700px;
  border-radius: 12px;
  overflow: hidden;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  margin-bottom: 16px;
}

.vrm-preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
}

.vrm-preview-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
}

.vrm-preview-placeholder p {
  margin: 8px 0 0 0;
  font-size: 14px;
}

.vrm-preview-placeholder .hint {
  font-size: 12px;
  color: #64748b;
  margin-top: 4px;
}

.action-section {
  margin-bottom: 16px;
}

.action-section h4 {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 12px 0;
}

.action-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.config-section {
  margin-top: 16px;
}

.config-section h4 {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 12px 0;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border-primary);
}

.config-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.config-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.config-label {
  font-size: 13px;
  color: var(--text-muted);
}

.config-value {
  font-size: 13px;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 6px;
}

.color-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 1px solid var(--border-primary);
}
</style>
