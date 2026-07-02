<template>
  <div class="tourist-analysis">
    <el-page-header @back="$router.push('/')" title="返回">
      <template #content>游客消费习惯分析</template>
    </el-page-header>

    <!-- 上传区域 -->
    <el-card style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span>数据导入</span>
        </div>
      </template>
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
        <div class="el-upload__text">拖拽Excel文件到此处 或 <em>点击选择文件</em></div>
        <template #tip>
          <div class="el-upload__tip">支持 .xlsx / .xls 格式的游客消费数据</div>
        </template>
      </el-upload>
      <div style="margin-top: 16px; text-align: center">
        <el-button type="primary" @click="analyzeData" :loading="loading" :disabled="!selectedFile">
          开始分析
        </el-button>
        <el-button type="success" @click="analyzeWithLLM" :loading="llmLoading" :disabled="!selectedFile" style="margin-left: 16px">
          AI深度分析
        </el-button>
      </div>
    </el-card>

    <!-- 分析结果 -->
    <template v-if="analysisResult">
      <!-- 数据概览 -->
      <el-card style="margin-top: 20px">
        <template #header>
          <div class="card-header">
            <span>数据概览</span>
          </div>
        </template>
        <el-row :gutter="20">
          <el-col :span="6">
            <el-statistic title="总记录数" :value="analysisResult.summary.total_records" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="景点数量" :value="analysisResult.summary.attractions_count" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="平均消费" :value="analysisResult.summary.cost_stats.total_cost.mean" :precision="2" suffix="元" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="平均满意度" :value="analysisResult.summary.avg_satisfaction" :precision="2" suffix="/5" />
          </el-col>
        </el-row>
      </el-card>

      <!-- 消费结构 -->
      <el-card style="margin-top: 20px">
        <template #header>
          <div class="card-header">
            <span>消费结构分析</span>
          </div>
        </template>
        <el-row :gutter="20">
          <el-col :span="12">
            <div ref="costChartRef" style="height: 300px"></div>
          </el-col>
          <el-col :span="12">
            <el-descriptions title="消费统计" :column="2" border>
              <el-descriptions-item label="门票消费">
                {{ analysisResult.summary.cost_stats.ticket_cost.mean?.toFixed(2) }}元
                <el-tag type="info" size="small">{{ analysisResult.summary.cost_structure.ticket_ratio?.toFixed(1) }}%</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="餐饮消费">
                {{ analysisResult.summary.cost_stats.food_cost.mean?.toFixed(2) }}元
                <el-tag type="success" size="small">{{ analysisResult.summary.cost_structure.food_ratio?.toFixed(1) }}%</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="购物消费">
                {{ analysisResult.summary.cost_stats.shopping_cost.mean?.toFixed(2) }}元
                <el-tag type="warning" size="small">{{ analysisResult.summary.cost_structure.shopping_ratio?.toFixed(1) }}%</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="交通消费">
                {{ analysisResult.summary.cost_stats.transport_cost.mean?.toFixed(2) }}元
                <el-tag type="danger" size="small">{{ analysisResult.summary.cost_structure.transport_ratio?.toFixed(1) }}%</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="娱乐消费">
                {{ analysisResult.summary.cost_stats.entertainment_cost.mean?.toFixed(2) }}元
                <el-tag type="primary" size="small">{{ analysisResult.summary.cost_structure.entertainment_ratio?.toFixed(1) }}%</el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </el-col>
        </el-row>
      </el-card>

      <!-- 年龄消费分析 -->
      <el-card style="margin-top: 20px">
        <template #header>
          <div class="card-header">
            <span>年龄段消费分析</span>
          </div>
        </template>
        <el-table :data="ageTableData" stripe style="width: 100%">
          <el-table-column prop="age_group" label="年龄段" width="120" />
          <el-table-column prop="count" label="人数" width="100" />
          <el-table-column prop="avg_total_cost" label="平均消费" width="120">
            <template #default="{ row }">
              {{ row.avg_total_cost?.toFixed(2) }}元
            </template>
          </el-table-column>
          <el-table-column prop="avg_stay_duration" label="平均停留" width="120">
            <template #default="{ row }">
              {{ row.avg_stay_duration?.toFixed(1) }}小时
            </template>
          </el-table-column>
          <el-table-column prop="avg_satisfaction" label="平均满意度" width="120">
            <template #default="{ row }">
              <el-rate v-model="row.avg_satisfaction" disabled :max="5" />
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 消费模式识别 -->
      <el-card style="margin-top: 20px">
        <template #header>
          <div class="card-header">
            <span>消费模式识别</span>
          </div>
        </template>
        <el-row :gutter="20">
          <el-col :span="8" v-for="pattern in analysisResult.patterns" :key="pattern.pattern_name">
            <el-card shadow="hover">
              <template #header>
                <div class="pattern-header">
                  <span>{{ pattern.pattern_name }}</span>
                  <el-tag type="primary" size="small">{{ pattern.percentage?.toFixed(1) }}%</el-tag>
                </div>
              </template>
              <div class="pattern-content">
                <p class="pattern-desc">{{ pattern.description }}</p>
                <p class="pattern-cost">平均消费: {{ pattern.avg_cost?.toFixed(0) }}元</p>
                <ul class="pattern-chars">
                  <li v-for="char in pattern.characteristics" :key="char">{{ char }}</li>
                </ul>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-card>

      <!-- 数据洞察 -->
      <el-card style="margin-top: 20px">
        <template #header>
          <div class="card-header">
            <span>数据洞察</span>
          </div>
        </template>
        <el-timeline>
          <el-timeline-item
            v-for="(insight, index) in analysisResult.insights"
            :key="index"
            :type="index === 0 ? 'primary' : 'success'"
            :hollow="index > 0"
          >
            {{ insight }}
          </el-timeline-item>
        </el-timeline>
      </el-card>

      <!-- 优化建议 -->
      <el-card style="margin-top: 20px">
        <template #header>
          <div class="card-header">
            <span>优化建议</span>
          </div>
        </template>
        <el-collapse>
          <el-collapse-item v-for="(rec, index) in analysisResult.recommendations" :key="index" :title="`建议 ${index + 1}`">
            <p>{{ rec }}</p>
          </el-collapse-item>
        </el-collapse>
      </el-card>
    </template>

    <!-- LLM分析结果 -->
    <el-card v-if="llmAnalysis" style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span>AI深度分析报告</span>
          <el-tag type="success" size="small">由AI生成</el-tag>
        </div>
      </template>
      <div class="llm-analysis-content" v-html="formatLLMContent(llmAnalysis)"></div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import request from '@/utils/request'

