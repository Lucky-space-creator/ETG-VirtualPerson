<template>
  <div class="tourist-import">
    <!-- 顶部标题 -->
    <el-card>
      <div class="page-header">
        <div>
          <span class="page-title">游客消费数据导入</span>
          <span class="page-desc">支持Excel格式的消费数据批量导入</span>
        </div>
        <el-button @click="$router.push('/dashboard')">返回仪表盘</el-button>
      </div>
    </el-card>

    <!-- 导入区域 -->
    <el-card style="margin-top:16px">
      <template #header>
        <span>数据导入</span>
      </template>

      <el-form label-width="100px">
        <el-form-item label="选择景区">
          <el-select v-model="scenicSpot" placeholder="请选择景区" style="width:100%">
            <el-option v-for="spot in spots" :key="spot.id" :label="spot.name" :value="spot.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="Excel文件">
          <el-upload
            ref="uploadRef"
            drag
            :auto-upload="false"
            :show-file-list="true"
            :limit="1"
            :on-change="handleFileChange"
            :on-exceed="handleExceed"
            accept=".xlsx,.xls"
          >
            <el-icon :size="48"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽Excel文件到此处 或 <em>点击选择</em></div>
            <template #tip>
              <div class="el-upload__tip">
                支持 .xlsx / .xls 格式，列名需包含：tourist_id, age, gender, attraction_name, total_cost, satisfaction 等
              </div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleImport" :loading="importing" :disabled="!selectedFile">
            开始导入
          </el-button>
          <el-button @click="handleClear" :disabled="scenicSpot === 'all'">
            清空当前景区数据
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 导入结果 -->
    <el-card v-if="importResult" style="margin-top:16px">
      <template #header>
        <span>导入结果</span>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="总行数">{{ importResult.totalRows }}</el-descriptions-item>
        <el-descriptions-item label="成功数">
          <el-tag type="success">{{ importResult.successCount }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="失败数">
          <el-tag :type="importResult.failCount > 0 ? 'danger' : 'info'">{{ importResult.failCount }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag type="success">导入完成</el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <div v-if="importResult.errors && importResult.errors.length > 0" style="margin-top:16px">
        <h4>错误详情：</h4>
        <ul class="error-list">
          <li v-for="(err, index) in importResult.errors" :key="index">{{ err }}</li>
        </ul>
      </div>
    </el-card>

    <!-- 数据预览 -->
    <el-card style="margin-top:16px">
      <template #header>
        <div class="card-header">
          <span>已导入数据预览</span>
          <el-select v-model="previewSpot" size="small" style="width:150px" @change="loadPreview">
            <el-option label="全部景区" value="all" />
            <el-option v-for="spot in spots" :key="spot.id" :label="spot.name" :value="spot.id" />
          </el-select>
        </div>
      </template>

      <el-table :data="previewData" stripe style="width:100%" max-height="400">
        <el-table-column prop="touristId" label="游客ID" width="100" />
        <el-table-column prop="userNickname" label="昵称" width="100" />
        <el-table-column prop="age" label="年龄" width="60" />
        <el-table-column prop="gender" label="性别" width="60" />
        <el-table-column prop="attractionName" label="景点" width="120" show-overflow-tooltip />
        <el-table-column prop="visitDate" label="访问日期" width="100" />
        <el-table-column prop="totalCost" label="总消费" width="100">
          <template #default="{ row }">¥{{ row.totalCost || 0 }}</template>
        </el-table-column>
        <el-table-column prop="satisfaction" label="满意度" width="80">
          <template #default="{ row }">
            <el-rate v-model="row.satisfaction" disabled :max="5" size="small" />
          </template>
        </el-table-column>
        <el-table-column prop="scenicSpot" label="景区" width="100" />
      </el-table>

      <el-pagination
        v-if="previewTotal > 0"
        style="margin-top:16px;justify-content:center"
        layout="total, prev, pager, next"
        :total="previewTotal"
        :page-size="pageSize"
        v-model:current-page="currentPage"
        @current-change="loadPreview"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'

const spots = ref([
  { id: 'lingshan', name: '灵山胜境' },
  { id: 'zhoubian', name: '周庄古镇' },
  { id: 'xihu', name: '西湖风景区' },
  { id: 'gugong', name: '故宫博物院' },
  { id: 'default', name: '通用知识库' }
])

const scenicSpot = ref('lingshan')
const selectedFile = ref(null)
const importing = ref(false)
const importResult = ref(null)

const previewSpot = ref('all')
const previewData = ref([])
const previewTotal = ref(0)
const currentPage = ref(1)
const pageSize = 20

const handleFileChange = (file) => {
  selectedFile.value = file.raw
}

const handleExceed = () => {
  ElMessage.warning('只能上传一个文件，请先删除已选文件')
}

const handleImport = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择Excel文件')
    return
  }

  importing.value = true
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    formData.append('scenicSpot', scenicSpot.value)

    const res = await request.post('/tourist/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })

    importResult.value = res.data.data
    ElMessage.success(`导入成功: ${res.data.data.successCount} 条`)

    // 刷新预览
    loadPreview()
  } catch (e) {
    ElMessage.error('导入失败: ' + (e.response?.data?.message || e.message))
  } finally {
    importing.value = false
  }
}

const handleClear = async () => {
  try {
    await ElMessageBox.confirm(
      `确定清空 "${spots.value.find(s => s.id === scenicSpot.value)?.name}" 的所有消费数据？`,
      '警告',
      { type: 'warning' }
    )

    await request.delete(`/tourist/clear/${scenicSpot.value}`)
    ElMessage.success('清空成功')
    loadPreview()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('清空失败')
    }
  }
}

const loadPreview = async () => {
  try {
    const res = await request.get('/tourist/page', {
      params: {
        pageNum: currentPage.value,
        pageSize: pageSize,
        scenicSpot: previewSpot.value
      }
    })

    const data = res.data.data
    previewData.value = data.records || []
    previewTotal.value = data.total || 0
  } catch (e) {
    previewData.value = []
    previewTotal.value = 0
  }
}

onMounted(() => {
  loadPreview()
})
</script>

<style scoped>
.tourist-import {
  padding: 0;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a2e;
}

.page-desc {
  font-size: 13px;
  color: #909399;
  margin-left: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.error-list {
  padding-left: 20px;
  color: #f56c6c;
  font-size: 13px;
}

.error-list li {
  margin-bottom: 4px;
}
</style>
