<template>
  <div class="dashboard">
    <!-- 顶部欢迎区域 -->
    <div class="welcome-banner">
      <div class="welcome-content">
        <div class="welcome-text">
          <h1 class="welcome-title">数据驾驶舱</h1>
          <p class="welcome-subtitle">实时监控景区运营数据，智能分析游客行为</p>
        </div>
        <div class="welcome-time">
          <div class="time-display">{{ currentTime }}</div>
          <div class="date-display">{{ currentDate }}</div>
        </div>
      </div>
    </div>

    <!-- 核心指标卡片 -->
    <div class="metrics-grid">
      <div class="metric-card" v-for="(metric, index) in metrics" :key="index">
        <div class="metric-icon" :style="{ background: metric.gradient }">
          <el-icon :size="28"><component :is="metric.icon" /></el-icon>
        </div>
        <div class="metric-info">
          <div class="metric-label">{{ metric.label }}</div>
          <div class="metric-value">{{ metric.value }}</div>
          <div class="metric-trend" :class="metric.trend > 0 ? 'up' : 'down'">
            <el-icon><component :is="metric.trend > 0 ? 'Top' : 'Bottom'" /></el-icon>
            {{ Math.abs(metric.trend) }}% 较昨日
          </div>
        </div>
        <div class="metric-sparkline">
          <div class="sparkline-bar" v-for="(v, i) in metric.sparkline" :key="i"
               :style="{ height: v + '%', background: metric.gradient }"></div>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-grid">
      <!-- 趋势图 -->
      <div class="chart-card main-chart">
        <div class="chart-header">
          <div>
            <h3 class="chart-title">游客与消费趋势</h3>
            <p class="chart-subtitle">实时监控游客流量和消费变化</p>
          </div>
          <div class="chart-controls">
            <el-radio-group v-model="trendDays" size="small" @change="fetchTrend">
              <el-radio-button :value="7">7天</el-radio-button>
              <el-radio-button :value="30">30天</el-radio-button>
            </el-radio-group>
          </div>
        </div>
        <div ref="trendChart" class="chart-body"></div>
      </div>

      <!-- 消费结构 -->
      <div class="chart-card">
        <div class="chart-header">
          <div>
            <h3 class="chart-title">消费结构</h3>
            <p class="chart-subtitle">各类消费占比分析</p>
          </div>
        </div>
        <div ref="consumptionChart" class="chart-body"></div>
      </div>

      <!-- 年龄分布 -->
      <div class="chart-card">
        <div class="chart-header">
          <div>
            <h3 class="chart-title">游客画像</h3>
            <p class="chart-subtitle">年龄与满意度分布</p>
          </div>
        </div>
        <div class="chart-body">
          <div class="profile-grid">
            <div class="profile-item" v-for="age in ageDistribution" :key="age.name">
              <div class="profile-label">{{ age.name }}</div>
              <div class="profile-bar-container">
                <div class="profile-bar" :style="{ width: age.percent + '%', background: age.color }"></div>
              </div>
              <div class="profile-value">{{ age.value }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 景点热度 -->
      <div class="chart-card">
        <div class="chart-header">
          <div>
            <h3 class="chart-title">景点热度TOP5</h3>
            <p class="chart-subtitle">最受欢迎景点排行</p>
          </div>
        </div>
        <div ref="spotChart" class="chart-body"></div>
      </div>
    </div>

    <!-- 底部数据表格 -->
    <div class="table-section">
      <div class="table-card">
        <div class="table-header">
          <h3 class="table-title">景点消费排行</h3>
          <el-button size="small" text @click="showAllSpotsDialog = true">查看全部</el-button>
        </div>
        <el-table :data="spotRanking" style="width:100%" :header-cell-style="headerStyle" :row-style="rowStyle">
          <el-table-column type="index" label="#" width="50" fixed />
          <el-table-column prop="name" label="景点名称" min-width="120">
            <template #default="{ row }">
              <div class="spot-name">
                <div class="spot-dot" :style="{ background: getSpotColor(row.name) }"></div>
                {{ row.name }}
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="visitorCount" label="游客数" min-width="80">
            <template #default="{ row }">
              <span class="highlight-value">{{ formatNumber(row.visitorCount) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="avgConsumption" label="人均消费" min-width="100">
            <template #default="{ row }">
              <span class="money-value">¥{{ row.avgConsumption }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="totalConsumption" label="总消费额" min-width="100">
            <template #default="{ row }">
              <span class="money-value">¥{{ formatMoney(row.totalConsumption) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="avgSatisfaction" label="满意度" min-width="120">
            <template #default="{ row }">
              <div class="satisfaction-bar">
                <div class="satisfaction-fill" :style="{ width: (row.avgSatisfaction / 5 * 100) + '%' }"></div>
                <span class="satisfaction-text">{{ row.avgSatisfaction }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="avgStayDuration" label="停留时长" min-width="80">
            <template #default="{ row }">
              {{ row.avgStayDuration }}h
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 全部景点消费数据弹窗 -->
    <el-dialog v-model="showAllSpotsDialog" title="景点消费数据" width="85%" style="max-width:900px">
      <el-table :data="allSpotRanking" style="width:100%" stripe :header-cell-style="headerStyle" :row-style="rowStyle">
        <el-table-column type="index" label="#" width="50" fixed />
        <el-table-column prop="name" label="景点名称" min-width="120">
          <template #default="{ row }">
            <div class="spot-name">
              <div class="spot-dot" :style="{ background: getSpotColor(row.name) }"></div>
              {{ row.name }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="visitorCount" label="游客数" width="100">
          <template #default="{ row }">
            <span class="highlight-value">{{ formatNumber(row.visitorCount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="avgConsumption" label="人均消费" width="120">
          <template #default="{ row }">
            <span class="money-value">¥{{ row.avgConsumption }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalConsumption" label="总消费额" width="120">
          <template #default="{ row }">
            <span class="money-value">¥{{ formatMoney(row.totalConsumption) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="avgSatisfaction" label="满意度" width="150">
          <template #default="{ row }">
            <div class="satisfaction-bar">
              <div class="satisfaction-fill" :style="{ width: (row.avgSatisfaction / 5 * 100) + '%' }"></div>
              <span class="satisfaction-text">{{ row.avgSatisfaction }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="avgStayDuration" label="停留时长" width="100">
          <template #default="{ row }">
            {{ row.avgStayDuration }}h
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { User, TrendCharts, Money, ChatDotRound, Top, Bottom } from '@element-plus/icons-vue'
import { useThemeStore } from '@/stores/theme'
import request from '@/utils/request'

const router = useRouter()
const themeStore = useThemeStore()
let echartsModule = null

// 主题相关颜色
const isDark = computed(() => themeStore.isDark)
const textColor = computed(() => isDark.value ? '#94a3b8' : '#666')
const bgColor = computed(() => isDark.value ? 'transparent' : '#ffffff')
const borderColor = computed(() => isDark.value ? 'rgba(255,255,255,0.1)' : '#e0e0e0')
const gridColor = computed(() => isDark.value ? 'rgba(255,255,255,0.05)' : '#f0f0f0')

// 时间显示
const currentTime = ref('')
const currentDate = ref('')
const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
  currentDate.value = now.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })
}

// 核心指标
const stats = ref({})
const realtime = ref({})

const metrics = computed(() => [
  {
    label: '总游客数',
    value: formatNumber(stats.value.totalUsers || 0),
    icon: 'User',
    gradient: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    trend: 12,
    sparkline: [30, 45, 60, 55, 70, 65, 80]
  },
  {
    label: '今日游客',
    value: formatNumber(realtime.value.onlineTourists || 0),
    icon: 'TrendCharts',
    gradient: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
    trend: 8,
    sparkline: [20, 35, 40, 50, 45, 55, 60]
  },
  {
    label: '总消费额',
    value: '¥' + formatMoney(realtime.value.todayConsumption || 0),
    icon: 'Money',
    gradient: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
    trend: 15,
    sparkline: [40, 35, 50, 60, 55, 70, 85]
  },
  {
    label: 'AI对话数',
    value: formatNumber(stats.value.totalMessages || 0),
    icon: 'ChatDotRound',
    gradient: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
    trend: 5,
    sparkline: [25, 30, 35, 40, 38, 45, 50]
  }
])

// 图表数据
const trendDays = ref(7)
const spotRanking = ref([])
const allSpotRanking = ref([])
const ageDistribution = ref([])
const showAllSpotsDialog = ref(false)

// 图表引用
const trendChart = ref(null)
const consumptionChart = ref(null)
const spotChart = ref(null)

let charts = []

// 格式化函数
const formatNumber = (num) => {
  if (num >= 10000) return (num / 10000).toFixed(1) + '万'
  return num.toLocaleString()
}

const formatMoney = (num) => {
  if (num >= 10000) return (num / 10000).toFixed(1) + '万'
  return num.toLocaleString()
}

const getSpotColor = (name) => {
  const colors = ['#667eea', '#f093fb', '#4facfe', '#43e97b', '#f5576c']
  return colors[name.length % colors.length]
}

// 表格样式
const headerStyle = {
  background: 'rgba(255,255,255,0.05)',
  color: '#94a3b8',
  fontWeight: '500',
  fontSize: '12px',
  borderBottom: '1px solid rgba(255,255,255,0.05)'
}

const rowStyle = {
  background: 'transparent',
  color: '#e2e8f0',
  borderBottom: '1px solid rgba(255,255,255,0.03)'
}

// 数据获取
const fetchStats = async () => {
  try {
    const res = await request.get('/statistics/dashboard')
    stats.value = res.data.data || {}
    console.log('[Dashboard] Stats loaded:', stats.value)
  } catch (e) { console.error('[Dashboard] fetchStats error:', e) }
}

const fetchRealtime = async () => {
  try {
    const res = await request.get('/statistics/realtime')
    realtime.value = res.data.data || {}
    console.log('[Dashboard] Realtime loaded:', realtime.value)
  } catch (e) { console.error('[Dashboard] fetchRealtime error:', e) }
}

const fetchTrend = async () => {
  try {
    const res = await request.get(`/statistics/trend?days=${trendDays.value}`)
    await renderTrendChart(res.data.data || [])
  } catch (e) { console.error('[Dashboard] fetchTrend error:', e) }
}

const fetchConsumptionData = async () => {
  try {
    const res = await request.get('/statistics/consumption')
    const data = res.data.data || {}
    spotRanking.value = (data.spotRanking || []).slice(0, 5)
    allSpotRanking.value = data.spotRanking || []

    // 年龄分布
    const ageData = data.ageDistribution || []
    const total = ageData.reduce((sum, d) => sum + (d.value || 0), 0) || 1
    const colors = ['#667eea', '#f093fb', '#4facfe', '#43e97b', '#f5576c']
    ageDistribution.value = ageData.map((d, i) => ({
      ...d,
      percent: Math.round((d.value / total) * 100),
      color: colors[i % colors.length]
    }))

    if (data.structure) await renderConsumptionChart(data.structure)
    if (data.spotRanking) await renderSpotChart(data.spotRanking.slice(0, 5))
  } catch (e) { console.error('[Dashboard] fetchConsumptionData error:', e) }
}

// 图表渲染
const initChart = async (el) => {
  if (!echartsModule) echartsModule = await import('echarts')
  const chart = echartsModule.init(el, 'dark')
  charts.push(chart)
  return chart
}

const renderTrendChart = async (data) => {
  if (!trendChart.value) return
  const chart = await initChart(trendChart.value)
  chart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(15,23,42,0.9)', borderColor: 'rgba(255,255,255,0.1)' },
    legend: { data: ['游客数', '消费额'], top: 0, textStyle: { color: '#94a3b8' } },
    xAxis: { type: 'category', data: data.map(d => d.date), axisLine: { lineStyle: { color: 'rgba(255,255,255,0.1)' } }, axisLabel: { color: '#94a3b8' } },
    yAxis: [
      { type: 'value', name: '游客数', position: 'left', axisLine: { lineStyle: { color: 'rgba(255,255,255,0.1)' } }, axisLabel: { color: '#94a3b8' }, splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } } },
      { type: 'value', name: '消费额(元)', position: 'right', axisLine: { lineStyle: { color: 'rgba(255,255,255,0.1)' } }, axisLabel: { color: '#94a3b8' }, splitLine: { show: false } }
    ],
    series: [
      {
        name: '游客数', type: 'bar', data: data.map(d => d.messages || 0),
        itemStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: '#667eea' }, { offset: 1, color: '#764ba2' }] }, borderRadius: [4, 4, 0, 0] },
        barWidth: '40%'
      },
      {
        name: '消费额', type: 'line', yAxisIndex: 1, smooth: true,
        data: data.map(d => (d.messages || 0) * 500),
        lineStyle: { color: '#43e97b', width: 2 },
        itemStyle: { color: '#43e97b' },
        areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(67,233,123,0.3)' }, { offset: 1, color: 'rgba(67,233,123,0)' }] } }
      }
    ],
    grid: { top: 40, right: 60, bottom: 30, left: 50 }
  })
}

const renderConsumptionChart = async (data) => {
  if (!consumptionChart.value) return
  const chart = await initChart(consumptionChart.value)
  const colors = ['#667eea', '#f093fb', '#4facfe', '#43e97b', '#f5576c']
  chart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'item', formatter: '{b}: {c}%', backgroundColor: 'rgba(15,23,42,0.9)', borderColor: 'rgba(255,255,255,0.1)' },
    series: [{
      type: 'pie',
      radius: ['45%', '75%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: '#0f172a', borderWidth: 2 },
      label: { show: true, position: 'outside', color: '#94a3b8', fontSize: 12 },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold', color: '#fff' } },
      data: data.map((d, i) => ({ name: d.name, value: d.value, itemStyle: { color: colors[i % colors.length] } }))
    }]
  })
}

const renderSpotChart = async (data) => {
  if (!spotChart.value) return
  const chart = await initChart(spotChart.value)
  const colors = ['#667eea', '#f093fb', '#4facfe', '#43e97b', '#f5576c']
  chart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(15,23,42,0.9)', borderColor: 'rgba(255,255,255,0.1)' },
    xAxis: { type: 'category', data: data.map(d => d.name), axisLine: { lineStyle: { color: 'rgba(255,255,255,0.1)' } }, axisLabel: { color: '#94a3b8', rotate: 30 } },
    yAxis: { type: 'value', axisLine: { lineStyle: { color: 'rgba(255,255,255,0.1)' } }, axisLabel: { color: '#94a3b8' }, splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } } },
    series: [{
      type: 'bar',
      data: data.map((d, i) => ({ value: d.visitorCount, itemStyle: { color: colors[i % colors.length], borderRadius: [4, 4, 0, 0] } })),
      barWidth: '50%'
    }],
    grid: { top: 10, right: 10, bottom: 40, left: 50 }
  })
}