const uploadRef = ref(null)
const selectedFile = ref(null)
const loading = ref(false)
const llmLoading = ref(false)
const analysisResult = ref(null)
const llmAnalysis = ref(null)
const costChartRef = ref(null)

const ageTableData = computed(() => {
  if (!analysisResult.value?.age_consumption) return []
  return Object.entries(analysisResult.value.age_consumption).map(([group, data]) => ({
    age_group: group,
    ...data
  }))
})

const handleFileChange = (file) => {
  selectedFile.value = file.raw
}

const handleExceed = () => {
  ElMessage.warning('只能上传一个文件，请先删除已选文件')
}

const analyzeData = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择Excel文件')
    return
  }

  loading.value = true
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)

    const res = await request.post('http://localhost:5001/api/rag/tourist/analyze', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })

    analysisResult.value = res.data
    ElMessage.success('分析完成')

    await nextTick()
    renderCostChart()
  } catch (error) {
    ElMessage.error('分析失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const analyzeWithLLM = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择Excel文件')
    return
  }

  llmLoading.value = true
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)

    const res = await request.post('http://localhost:5001/api/rag/tourist/analyze/llm', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000 // 2分钟超时
    })

    llmAnalysis.value = res.data.llm_analysis
    ElMessage.success('AI分析完成')
  } catch (error) {
    ElMessage.error('AI分析失败: ' + (error.message || '未知错误'))
  } finally {
    llmLoading.value = false
  }
}

const renderCostChart = () => {
  if (!costChartRef.value || !analysisResult.value) return

  const chart = echarts.init(costChartRef.value)
  const structure = analysisResult.value.summary.cost_structure

  const option = {
    title: {
      text: '消费结构占比',
      left: 'center'
    },
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c}%'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [
      {
        name: '消费结构',
        type: 'pie',
        radius: '50%',
        data: [
          { value: structure.ticket_ratio?.toFixed(1), name: '门票' },
          { value: structure.food_ratio?.toFixed(1), name: '餐饮' },
          { value: structure.shopping_ratio?.toFixed(1), name: '购物' },
          { value: structure.transport_ratio?.toFixed(1), name: '交通' },
          { value: structure.entertainment_ratio?.toFixed(1), name: '娱乐' }
        ],
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  }

  chart.setOption(option)

  window.addEventListener('resize', () => {
    chart.resize()
  })
}

const formatLLMContent = (content) => {
  if (!content) return ''
  // 简单的Markdown转HTML
  return content
    .replace(/\n/g, '<br>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
}
</script>

<style scoped>
.tourist-analysis {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pattern-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pattern-content {
  font-size: 14px;
}

.pattern-desc {
  color: #606266;
  margin-bottom: 10px;
}

.pattern-cost {
  font-weight: bold;
  color: #409eff;
  margin-bottom: 10px;
}

.pattern-chars {
  list-style: none;
  padding: 0;
  margin: 0;
}

.pattern-chars li {
  padding: 4px 0;
  color: #909399;
  font-size: 12px;
}

.pattern-chars li::before {
  content: "•";
  margin-right: 8px;
  color: #409eff;
}

.llm-analysis-content {
  line-height: 1.8;
  color: #303133;
  font-size: 14px;
}

.llm-analysis-content strong {
  color: #409eff;
}
</style>
