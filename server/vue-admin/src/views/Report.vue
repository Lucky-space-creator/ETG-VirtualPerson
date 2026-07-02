<template>
  <div class="report-page">
    <el-card>
      <div style="display:flex;align-items:center;justify-content:space-between">
        <h3 style="margin:0;font-size:16px">用户分析报告</h3>
        <div>
          <el-button type="primary" @click="handleGenerate">生成报告</el-button>
          <el-button @click="handleExport">导出报告</el-button>
        </div>
      </div>
    </el-card>

    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="12">
        <el-card>
          <template #header>
            情感分布
            <el-tooltip content="积极:感谢/满意 开心; 消极:不满/抱怨; 中性:普通提问">
              <el-icon style="margin-left:4px"><QuestionFilled /></el-icon>
            </el-tooltip>
          </template>
          <div ref="sentimentChart" style="height:300px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>热门问题 TOP10</template>
          <el-table :data="hotQuestions" size="small" max-height="300">
            <el-table-column type="index" label="#" width="40" />
            <el-table-column prop="question" label="问题" show-overflow-tooltip />
            <el-table-column prop="count" label="频次" width="80" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="12">
        <el-card>
          <template #header>情感趋势（近30天）</template>
          <div ref="trendChart" style="height:300px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>数字人使用统计</template>
          <div ref="avatarChart" style="height:300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top:20px">
      <template #header>游客画像（LLM智能分析）</template>
      <el-descriptions :column="2" border v-if="llmInsight.routePreference">
        <el-descriptions-item label="路线偏好">{{ llmInsight.routePreference }}</el-descriptions-item>
        <el-descriptions-item label="兴趣特征">{{ llmInsight.interestFeatures }}</el-descriptions-item>
        <el-descriptions-item label="游客类型">{{ llmInsight.touristType }}</el-descriptions-item>
        <el-descriptions-item label="服务满意度">{{ llmInsight.satisfaction }}</el-descriptions-item>
        <el-descriptions-item label="综合总结" :span="2">{{ llmInsight.summary }}</el-descriptions-item>
      </el-descriptions>
      <el-descriptions :column="1" border v-else>
        <el-descriptions-item label="状态">
          {{ llmInsight.summary || llmInsight.error || '暂无分析数据' }}
        </el-descriptions-item>
        <el-descriptions-item label="原始分析" v-if="llmInsight.rawAnalysis">
          {{ llmInsight.rawAnalysis }}
        </el-descriptions-item>
      </el-descriptions>
      <div v-if="llmInsight.dataCount" style="margin-top:8px;color:#909399;font-size:12px">
        基于 {{ llmInsight.dataCount }} 条聊天记录分析
        <span v-if="llmInsight.analyzedAt"> | 分析时间: {{ llmInsight.analyzedAt }}</span>
      </div>
    </el-card>

    <el-card style="margin-top:20px">
      <template #header>用户活跃度 TOP10（仅用户消息）</template>
      <el-table :data="topUsers" size="small" max-height="300">
        <el-table-column type="index" label="#" width="40" />
        <el-table-column prop="username" label="用户" width="120" />
        <el-table-column prop="messageCount" label="消息数" width="100" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { QuestionFilled } from '@element-plus/icons-vue'

let echartsModule = null

const hotQuestions = ref([])
const topUsers = ref([])
const llmInsight = ref({})
const sentimentChart = ref(null)
const trendChart = ref(null)
const avatarChart = ref(null)
let sChart = null, tChart = null, aChart = null
let pollTimer = null

onMounted(() => {
  fetchReportData()
})

const handleGenerate = async () => {
  try {
    const res = await request.post('/report/generate')
    ElMessage.success(res.data?.data || '报告生成中，请稍候...')
    if (pollTimer) clearInterval(pollTimer)
    let attempts = 0
    pollTimer = setInterval(async () => {
      attempts++
      await fetchReportData()
      const summary = llmInsight.value?.summary || ''
      const done = summary !== 'LLM分析中，请稍候...' &&
                   summary !== '请点击"生成报告"触发LLM智能分析'
      if (done || attempts > 30) {
        clearInterval(pollTimer)
        pollTimer = null
        if (done) ElMessage.success('报告生成完成')
      }
    }, 3000)
  } catch (e) {
    ElMessage.error('生成失败')
  }
}

