<template>
  <div class="statistics-container">
    <h2>学习统计</h2>

    <!-- ==================== 统计卡片 ==================== -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="12" :sm="12" :md="6" :lg="6" :xl="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon blue">
            <el-icon :size="32"><Clock /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ formatHours(summary.totalHours) }}</div>
            <div class="stat-label">累计学习时长</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6" :lg="6" :xl="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon green">
            <el-icon :size="32"><Collection /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ summary.studyingCount || 0 }}</div>
            <div class="stat-label">在学课程</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6" :lg="6" :xl="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon orange">
            <el-icon :size="32"><CircleCheck /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ summary.completedCount || 0 }}</div>
            <div class="stat-label">已完成课程</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6" :lg="6" :xl="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon red">
            <el-icon :size="32"><TrendCharts /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ summary.avgCompletionRate || 0 }}%</div>
            <div class="stat-label">平均完成率</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ==================== 学习趋势 + 课程进度 ==================== -->
    <el-row :gutter="20" class="charts-row">
      <el-col :xs="24" :sm="24" :md="14" :lg="14" :xl="14">
        <el-card class="chart-card" shadow="never">
          <template #header>
            <div class="chart-header">
              <span>学习趋势（近7天）</span>
              <el-tag v-if="trendData.length === 0" type="info" size="small">暂无数据</el-tag>
            </div>
          </template>
          <!-- 有数据时显示图表 -->
          <div v-if="trendData.length > 0" ref="trendChartRef" class="chart-container"></div>
          <!-- 无数据时显示提示 -->
          <div v-else class="empty-chart">
            <el-icon :size="48" color="#DCDFE6"><DataLine /></el-icon>
            <p>暂无学习记录</p>
            <span class="sub-text">开始学习后，系统将自动记录每日学习时长</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="24" :md="10" :lg="10" :xl="10">
        <el-card class="chart-card" shadow="never">
          <template #header>
            <div class="chart-header">
              <span>课程完成进度</span>
              <el-tag v-if="courseProgressData.length === 0" type="info" size="small">暂无数据</el-tag>
            </div>
          </template>
          <div v-if="courseProgressData.length > 0" ref="progressChartRef" class="chart-container"></div>
          <div v-else class="empty-chart">
            <el-icon :size="48" color="#DCDFE6"><List /></el-icon>
            <p>暂无课程进度数据</p>
            <span class="sub-text">加入课程并学习后，此处将展示各课程完成进度</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ==================== 能力分析雷达图 ==================== -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card class="chart-card" shadow="never">
          <template #header>
            <div class="chart-header">
              <span>能力分析</span>
              <span class="chart-desc">基于各课程完成率</span>
              <el-tag v-if="radarData.length === 0" type="info" size="small">暂无数据</el-tag>
            </div>
          </template>
          <div v-if="radarData.length > 0" ref="radarChartRef" class="chart-container"></div>
          <div v-else class="empty-chart">
            <el-icon :size="48" color="#DCDFE6"><Aim /></el-icon>
            <p>暂无能力分析数据</p>
            <span class="sub-text">完成多门课程学习后，将生成个人能力雷达图</span>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Clock, Collection, CircleCheck, TrendCharts, DataLine, List, Aim } from '@element-plus/icons-vue'

// ==================== API 导入 ====================
// 方案A：使用专门的统计接口（推荐，需要后端配合）
import {
  getStudyOverview,
  getStatisticsTrend,
  getCourseRanking,
  getAbilityRadar
} from '@/api/study'

// 方案B：从已有接口推导统计数据（备选，当统计接口不可用时启用）
// import { getStudentCourses, getStudyRecords } from '@/api/course'

// ==================== 数据状态 ====================
const showDebugTip = ref(true)
const useMock = ref(true)