// 轮询
let pollTimer = null
const poll = async () => {
  await Promise.all([fetchStats(), fetchRealtime()])
}

onMounted(async () => {
  updateTime()
  setInterval(updateTime, 1000)

  await fetchStats()
  await fetchRealtime()
  await nextTick()
  fetchTrend()
  fetchConsumptionData()
  pollTimer = setInterval(poll, 300000)
})

onBeforeUnmount(() => {
  charts.forEach(c => c.dispose())
  charts = []
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style scoped>
.dashboard {
  min-height: 100vh;
  background: var(--bg-primary);
  padding: 20px;
  transition: background 0.3s ease;
}

/* 欢迎横幅 */
.welcome-banner {
  background: linear-gradient(135deg, rgba(102,126,234,0.15) 0%, rgba(118,75,162,0.15) 100%);
  border: 1px solid var(--border-primary);
  border-radius: 16px;
  padding: 24px 32px;
  margin-bottom: 20px;
}

.welcome-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.welcome-title {
  font-size: 24px;
  font-weight: 700;
  background: var(--accent-gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin: 0;
}

.welcome-subtitle {
  font-size: 14px;
  color: var(--text-muted);
  margin: 4px 0 0 0;
}

.time-display {
  font-size: 28px;
  font-weight: 300;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}

.date-display {
  font-size: 12px;
  color: var(--text-muted);
  text-align: right;
}

/* 指标卡片 */
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.metric-card {
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  border-radius: 16px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: transform 0.3s, box-shadow 0.3s, background 0.3s;
}

.metric-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
}

.metric-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.metric-info {
  flex: 1;
}

.metric-label {
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 4px;
}

.metric-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
}

.metric-trend {
  font-size: 11px;
  margin-top: 4px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.metric-trend.up { color: #43e97b; }
.metric-trend.down { color: #f5576c; }

.metric-sparkline {
  display: flex;
  align-items: flex-end;
  gap: 3px;
  height: 40px;
}

.sparkline-bar {
  width: 4px;
  border-radius: 2px;
  opacity: 0.6;
}

/* 图表区域 */
.charts-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.chart-card {
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  border-radius: 16px;
  padding: 20px;
  transition: background 0.3s ease;
}

.main-chart {
  grid-column: span 2;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.chart-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.chart-subtitle {
  font-size: 12px;
  color: var(--text-muted);
  margin: 4px 0 0 0;
}

.chart-body {
  height: 280px;
}

/* 游客画像 */
.profile-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 10px 0;
}

.profile-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.profile-label {
  width: 60px;
  font-size: 12px;
  color: var(--text-muted);
}

.profile-bar-container {
  flex: 1;
  height: 8px;
  background: var(--border-primary);
  border-radius: 4px;
  overflow: hidden;
}

.profile-bar {
  height: 100%;
  border-radius: 4px;
  transition: width 1s ease;
}

.profile-value {
  width: 40px;
  text-align: right;
  font-size: 12px;
  color: var(--text-primary);
}

/* 表格区域 */
.table-section {
  margin-top: 20px;
}

.table-card {
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  border-radius: 16px;
  padding: 20px;
  transition: background 0.3s ease;
  overflow-x: auto;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.table-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.spot-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.spot-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.highlight-value {
  color: var(--accent-primary);
  font-weight: 600;
}

.money-value {
  color: #43e97b;
  font-weight: 500;
}

.satisfaction-bar {
  position: relative;
  height: 20px;
  background: var(--border-primary);
  border-radius: 10px;
  overflow: hidden;
  display: flex;
  align-items: center;
}

.satisfaction-fill {
  height: 100%;
  background: linear-gradient(90deg, #43e97b, #38f9d7);
  border-radius: 10px;
  transition: width 1s ease;
}

.satisfaction-text {
  position: absolute;
  right: 8px;
  font-size: 11px;
  color: var(--text-primary);
  font-weight: 600;
}
</style>
