<template>
  <div class="wrong-questions-container">
    <div class="page-header">
      <h2>
        <el-icon><DocumentDelete /></el-icon>
        错题本
      </h2>
      <el-tag type="info">共 {{ wrongQuestionList.length }} 道错题</el-tag>
    </div>

    <el-empty v-if="!loading && wrongQuestionList.length === 0" description="暂无错题，继续保持！" :image-size="120" />

    <div v-loading="loading" class="question-list">
      <el-card
        v-for="(item, index) in wrongQuestionList"
        :key="item.id || item.questionId"
        class="question-card"
        shadow="hover"
      >
        <div class="question-header">
          <div class="question-meta">
            <span class="question-number">第 {{ index + 1 }} 题</span>
            <el-tag :type="getQuestionTypeTag(item.questionType)" size="small">
              {{ getQuestionTypeName(item.questionType) }}
            </el-tag>
            <el-tag v-if="item.examName || item.sourceType === 1" type="warning" size="small">考试</el-tag>
            <el-tag v-else-if="item.lessonName || item.sourceType === 2" type="success" size="small">练习</el-tag>
          </div>
          <el-button
            type="danger"
            link
            size="small"
            :icon="Delete"
            @click="handleRemove(item.questionId || item.id)"
          >移除</el-button>
        </div>

        <p class="question-content">{{ item.questionContent }}</p>

        <!-- 选项展示 -->
        <div class="options-list" v-if="item.questionOptions && item.questionOptions.length > 0">
          <div
            v-for="(opt, idx) in parseOptions(item.questionOptions)"
            :key="idx"
            class="option-item"
            :class="{
              'correct-option': isCorrectOption(item, idx),
              'wrong-option': isWrongOption(item, idx)
            }"
          >
            <span class="option-label">{{ optLabel(idx) }}</span>
            <span class="option-content">{{ getOptionText(opt) }}</span>
            <el-icon v-if="isCorrectOption(item, idx)" class="option-icon correct"><CircleCheck /></el-icon>
            <el-icon v-if="isWrongOption(item, idx)" class="option-icon wrong"><CircleClose /></el-icon>
          </div>
        </div>

        <!-- 判断题特殊展示 -->
        <div class="options-list" v-else-if="item.questionType === 3">
          <div
            class="option-item"
            :class="{
              'correct-option': item.correctAnswer === 'A' || item.correctAnswer === '正确',
              'wrong-option': item.studentAnswer === 'A' && item.correctAnswer !== 'A'
            }"
          >
            <span class="option-label">A</span>
            <span class="option-content">正确</span>
          </div>
          <div
            class="option-item"
            :class="{
              'correct-option': item.correctAnswer === 'B' || item.correctAnswer === '错误',
              'wrong-option': item.studentAnswer === 'B' && item.correctAnswer !== 'B'
            }"
          >
            <span class="option-label">B</span>
            <span class="option-content">错误</span>
          </div>
        </div>

        <!-- 答案区域 -->
        <div class="answer-section">
          <div class="answer-row">
            <span class="answer-label">你的答案：</span>
            <el-tag :type="'danger'" size="small">{{ formatAnswer(item.studentAnswer) || '未作答' }}</el-tag>
          </div>
          <div class="answer-row">
            <span class="answer-label">正确答案：</span>
            <el-tag :type="'success'" size="small">{{ formatAnswer(item.correctAnswer) }}</el-tag>
          </div>
        </div>

        <!-- 解析 -->
        <div class="analysis-section" v-if="item.answerAnalysis">
          <el-divider content-position="left">答案解析</el-divider>
          <p class="analysis-content">{{ item.answerAnalysis }}</p>
        </div>

        <!-- 来源信息 -->
        <div class="source-info" v-if="item.sourceName || item.lessonName || item.examName">
          <el-divider content-position="left">来源</el-divider>
          <p class="source-text" v-if="item.examName">
            <el-icon><Document /></el-icon>
            考试：{{ item.examName }}
          </p>
          <p class="source-text" v-if="item.lessonName">
            <el-icon><VideoPlay /></el-icon>
            课时：{{ item.lessonName }}
          </p>
          <p class="source-text" v-if="item.courseName">
            <el-icon><Reading /></el-icon>
            课程：{{ item.courseName }}
          </p>
        </div>
      </el-card>
    </div>

    <!-- 批量操作 -->
    <div class="batch-actions" v-if="wrongQuestionList.length > 0">
      <el-popconfirm
        title="确定要清空所有错题吗？"
        confirm-button-text="确定"
        cancel-button-text="取消"
        @confirm="handleClearAll"
      >
        <template #reference>
          <el-button type="danger" :icon="Delete">清空错题本</el-button>
        </template>
      </el-popconfirm>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  DocumentDelete,
  Delete,
  CircleCheck,
  CircleClose,
  Document,
  VideoPlay,
  Reading
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getWrongQuestions, removeWrongQuestion } from '@/api/study'

const loading = ref(false)
const wrongQuestionList = ref<any[]>([])

const parseRes = (res: any): any => {
  if (!res) return null
  if (res.data && res.data.code === 200) return res.data.data !== null ? res.data.data : []
  if (typeof res.code === 'number' && res.code === 200) return res.data !== null ? res.data : []
  if (Array.isArray(res)) return res
  return []
}
const isSuccess = (res: any): boolean => {
  if (!res) return false
  if (res.data && res.data.code === 200) return true
  if (typeof res.code === 'number' && res.code === 200) return true
  return false
}