const handleExport = () => {
  // 收集所有报告数据，生成可打印的文档
  const win = window.open('', '_blank')
  const sentiment = (window.__reportData?.sentiment || [])
  const sentimentHtml = sentiment.map(s => {
    const label = s.emotion === 'positive' ? '积极' :
                  s.emotion === 'negative' ? '消极' : '中性'
    return `<tr><td>${label}</td><td>${s.count}</td><td>${s.ratio}%</td></tr>`
  }).join('')

  const hotHtml = hotQuestions.value.map((q, i) =>
    `<tr><td>${i + 1}</td><td>${q.question}</td><td>${q.count}</td></tr>`
  ).join('')

  const topHtml = topUsers.value.map((u, i) =>
    `<tr><td>${i + 1}</td><td>${u.username}</td><td>${u.messageCount}</td></tr>`
  ).join('')

  const insight = llmInsight.value || {}
  const insightHtml = insight.routePreference ? `
    <tr><td>路线偏好</td><td>${insight.routePreference || '-'}</td></tr>
    <tr><td>兴趣特征</td><td>${insight.interestFeatures || '-'}</td></tr>
    <tr><td>游客类型</td><td>${insight.touristType || '-'}</td></tr>
    <tr><td>服务满意度</td><td>${insight.satisfaction || '-'}</td></tr>
    <tr><td>综合总结</td><td>${insight.summary || '-'}</td></tr>
  ` : `<tr><td>状态</td><td>${insight.summary || '未生成'}</td></tr>`

  win.document.write(`<!DOCTYPE html><html><head><meta charset="utf-8">
    <title>用户分析报告</title>
    <style>
      body{font-family:Microsoft YaHei,sans-serif;padding:40px;max-width:900px;margin:0 auto;color:#333}
      h1{text-align:center;border-bottom:2px solid #409EFF;padding-bottom:12px}
      h2{color:#409EFF;margin-top:30px;border-left:4px solid #409EFF;padding-left:10px}
      table{width:100%;border-collapse:collapse;margin:10px 0}
      th,td{border:1px solid #ddd;padding:8px 12px;text-align:left}
      th{background:#f5f7fa;font-weight:600}
      tr:nth-child(even){background:#fafafa}
      .meta{color:#999;font-size:12px;text-align:center;margin-top:30px}
      @media print{body{padding:20px}}
    </style>
  </head><body>
    <h1>用户分析报告</h1>
    <p style="text-align:center;color:#666">生成时间：${new Date().toLocaleString()}</p>

    <h2>一、情感分布</h2>
    <table><tr><th>情感</th><th>数量</th><th>占比</th></tr>${sentimentHtml}</table>

    <h2>二、热门问题 TOP10</h2>
    <table><tr><th>#</th><th>问题</th><th>频次</th></tr>${hotHtml}</table>

    <h2>三、游客画像</h2>
    <table><tr><th>维度</th><th>分析结果</th></tr>${insightHtml}</table>

    <h2>四、用户活跃度 TOP10</h2>
    <table><tr><th>#</th><th>用户</th><th>消息数</th></tr>${topHtml}</table>

    <p class="meta">VirtualWife 用户分析系统 · 自动生成</p>
    <script>window.print();<\/script>
  </body></html>`)
  win.document.close()
}

const fetchReportData = async () => {
  try {
    const res = await request.get('/report/data')
    const data = res.data.data || {}
    window.__reportData = data
    hotQuestions.value = data.hotQuestions || []
    topUsers.value = data.userProfile?.topUsers || []
    llmInsight.value = data.userProfile?.llmInsight || {}
    await nextTick()
    renderCharts(data)
  } catch (e) {
    ElMessage.error('获取报告数据失败')
  }
}

const renderCharts = async (data) => {
  try {
    if (!echartsModule) echartsModule = await import('echarts')

    if (sentimentChart.value) {
      if (!sChart) sChart = echartsModule.init(sentimentChart.value)
      const sentimentData = (data.sentiment || []).map(item => ({
        name: item.emotion === 'positive' ? '积极' :
              item.emotion === 'negative' ? '消极' : '中性',
        value: item.count
      }))
      sChart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        legend: { orient: 'vertical', left: 'left' },
        color: ['#67C23A', '#F56C6C', '#909399'],
        series: [{ type: 'pie', radius: ['40%', '70%'], data: sentimentData }]
      })
    }

    if (trendChart.value) {
      if (!tChart) tChart = echartsModule.init(trendChart.value)
      const trendData = data.sentimentTrend || []
      const dates = trendData.map(d => d.date)
      const nameMap = { positive: '积极', negative: '消极', neutral: '中性' }
      const colorMap = { positive: '#67C23A', negative: '#F56C6C', neutral: '#909399' }
      tChart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: Object.values(nameMap) },
        xAxis: { type: 'category', data: dates },
        yAxis: { type: 'value' },
        series: ['positive', 'negative', 'neutral'].map(emotion => ({
          name: nameMap[emotion], type: 'line', smooth: true,
          data: trendData.map(d => d[emotion] || 0),
          itemStyle: { color: colorMap[emotion] }
        })),
        grid: { top: 40, right: 20, bottom: 30, left: 40 }
      })
    }

    if (avatarChart.value) {
      if (!aChart) aChart = echartsModule.init(avatarChart.value)
      const avatarData = (data.userProfile?.avatarStats || []).map(item => ({
        name: item.avatarName, value: item.count
      }))
      aChart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        legend: { orient: 'vertical', left: 'left' },
        series: [{ type: 'pie', radius: ['40%', '70%'], data: avatarData }]
      })
    }
  } catch (e) { console.error('渲染图表失败', e) }
}

onBeforeUnmount(() => {
  if (sChart) { sChart.dispose(); sChart = null }
  if (tChart) { tChart.dispose(); tChart = null }
  if (aChart) { aChart.dispose(); aChart = null }
  echartsModule = null
})
</script>
