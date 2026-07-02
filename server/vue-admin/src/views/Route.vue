<template>
  <div class="route-page">
    <!-- 顶部操作栏 -->
    <el-card>
      <div class="page-header">
        <div>
          <span style="font-size:18px;font-weight:600">游览路线管理</span>
          <span style="margin-left:12px;color:#909399;font-size:13px">管理景区路线和景点信息</span>
        </div>
        <el-button type="primary" @click="openRouteDialog(null)">
          <el-icon><Plus /></el-icon>新建路线
        </el-button>
      </div>
    </el-card>

    <div style="display:flex;gap:16px;margin-top:16px">
      <!-- 左侧：路线列表 -->
      <el-card style="width:360px;flex-shrink:0">
        <template #header>
          <span>路线列表 ({{ routes.length }})</span>
        </template>
        <div v-loading="loading" class="route-list">
          <div v-for="route in routes" :key="route.id"
            :class="['route-item', { active: selectedRoute?.id === route.id }]"
            @click="selectRoute(route)">
            <div class="route-item-header">
              <span class="route-name">{{ route.routeName }}</span>
              <div class="route-actions">
                <el-button size="small" text @click.stop="openRouteDialog(route)">
                  <el-icon><Edit /></el-icon>
                </el-button>
                <el-button size="small" text type="danger" @click.stop="deleteRoute(route.id)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>
            <div class="route-meta">
              <el-tag v-if="route.interestTags" v-for="tag in parseTags(route.interestTags)" :key="tag" size="small" type="info" style="margin:2px">{{ tag }}</el-tag>
              <span v-else style="color:#c0c4cc;font-size:12px">暂无标签</span>
            </div>
            <div class="route-info">
              <span><el-icon><Clock /></el-icon> {{ route.timeBudget || '-' }}分钟</span>
              <span><el-icon><Lightning /></el-icon> {{ energyLabel(route.energyLevel) }}</span>
              <span><el-icon><Location /></el-icon> {{ route.spots?.length || 0 }}个景点</span>
            </div>
          </div>
          <el-empty v-if="!loading && routes.length===0" description="暂无路线" />
        </div>
      </el-card>

      <!-- 右侧：路线详情 + 景点管理 -->
      <el-card style="flex:1" v-if="selectedRoute">
        <template #header>
          <div style="display:flex;justify-content:space-between;align-items:center">
            <span>{{ selectedRoute.routeName }} — 景点管理</span>
            <el-button type="primary" size="small" @click="openSpotDialog(null)">
              <el-icon><Plus /></el-icon>添加景点
            </el-button>
          </div>
        </template>

        <!-- 路线基本信息 -->
        <el-descriptions :column="3" border style="margin-bottom:20px">
          <el-descriptions-item label="路线名称">{{ selectedRoute.routeName }}</el-descriptions-item>
          <el-descriptions-item label="预计时间">{{ selectedRoute.timeBudget }}分钟</el-descriptions-item>
          <el-descriptions-item label="体力要求">{{ energyLabel(selectedRoute.energyLevel) }}</el-descriptions-item>
          <el-descriptions-item label="兴趣标签" :span="3">
            <el-tag v-for="tag in parseTags(selectedRoute.interestTags)" :key="tag" style="margin-right:6px">{{ tag }}</el-tag>
            <span v-if="!selectedRoute.interestTags" style="color:#c0c4cc">未设置</span>
          </el-descriptions-item>
          <el-descriptions-item label="路线描述" :span="3">{{ selectedRoute.description || '未设置' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 景点列表 -->
        <el-table :data="selectedRoute.spots || []" border size="small">
          <el-table-column label="图片" width="80">
            <template #default="{ row }">
              <el-image v-if="row.imageUrl" :src="getFullImageUrl(row.imageUrl)" style="width:50px;height:50px;border-radius:6px" fit="cover" :preview-src-list="[getFullImageUrl(row.imageUrl)]" />
              <div v-else class="spot-img-placeholder"><el-icon><Picture /></el-icon></div>
            </template>
          </el-table-column>
          <el-table-column prop="spotName" label="景点名称" width="140" />
          <el-table-column label="坐标" width="180">
            <template #default="{ row }">
              <span style="font-size:12px">{{ row.latitude }}, {{ row.longitude }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="geoRadius" label="围栏(m)" width="80" />
          <el-table-column prop="narrateText" label="讲解词" show-overflow-tooltip />
          <el-table-column prop="spotOrder" label="顺序" width="70" align="center" />
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="openSpotDialog(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="deleteSpot(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 右侧：未选择提示 -->
      <el-card style="flex:1" v-else>
        <el-empty description="请从左侧选择一条路线">
          <el-button type="primary" @click="openRouteDialog(null)">新建路线</el-button>
        </el-empty>
      </el-card>
    </div>

    <!-- 路线编辑弹窗 -->
    <el-dialog v-model="routeDialogVisible" :title="routeForm.id ? '编辑路线' : '新建路线'" width="600px" destroy-on-close>
      <el-form :model="routeForm" label-width="100px">
        <el-form-item label="路线名称" required>
          <el-input v-model="routeForm.routeName" placeholder="如：灵山大佛深度游" />
        </el-form-item>
        <el-form-item label="兴趣标签">
          <div style="width:100%">
            <div style="margin-bottom:8px">
              <el-tag v-for="tag in routeForm.tagList" :key="tag" closable @close="removeTag(tag)" style="margin-right:6px;margin-bottom:4px">{{ tag }}</el-tag>
            </div>
            <div style="display:flex;gap:8px">
              <el-input v-model="newTag" size="small" placeholder="输入标签" style="width:200px" @keyup.enter="addTag" />
              <el-button size="small" @click="addTag" :disabled="!newTag.trim()">添加</el-button>
              <el-button size="small" @click="addPresetTag" type="info" plain>推荐标签</el-button>
            </div>
            <div v-if="showPresets" style="margin-top:8px">
              <el-tag v-for="pt in presetTags" :key="pt" size="small" type="info" effect="plain"
                :disabled="routeForm.tagList.includes(pt)"
                style="margin-right:6px;margin-bottom:4px;cursor:pointer"
                @click="addPresetTagItem(pt)">{{ pt }}</el-tag>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="预计时间">
          <el-input-number v-model="routeForm.timeBudget" :min="10" :max="480" :step="10" />
          <span style="margin-left:8px;color:#909399">分钟</span>
        </el-form-item>
        <el-form-item label="体力要求">
          <el-radio-group v-model="routeForm.energyLevel">
            <el-radio-button :value="1">轻松</el-radio-button>
            <el-radio-button :value="2">适中</el-radio-button>
            <el-radio-button :value="3">挑战</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="路线描述">
          <el-input v-model="routeForm.description" type="textarea" :rows="3" placeholder="简要描述这条路线的特色" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="routeDialogVisible=false">取消</el-button>
        <el-button type="primary" @click="saveRoute" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 景点编辑弹窗 -->
    <el-dialog v-model="spotDialogVisible" :title="spotForm.id ? '编辑景点' : '添加景点'" width="650px" destroy-on-close>
      <el-form :model="spotForm" label-width="100px">
        <el-form-item label="景点名称" required>
          <el-input v-model="spotForm.spotName" placeholder="如：灵山大佛" />
        </el-form-item>
        <el-form-item label="景点图片">
          <div style="display:flex;gap:16px;align-items:flex-start">
            <el-upload
              action=""
              :http-request="uploadSpotImage"
              :show-file-list="false"
              accept="image/*"
              style="width:120px">
              <div class="image-upload-area">
                <img v-if="spotImageUrl" :src="spotImageUrl" style="width:120px;height:120px;border-radius:8px;object-fit:cover" />
                <div v-else class="image-upload-placeholder">
                  <el-icon :size="24"><Plus /></el-icon>
                  <span style="font-size:12px;margin-top:4px">上传图片</span>
                </div>
              </div>
            </el-upload>
            <div style="color:#909399;font-size:12px;line-height:1.8">
              <p>支持 JPG/PNG 格式</p>
              <p>建议尺寸 800x600</p>
              <p>文件大小 ≤ 5MB</p>
            </div>
          </div>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="纬度">
              <el-input-number v-model="spotForm.latitude" :precision="7" :step="0.0001" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="经度">
              <el-input-number v-model="spotForm.longitude" :precision="7" :step="0.0001" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="围栏半径">
              <el-input-number v-model="spotForm.geoRadius" :min="10" :max="1000" :step="10" style="width:100%" />
              <span style="font-size:12px;color:#909399">米（进入此范围触发讲解）</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="游览顺序">
              <el-input-number v-model="spotForm.spotOrder" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="讲解词">
          <el-input v-model="spotForm.narrateText" type="textarea" :rows="5" placeholder="当游客进入围栏范围时，数字人将播报此讲解词" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="spotDialogVisible=false">取消</el-button>
        <el-button type="primary" @click="saveSpot" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Clock, Lightning, Location, Picture } from '@element-plus/icons-vue'

const routes = ref([])
const loading = ref(false)
const saving = ref(false)
const selectedRoute = ref(null)

// 路线弹窗
const routeDialogVisible = ref(false)
const routeForm = reactive({ id: null, routeName: '', tagList: [], timeBudget: 120, energyLevel: 1, description: '' })
const newTag = ref('')
const showPresets = ref(false)
const presetTags = ['历史文化', '自然风光', '美食体验', '拍照打卡', '亲子互动', '祈福朝拜', '艺术欣赏', '夜景观赏']

// 景点弹窗
const spotDialogVisible = ref(false)
const spotForm = reactive({ id: null, spotName: '', latitude: 0, longitude: 0, geoRadius: 100, narrateText: '', spotOrder: 0, imageUrl: '' })
const spotImageUrl = ref('')

// MinIO地址前缀
const MINIO_BASE_URL = 'http://localhost:9000/virtual-knowledge/'

// 获取完整的图片URL
const getFullImageUrl = (imageUrl) => {
  if (!imageUrl) return ''
  if (imageUrl.startsWith('http')) return imageUrl
  return MINIO_BASE_URL + imageUrl
}

// 解析标签
const parseTags = (tags) => {
  if (!tags) return []
  try { return JSON.parse(tags) } catch { return tags.split(',').map(t => t.trim()).filter(Boolean) }
}

const energyLabel = (level) => ({ 1: '轻松', 2: '适中', 3: '挑战' }[level] || '-')

// 加载数据
const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/route/page', { params: { pageNum: 1, pageSize: 50 } })
    const list = res.data.data.records
    await Promise.all(list.map(async (route) => {
      try {
        const sres = await request.get(`/route/${route.id}/spot/page`, { params: { pageNum: 1, pageSize: 100 } })
        route.spots = (sres.data.data.records || []).sort((a, b) => a.spotOrder - b.spotOrder)
      } catch { route.spots = [] }
    }))
    routes.value = list
    // 保持选中状态
    if (selectedRoute.value) {
      const updated = list.find(r => r.id === selectedRoute.value.id)
      if (updated) selectedRoute.value = updated
    }
  } finally { loading.value = false }
}

const selectRoute = (route) => { selectedRoute.value = route }

// ===== 路线操作 =====
const openRouteDialog = (route) => {
  if (route) {
    routeForm.id = route.id
    routeForm.routeName = route.routeName
    routeForm.tagList = parseTags(route.interestTags)
    routeForm.timeBudget = route.timeBudget || 120
    routeForm.energyLevel = route.energyLevel || 1
    routeForm.description = route.description || ''
  } else {
    routeForm.id = null
    routeForm.routeName = ''
    routeForm.tagList = []
    routeForm.timeBudget = 120
    routeForm.energyLevel = 1
    routeForm.description = ''
  }
  newTag.value = ''
  showPresets.value = false
  routeDialogVisible.value = true
}

const addTag = () => {
  const tag = newTag.value.trim()
  if (tag && !routeForm.tagList.includes(tag)) {
    routeForm.tagList.push(tag)
    newTag.value = ''
  }
}
const removeTag = (tag) => { routeForm.tagList = routeForm.tagList.filter(t => t !== tag) }
const addPresetTag = () => { showPresets.value = !showPresets.value }
const addPresetTagItem = (tag) => {
  if (!routeForm.tagList.includes(tag)) routeForm.tagList.push(tag)
}

const saveRoute = async () => {
  if (!routeForm.routeName.trim()) { ElMessage.warning('请输入路线名称'); return }
  saving.value = true
  try {
    const payload = {
      routeName: routeForm.routeName,
      interestTags: JSON.stringify(routeForm.tagList),
      timeBudget: routeForm.timeBudget,
      energyLevel: routeForm.energyLevel,
      description: routeForm.description,
    }
    if (routeForm.id) {
      await request.put(`/route/${routeForm.id}`, payload)
    } else {
      await request.post('/route', payload)
    }
    ElMessage.success('保存成功')
    routeDialogVisible.value = false
    await fetchData()
  } finally { saving.value = false }
}

const deleteRoute = async (id) => {
  await ElMessageBox.confirm('删除路线将同时删除所有景点，确定？', '警告', { type: 'warning' })
  await request.delete(`/route/${id}`)
  ElMessage.success('删除成功')
  if (selectedRoute.value?.id === id) selectedRoute.value = null
  fetchData()
}

// ===== 景点操作 =====
const openSpotDialog = (spot) => {
  if (spot) {
    spotForm.id = spot.id
    spotForm.spotName = spot.spotName
    spotForm.latitude = spot.latitude || 0
    spotForm.longitude = spot.longitude || 0
    spotForm.geoRadius = spot.geoRadius || 100
    spotForm.narrateText = spot.narrateText || ''
    spotForm.spotOrder = spot.spotOrder || 0
    spotForm.imageUrl = spot.imageUrl || ''
    spotImageUrl.value = getFullImageUrl(spot.imageUrl) || ''
  } else {
    spotForm.id = null
    spotForm.spotName = ''
    spotForm.latitude = 0
    spotForm.longitude = 0
    spotForm.geoRadius = 100
    spotForm.narrateText = ''
    spotForm.spotOrder = (selectedRoute.value?.spots?.length || 0) + 1
    spotForm.imageUrl = ''
    spotImageUrl.value = ''
  }
  spotDialogVisible.value = true
}

const uploadSpotImage = async (options) => {
  const formData = new FormData()
  formData.append('file', options.file)
  const res = await request.post('/route/spot/upload-image', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
  spotForm.imageUrl = res.data.data.path
  spotImageUrl.value = res.data.data.displayUrl || getFullImageUrl(res.data.data.path)
  ElMessage.success('图片上传成功')
}

const saveSpot = async () => {
  if (!spotForm.spotName.trim()) { ElMessage.warning('请输入景点名称'); return }
  saving.value = true
  try {
    const payload = { ...spotForm }
    delete payload.id
    if (spotForm.id) {
      await request.put(`/route/${selectedRoute.value.id}/spot/${spotForm.id}`, payload)
    } else {
      await request.post(`/route/${selectedRoute.value.id}/spot`, payload)
    }
    ElMessage.success('保存成功')
    spotDialogVisible.value = false
    await fetchData()
  } finally { saving.value = false }
}

const deleteSpot = async (spotId) => {
  await ElMessageBox.confirm('确定删除此景点？', '提示', { type: 'warning' })
  await request.delete(`/route/${selectedRoute.value.id}/spot/${spotId}`)
  ElMessage.success('删除成功')
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.page-header { display: flex; align-items: center; justify-content: space-between; }
.route-list { max-height: calc(100vh - 280px); overflow-y: auto; }
.route-item {
  padding: 14px; margin-bottom: 10px; border-radius: 10px;
  border: 1px solid var(--border-primary); cursor: pointer; transition: all .2s;
  background: var(--bg-card);
}
.route-item:hover { border-color: var(--accent-primary); background: var(--bg-hover); }
.route-item.active { border-color: var(--accent-primary); background: var(--bg-active); box-shadow: 0 2px 8px rgba(102,126,234,0.15); }
.route-item-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.route-name { font-weight: 600; font-size: 15px; color: var(--text-primary); }
.route-meta { margin-bottom: 8px; min-height: 24px; }
.route-info { display: flex; gap: 16px; font-size: 12px; color: var(--text-muted); }
.route-info span { display: flex; align-items: center; gap: 4px; }
.route-actions { display: flex; gap: 4px; }
.spot-img-placeholder {
  width: 50px; height: 50px; border-radius: 6px; background: var(--border-primary);
  display: flex; align-items: center; justify-content: center; color: var(--text-muted);
}
.image-upload-area {
  width: 120px; height: 120px; border: 1px dashed var(--border-input); border-radius: 8px;
  display: flex; align-items: center; justify-content: center; cursor: pointer; overflow: hidden;
}
.image-upload-area:hover { border-color: var(--accent-primary); }
.image-upload-placeholder {
  display: flex; flex-direction: column; align-items: center; color: var(--text-muted);
}
</style>