const mockStats = {
  totalHours: 11,
  studyingCount: 7,
  completedCount: 3,
  avgCompletionRate: 42,
  weeklyTrend: [
    { date: '05-27', minutes: 45 },
    { date: '05-28', minutes: 90 },
    { date: '05-29', minutes: 30 },
    { date: '05-30', minutes: 120 },
    { date: '05-31', minutes: 60 },
    { date: '06-01', minutes: 150 },
    { date: '06-02', minutes: 80 },
  ],
  courseProgress: [
    { courseName: 'Python编程入门', completionRate: 75, completedLessons: 15, totalLessons: 20 },
    { courseName: '高中化学基础', completionRate: 60, completedLessons: 6, totalLessons: 10 },
    { courseName: '趣味化学实验', completionRate: 30, completedLessons: 3, totalLessons: 10 },
    { courseName: '高中物理力学', completionRate: 45, completedLessons: 9, totalLessons: 20 },
    { courseName: '英语口语提升班', completionRate: 20, completedLessons: 2, totalLessons: 10 },
    { courseName: '高中数学进阶', completionRate: 55, completedLessons: 11, totalLessons: 20 },
    { courseName: '初中数学基础课程', completionRate: 90, completedLessons: 18, totalLessons: 20 },
  ],
  radarData: [
    { dimension: 'Python', score: 75 },
    { dimension: '高中化学', score: 45 },
    { dimension: '趣味化学', score: 30 },
    { dimension: '高中物理', score: 45 },
    { dimension: '英语口语', score: 20 },
    { dimension: '高中数学', score: 55 },
    { dimension: '初中数学', score: 90 },
  ]
}
const summary = ref({
  totalHours: 0,
  studyingCount: 0,
  completedCount: 0,
  avgCompletionRate: 0
})

const trendData = ref<any[]>([])
const courseProgressData = ref<any[]>([])
const radarData = ref<any[]>([])

const trendChartRef = ref<HTMLElement>()
const progressChartRef = ref<HTMLElement>()
const radarChartRef = ref<HTMLElement>()

let trendChart: any = null
let progressChart: any = null
let radarChart: any = null

// ==================== 格式化 ====================
const formatHours = (hours: number): string => {
  if (!hours || hours <= 0) return '0小时'
  if (hours < 1) {
    const mins = Math.round(hours * 60)
    return mins + '分钟'
  }
  return Math.round(hours * 10) / 10 + '小时'
}

// ==================== 增强版数据解析 ====================
const parseData = (res: any): any => {
  console.log('[Statistics] API原始响应:', res)

  if (!res) {
    console.warn('[Statistics] 响应为空')
    return null
  }

  // 情况1: res.data.data (最外层有code/message/data)
  if (res.data && res.data.data !== undefined && res.data.code === 200) {
    console.log('[Statistics] 解析路径: res.data.data')
    return res.data.data
  }

  // 情况2: res.data (外层无code，直接是data)
  if (res.data !== undefined && res.code === 200) {
    console.log('[Statistics] 解析路径: res.data')
    return res.data
  }

  // 情况3: 直接返回数组
  if (Array.isArray(res)) {
    console.log('[Statistics] 解析路径: 直接返回数组, 长度:', res.length)
    return res
  }

  // 情况4: res.data是数组
  if (res.data && Array.isArray(res.data)) {
    console.log('[Statistics] 解析路径: res.data数组, 长度:', res.data.length)
    return res.data
  }

  // 情况5: res.data.data是数组
  if (res.data && res.data.data && Array.isArray(res.data.data)) {
    console.log('[Statistics] 解析路径: res.data.data数组, 长度:', res.data.data.length)
    return res.data.data
  }

  // 情况6: 只要有data字段就返回
  if (res.data !== undefined) {
    console.log('[Statistics] 解析路径: res.data兜底')
    return res.data
  }

  // 情况7: 直接返回res本身
  console.log('[Statistics] 解析路径: 返回res本身')
  return res
}