// 将 v-for 的 string|number 索引安全转为 number
const n = (val: string | number): number => typeof val === 'number' ? val : parseInt(val, 10)
const optLabel = (val: string | number): string => {
  const labels = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H']
  return labels[n(val)] || String(val)
}

const fetchWrongQuestions = async () => {
  loading.value = true
  try {
    const res = await getWrongQuestions() as any
    const data = parseRes(res)
    if (Array.isArray(data)) {
      wrongQuestionList.value = data.map((item: any) => ({
        ...item,
        questionOptions: parseOptions(item.questionOptions)
      }))
    } else {
      wrongQuestionList.value = []
    }
  } catch (e: any) {
    console.error('获取错题本失败:', e)
    ElMessage.error('获取错题本失败')
    wrongQuestionList.value = []
  } finally {
    loading.value = false
  }
}

const parseOptions = (options: any): any[] => {
  if (!options) return []
  if (Array.isArray(options)) return options
  if (typeof options === 'string') {
    try {
      const parsed = JSON.parse(options)
      return Array.isArray(parsed) ? parsed : []
    } catch {
      return []
    }
  }
  return []
}

const getQuestionTypeName = (type: number): string => {
  const names: Record<number, string> = {
    1: '单选题',
    2: '多选题',
    3: '判断题',
    4: '填空题',
    5: '简答题'
  }
  return names[type] || '未知题型'
}

const getQuestionTypeTag = (type: number): string => {
  const tags: Record<number, string> = {
    1: 'primary',
    2: 'success',
    3: 'warning',
    4: 'info',
    5: ''
  }
  return tags[type] || ''
}

const formatAnswer = (answer: string | undefined): string => {
  if (!answer) return ''
  return answer
}

/**
 * 提取选项显示文本（健壮版，支持多种字段名）
 */
const getOptionText = (opt: any): string => {
  if (!opt) return ''
  if (typeof opt === 'string') return opt
  return opt.value || opt.optionContent || opt.content || opt.text || opt.label || opt.optionText || opt.title || opt.optionValue || opt.name || opt.description || JSON.stringify(opt)
}

const isCorrectOption = (item: any, optionIndex: number): boolean => {
  if (!item.correctAnswer) return false
  const label = optLabel(optionIndex)
  const correctAnswers = String(item.correctAnswer).split(',').map((a: string) => a.trim())
  return correctAnswers.includes(label)
}

const isWrongOption = (item: any, optionIndex: number): boolean => {
  if (!item.studentAnswer) return false
  const label = optLabel(optionIndex)
  const studentAnswers = String(item.studentAnswer).split(',').map((a: string) => a.trim())
  const correctAnswers = item.correctAnswer
    ? String(item.correctAnswer).split(',').map((a: string) => a.trim())
    : []
  return studentAnswers.includes(label) && !correctAnswers.includes(label)
}

const handleRemove = async (questionId: number) => {
  try {
    await ElMessageBox.confirm('确定要从错题本中移除这道题吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const res = await removeWrongQuestion(questionId)
    if (isSuccess(res)) {
      ElMessage.success('已移除')
      wrongQuestionList.value = wrongQuestionList.value.filter(
        (item) => (item.questionId || item.id) !== questionId
      )
    }
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.message || '移除失败')
    }
  }
}

const handleClearAll = async () => {
  try {
    const removePromises = wrongQuestionList.value.map((item) =>
      removeWrongQuestion(item.questionId || item.id)
    )
    await Promise.all(removePromises)
    ElMessage.success('已清空所有错题')
    wrongQuestionList.value = []
  } catch (e: any) {
    ElMessage.error('清空失败')
  }
}

onMounted(() => {
  fetchWrongQuestions()
})
</script>

<style scoped>
.wrong-questions-container {
  padding: 20px;
  max-width: 1000px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.question-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.question-card {
  border-radius: 8px;
}

.question-card :deep(.el-card__body) {
  padding: 16px;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.question-meta {
  display: flex;
  align-items: center;
  gap: 10px;
}

.question-number {
  font-weight: 600;
  font-size: 15px;
  color: #303133;
}

.question-content {
  font-size: 15px;
  line-height: 1.8;
  color: #303133;
  margin: 0 0 16px 0;
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 6px;
  background: #f5f7fa;
  border: 1px solid transparent;
  transition: all 0.2s;
}

.option-label {
  font-weight: 600;
  color: #606266;
  min-width: 24px;
}

.option-content {
  flex: 1;
  color: #303133;
}

.option-icon {
  font-size: 16px;
}

.option-icon.correct {
  color: #67c23a;
}

.option-icon.wrong {
  color: #f56c6c;
}

.correct-option {
  background: #f0f9eb;
  border-color: #b3e19d;
}

.wrong-option {
  background: #fef0f0;
  border-color: #fbc4c4;
}

.answer-section {
  display: flex;
  gap: 20px;
  margin-bottom: 12px;
  padding: 10px;
  background: #f5f7fa;
  border-radius: 6px;
}

.answer-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.answer-label {
  color: #606266;
  font-size: 14px;
}

.analysis-section {
  margin-top: 12px;
}

.analysis-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin: 0;
}

.source-info {
  margin-top: 12px;
}

.source-text {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #909399;
  margin: 4px 0;
}

.batch-actions {
  margin-top: 20px;
  text-align: center;
  padding: 20px;
}
</style>