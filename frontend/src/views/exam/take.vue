<template>
  <!-- 考试结果展示 -->
  <div class="exam-result-container" v-if="result">
    <el-result icon="success" title="考试提交成功" sub-title="您的考试已完成，以下是成绩详情">
      <template #extra>
        <el-button type="primary" @click="router.push('/exams')">返回考试中心</el-button>
      </template>
    </el-result>

    <el-card class="result-card">
      <template #header>
        <div class="result-header">
          <span>{{ exam?.examName || '考试结果' }}</span>
          <el-tag :type="resultTagType">{{ result.statusName }}</el-tag>
        </div>
      </template>

      <div class="result-score-section">
        <div class="score-circle">
          <span class="score-num">{{ result.obtainScore }}</span>
          <span class="score-total">/ {{ result.totalScore }}</span>
        </div>
        <div class="score-rate">正确率：{{ result.correctRate }}%</div>
      </div>

      <el-divider />

      <div class="result-stats">
        <div class="stat-item">
          <div class="stat-label">答对</div>
          <div class="stat-value correct">{{ result.correctCount }}</div>
        </div>
        <div class="stat-item">
          <div class="stat-label">答错</div>
          <div class="stat-value wrong">{{ result.wrongCount }}</div>
        </div>
        <div class="stat-item">
          <div class="stat-label">用时</div>
          <div class="stat-value">{{ result.useTimeStr || '-' }}</div>
        </div>
        <div class="stat-item">
          <div class="stat-label">得分</div>
          <div class="stat-value score">{{ result.obtainScore }}分</div>
        </div>
      </div>
    </el-card>
  </div>

  <!-- 考试页面 -->
  <div class="exam-taking-container" v-else-if="exam">
    <div class="exam-header">
      <h2>{{ exam.examName }}</h2>
      <div class="exam-timer">
        <el-icon><Timer /></el-icon>
        <span :class="{ 'warning': remainingTime < 300 }">
          剩余时间: {{ formatTime(remainingTime) }}
        </span>
      </div>
    </div>

    <div class="exam-info">
      <el-tag>{{ exam.examTypeName }}</el-tag>
      <el-tag type="info">共 {{ exam.questions?.length || 0 }} 题</el-tag>
      <el-tag type="success">总分 {{ exam.totalScore }} 分</el-tag>
      <el-tag type="warning">限时 {{ exam.timeLimit }} 分钟</el-tag>
    </div>

    <!-- 题目列表 -->
    <div class="question-list">
      <div
        v-for="(q, index) in exam.questions"
        :key="q.id"
        class="question-card"
      >
        <div class="question-header">
          <span class="question-number">第{{ (index as number) + 1 }}题</span>
          <el-tag size="small" type="info">{{ q.questionTypeName }}</el-tag>
          <span class="question-score">({{ q.score }}分)</span>
        </div>

        <!-- 题目内容 -->
        <div class="question-content">{{ q.questionContent }}</div>

        <!-- 单选题 -->
        <el-radio-group
          v-if="q.questionType === 1"
          v-model="answers[q.questionId]"
          class="options-group"
        >
          <el-radio
            v-for="opt in q.questionOptions"
            :key="opt.key"
            :value="opt.key"
          >
            {{ opt.key }}. {{ opt.value }}
          </el-radio>
        </el-radio-group>

        <!-- 多选题 -->
        <el-checkbox-group
          v-else-if="q.questionType === 2"
          v-model="answersMulti[q.questionId]"
          class="options-group"
        >
          <el-checkbox
            v-for="opt in q.questionOptions"
            :key="opt.key"
            :value="opt.key"
          >
            {{ opt.key }}. {{ opt.value }}
          </el-checkbox>
        </el-checkbox-group>

        <!-- 判断题 -->
        <el-radio-group
          v-else-if="q.questionType === 3"
          v-model="answers[q.questionId]"
          class="options-group"
        >
          <el-radio value="A">正确</el-radio>
          <el-radio value="B">错误</el-radio>
        </el-radio-group>

        <!-- 填空题 -->
        <el-input
          v-else-if="q.questionType === 4"
          v-model="answers[q.questionId]"
          type="textarea"
          :rows="3"
          placeholder="请输入答案"
        />
      </div>
    </div>

    <!-- 提交按钮 -->
    <div class="submit-section">
      <el-button type="primary" size="large" @click="submitExam" :loading="submitting">
        提交试卷
      </el-button>
    </div>
  </div>

  <!-- 加载状态 -->
  <div v-else v-loading="loading" class="loading-container">
    <el-empty v-if="!loading" description="试卷加载失败" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Timer } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getExamDetail, startExam as apiStartExam, submitExam as apiSubmitExam } from '@/api/exam'

const route = useRoute()
const router = useRouter()
const examId = Number(route.params.id)