// ==================== 数据获取 ====================
const fetchOverview = async () => {
  try {
    console.log('[Statistics] 正在请求: getStudyOverview')
    const res = await getStudyOverview()
    const data = parseData(res)

    if (data && typeof data === 'object') {
      summary.value = {
        totalHours: Number(data.totalHours) || 0,
        studyingCount: Number(data.studyingCount) || 0,
        completedCount: Number(data.completedCount) || 0,
        avgCompletionRate: Number(data.avgCompletionRate) || 0
      }
      console.log('[Statistics] 概览数据:', summary.value)
    } else {
      console.warn('[Statistics] getStudyOverview 返回数据为空或格式不对')
    }
  } catch (error: any) {
    console.error('[Statistics] getStudyOverview 请求失败:', error)
    if (error.response) {
      console.error('[Statistics] 错误响应状态:', error.response.status)
      console.error('[Statistics] 错误响应数据:', error.response.data)
    }
  }
}

const fetchTrend = async () => {
  try {
    console.log('[Statistics] 正在请求: getStatisticsTrend(7)')
    const res = await getStatisticsTrend(7)
    const data = parseData(res)

    if (Array.isArray(data) && data.length > 0) {
      trendData.value = data.map((d: any) => ({
        date: d.date || d.studyDate || d.day || '',
        minutes: Number(d.minutes) || Number(d.duration) || Number(d.studyTime) || 0
      }))
      console.log('[Statistics] 趋势数据条数:', trendData.value.length)
    } else {
      console.warn('[Statistics] getStatisticsTrend 返回空数组或非数组数据')
      trendData.value = []
    }
  } catch (error: any) {
    console.error('[Statistics] getStatisticsTrend 请求失败:', error)
    trendData.value = []
  }
}

const fetchCourseProgress = async () => {
  try {
    console.log('[Statistics] 正在请求: getCourseRanking')
    const res = await getCourseRanking()
    const data = parseData(res)

    if (Array.isArray(data) && data.length > 0) {
      courseProgressData.value = data.map((d: any) => ({
        courseName: d.courseName || d.name || d.title || '未知课程',
        completionRate: Number(d.completionRate) || Number(d.progress) || Number(d.rate) || 0,
        completedLessons: Number(d.completedLessons) || Number(d.finished) || 0,
        totalLessons: Number(d.totalLessons) || Number(d.total) || 0
      }))
      console.log('[Statistics] 课程进度条数:', courseProgressData.value.length)
    } else {
      console.warn('[Statistics] getCourseRanking 返回空数组或非数组数据')
      courseProgressData.value = []
    }
  } catch (error: any) {
    console.error('[Statistics] getCourseRanking 请求失败:', error)
    courseProgressData.value = []
  }
}

const fetchRadar = async () => {
  try {
    console.log('[Statistics] 正在请求: getAbilityRadar')
    const res = await getAbilityRadar()
    const data = parseData(res)

    if (Array.isArray(data) && data.length > 0) {
      radarData.value = data.map((d: any) => ({
        dimension: d.dimension || d.name || d.category || d.label || '',
        score: Number(d.score) || Number(d.value) || Number(d.rate) || 0
      })).filter((d: any) => d.dimension)
      console.log('[Statistics] 雷达数据条数:', radarData.value.length)
    } else {
      console.warn('[Statistics] getAbilityRadar 返回空数组或非数组数据')
      radarData.value = []
    }
  } catch (error: any) {
    console.error('[Statistics] getAbilityRadar 请求失败:', error)
    radarData.value = []
  }
}

// ==================== ECharts 初始化 ====================
const initTrendChart = async () => {
  if (!trendChartRef.value || trendData.value.length === 0) return

  try {
    const echarts = await import('echarts')
    if (trendChart) trendChart.dispose()
    trendChart = echarts.init(trendChartRef.value)

    const dates = trendData.value.map((d: any) => d.date || '')
    const minutes = trendData.value.map((d: any) => d.minutes || 0)

    // 如果所有数据都是0，显示提示
    const hasData = minutes.some((m: number) => m > 0)

    trendChart.setOption({
      tooltip: {
        trigger: 'axis',
        formatter: (params: any) => {
          const p = params[0]
          const h = Math.floor(p.value / 60)
          const m = p.value % 60
          const timeStr = h > 0 ? `${h}小时${m}分钟` : `${m}分钟`
          return `${p.axisValue}<br/>学习时长：<b>${timeStr}</b>`
        }
      },
      grid: { left: '3%', right: '4%', bottom: '3%', top: '15%', containLabel: true },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: dates,
        axisLine: { lineStyle: { color: '#DCDFE6' } },
        axisLabel: { color: '#606266' }
      },
      yAxis: {
        type: 'value',
        name: '分钟',
        nameTextStyle: { color: '#909399' },
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { lineStyle: { color: '#EBEEF5' } },
        axisLabel: { color: '#606266' }
      },
      series: [{
        name: '学习时长',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: { color: '#409EFF', width: 3 },
        itemStyle: { color: '#409EFF', borderColor: '#fff', borderWidth: 2 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
          ])
        },
        data: minutes
      }],
      ...(hasData ? {} : {
        title: {
          text: '近7天无学习记录',
          left: 'center',
          top: '40%',
          textStyle: { color: '#909399', fontSize: 14, fontWeight: 'normal' }
        }
      })
    })
  } catch (e) {
    console.error('[Statistics] 趋势图初始化失败:', e)
  }
}

const initProgressChart = async () => {
  if (!progressChartRef.value || courseProgressData.value.length === 0) return

  try {
    const echarts = await import('echarts')
    if (progressChart) progressChart.dispose()
    progressChart = echarts.init(progressChartRef.value)

    const data = [...courseProgressData.value].reverse()
    const names = data.map((d: any) => d.courseName || '未知')
    const rates = data.map((d: any) => d.completionRate || 0)

    progressChart.setOption({
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        formatter: (params: any) => {
          const p = params[0]
          const item = data[p.dataIndex]
          return `<b>${p.name}</b><br/>完成率：<b>${p.value}%</b><br/>课时：${item?.completedLessons || 0}/${item?.totalLessons || 0}`
        }
      },
      grid: { left: '3%', right: '12%', bottom: '3%', top: '5%', containLabel: true },
      xAxis: {
        type: 'value',
        max: 100,
        axisLabel: { formatter: '{value}%', color: '#606266' },
        splitLine: { lineStyle: { color: '#EBEEF5' } }
      },
      yAxis: {
        type: 'category',
        data: names,
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: { color: '#303133', fontSize: 12 }
      },
      series: [{
        type: 'bar',
        barWidth: 18,
        data: rates.map((value: number) => ({
          value,
          itemStyle: {
            color: value >= 80 ? '#67C23A'
              : value >= 50 ? '#409EFF'
              : value >= 20 ? '#E6A23C'
              : '#F56C6C',
            borderRadius: [0, 9, 9, 0]
          }
        })),
        label: {
          show: true,
          position: 'right',
          formatter: '{c}%',
          color: '#606266',
          fontSize: 11
        }
      }]
    })
  } catch (e) {
    console.error('[Statistics] 进度图初始化失败:', e)
  }
}

const initRadarChart = async () => {
  if (!radarChartRef.value || radarData.value.length === 0) return

  try {
    const echarts = await import('echarts')
    if (radarChart) radarChart.dispose()
    radarChart = echarts.init(radarChartRef.value)

    const dimensions = radarData.value.map((d: any) => d.dimension || '')
    const scores = radarData.value.map((d: any) => d.score || 0)

    radarChart.setOption({
      tooltip: {
        trigger: 'item',
        formatter: (params: any) => {
          let result = `<b>${params.name}</b><br/>`
          radarData.value.forEach((item: any, idx: number) => {
            result += `${item.dimension}: ${params.value[idx]}%<br/>`
          })
          return result
        }
      },
      radar: {
        indicator: dimensions.map((name: string) => ({ name, max: 100 })),
        shape: 'polygon',
        splitNumber: 4,
        axisName: { color: '#303133', fontSize: 12 },
        splitLine: { lineStyle: { color: '#DCDFE6' } },
        splitArea: { areaStyle: { color: ['#f5f7fa', '#fff', '#f5f7fa', '#fff'] } },
        axisLine: { lineStyle: { color: '#DCDFE6' } }
      },
      series: [{
        type: 'radar',
        data: [{
          value: scores,
          name: '能力值',
          symbol: 'circle',
          symbolSize: 8,
          lineStyle: { color: '#409EFF', width: 2 },
          itemStyle: { color: '#409EFF', borderColor: '#fff', borderWidth: 2 },
          areaStyle: { color: 'rgba(64, 158, 255, 0.2)' }
        }]
      }]
    })
  } catch (e) {
    console.error('[Statistics] 雷达图初始化失败:', e)
  }
}