const loading = ref(false)
const submitting = ref(false)
const exam = ref<any>(null)
const result = ref<any>(null)
const resultTagType = ref('success')
const answers = reactive<Record<number, string>>({})
const answersMulti = reactive<Record<number, string[]>>({})
const remainingTime = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

// ==================== 统一解析响应 ====================
const parseRes = (res: any): any => {
  if (!res) return null
  if (res.data && typeof res.data.code === 'number' && res.data.code === 200) return res.data.data
  if (typeof res.code === 'number' && res.code === 200) return res.data
  if (Array.isArray(res) || (typeof res === 'object' && res !== null && !res.code)) return res
  return null
}

const fetchExam = async () => {
  loading.value = true
  try {
    // 先调用 startExam 创建考试记录
    try {
      await apiStartExam(examId)
    } catch (startErr: any) {
      console.log('[take] startExam:', startErr?.message)
    }

    const res = await getExamDetail(examId) as any
    const data = parseRes(res)
    if (data) {
      exam.value = data
      // 初始化多选题答案
      if (data.questions) {
        for (const q of data.questions) {
          if (q.questionType === 2) {
            answersMulti[q.questionId] = []
          }
        }
      }
      // 设置倒计时
      if (data.timeLimit) {
        remainingTime.value = data.timeLimit * 60
        startTimer()
      }
    } else {
      ElMessage.error('试卷加载失败')
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '系统错误')
  } finally {
    loading.value = false
  }
}

const startTimer = () => {
  timer = setInterval(() => {
    remainingTime.value--
    if (remainingTime.value <= 0) {
      clearInterval(timer!)
      ElMessage.warning('考试时间已到，自动提交')
      handleSubmit()
    }
  }, 1000)
}

const formatTime = (seconds: number) => {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  if (h > 0) {
    return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  }
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

const submitExam = () => {
  ElMessageBox.confirm('确定要提交试卷吗？提交后不可修改', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    handleSubmit()
  }).catch(() => {})
}

const handleSubmit = async () => {
  if (timer) clearInterval(timer)

  const submitData: Record<number, string> = { ...answers }
  for (const [qid, vals] of Object.entries(answersMulti)) {
    submitData[Number(qid)] = (vals as string[]).sort().join(',')
  }

  submitting.value = true
  try {
    const res = await apiSubmitExam(examId, submitData)
    const data = parseRes(res)
    if (data) {
      result.value = data
      const rate = data.totalScore > 0 ? (data.obtainScore / data.totalScore * 100) : 0
      result.value.correctRate = Math.round(rate)
      resultTagType.value = rate >= 60 ? 'success' : 'danger'
      ElMessage.success('提交成功！成绩已公布')
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  if (!examId || isNaN(examId)) {
    ElMessage.error('试卷ID无效')
    return
  }
  fetchExam()
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.exam-taking-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.exam-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.exam-header h2 {
  margin: 0;
}

.exam-timer {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  color: #f56c6c;
}

.exam-timer .warning {
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.exam-info {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.question-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.question-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.05);
}

.question-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.question-number {
  font-weight: 600;
  color: #409EFF;
}

.question-score {
  margin-left: auto;
  color: #f56c6c;
}

.question-content {
  font-size: 15px;
  line-height: 1.6;
  margin-bottom: 16px;
  color: #303133;
  white-space: pre-wrap;
}

.options-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: flex-start;
}

/* ===== 选项左对齐：覆盖 Element Plus 默认居中 ===== */
.options-group :deep(.el-radio),
.options-group :deep(.el-checkbox) {
  justify-content: flex-start;
  width: 100%;
  margin-right: 0;
}

.options-group :deep(.el-radio__input),
.options-group :deep(.el-checkbox__input) {
  flex-shrink: 0;
}

.submit-section {
  display: flex;
  justify-content: center;
  margin-top: 30px;
  padding-bottom: 40px;
}

/* 考试结果样式 */
.exam-result-container {
  max-width: 700px;
  margin: 0 auto;
  padding: 20px;
}

.result-card {
  margin-top: 20px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: 600;
}

.result-score-section {
  text-align: center;
  padding: 30px 0;
}

.score-circle {
  display: inline-flex;
  align-items: baseline;
  gap: 4px;
}

.score-num {
  font-size: 56px;
  font-weight: 700;
  color: #409EFF;
  line-height: 1;
}

.score-total {
  font-size: 24px;
  color: #909399;
}

.score-rate {
  margin-top: 12px;
  font-size: 16px;
  color: #606266;
}

.result-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  padding: 10px 0;
}

.stat-item {
  text-align: center;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}

.stat-value.correct {
  color: #67C23A;
}

.stat-value.wrong {
  color: #F56C6C;
}

.stat-value.score {
  color: #409EFF;
}

.loading-container {
  padding: 100px 20px;
  text-align: center;
}
</style>