// ==================== 窗口缩放 ====================
const handleResize = () => {
  trendChart?.resize()
  progressChart?.resize()
  radarChart?.resize()
}

// ==================== 生命周期 ====================
onMounted(async () => {
  console.log('[Statistics] ========== 开始学习统计页面数据加载 ==========')

  if (useMock.value) {
    // ===== 使用 Mock 数据（答辩演示）=====
    console.log('[Statistics] 当前使用 Mock 数据')
    summary.value = {
      totalHours: mockStats.totalHours,
      studyingCount: mockStats.studyingCount,
      completedCount: mockStats.completedCount,
      avgCompletionRate: mockStats.avgCompletionRate
    }
    trendData.value = mockStats.weeklyTrend
    courseProgressData.value = mockStats.courseProgress
    radarData.value = mockStats.radarData
  } else {
    // ===== 走真实接口 =====
    await Promise.all([
      fetchOverview(),
      fetchTrend(),
      fetchCourseProgress(),
      fetchRadar()
    ])
  }

  console.log('[Statistics] 概览:', summary.value)
  console.log('[Statistics] 趋势条数:', trendData.value.length)
  console.log('[Statistics] 课程进度条数:', courseProgressData.value.length)
  console.log('[Statistics] 雷达条数:', radarData.value.length)

  nextTick(() => {
    initTrendChart()
    initProgressChart()
    initRadarChart()
  })

  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  progressChart?.dispose()
  radarChart?.dispose()
})
</script>

<style scoped>
.statistics-container {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.statistics-container h2 {
  margin-bottom: 20px;
  font-size: 20px;
  color: #303133;
}

/* ==================== 统计卡片 ==================== */
.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 16px;
  border-radius: 8px;
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  margin-right: 14px;
  flex-shrink: 0;
}

.stat-icon.blue { background-color: #409EFF; }
.stat-icon.green { background-color: #67C23A; }
.stat-icon.orange { background-color: #E6A23C; }
.stat-icon.red { background-color: #F56C6C; }

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

/* ==================== 图表卡片 ==================== */
.charts-row {
  margin-bottom: 20px;
}

.chart-card {
  margin-bottom: 20px;
  border-radius: 8px;
}

.chart-card :deep(.el-card__header) {
  padding: 14px 20px;
  border-bottom: 1px solid #EBEEF5;
}

.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.chart-header .chart-desc {
  font-size: 12px;
  color: #909399;
  font-weight: 400;
  margin-left: auto;
  margin-right: 8px;
}

.chart-container {
  height: 300px;
}

/* ==================== 空状态 ==================== */
.empty-chart {
  height: 300px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.empty-chart p {
  margin: 12px 0 4px;
  font-size: 14px;
  color: #606266;
}

.empty-chart .sub-text {
  font-size: 12px;
  color: #C0C4CC;
}

/* ==================== 响应式 ==================== */
@media (max-width: 768px) {
  .statistics-container {
    padding: 12px;
  }

  .stat-card {
    padding: 12px;
    margin-bottom: 12px;
  }

  .stat-icon {
    width: 44px;
    height: 44px;
    margin-right: 10px;
  }

  .stat-value {
    font-size: 20px;
  }

  .chart-container,
  .empty-chart {
    height: 240px;
  }
}
</style>