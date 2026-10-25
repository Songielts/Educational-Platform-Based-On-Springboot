<template>
  <div class="lesson-questions-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <el-button link @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <h2>课时题目管理</h2>
      </div>
      <el-tag type="info">{{ courseName }} - {{ lessonName }}</el-tag>
    </div>

    <el-row :gutter="20">
      <!-- 左侧：已关联题目 -->
      <el-col :span="12">
        <div class="section-card">
          <div class="section-header">
            <h3>已关联题目</h3>
            <el-tag type="success">{{ linkedQuestions.length }} 题</el-tag>
          </div>

          <el-empty v-if="!linkedLoading && linkedQuestions.length === 0" description="暂无题目，请从右侧添加" :image-size="80" />

          <div v-loading="linkedLoading" class="question-list">
            <el-card
              v-for="(item, index) in linkedQuestions"
              :key="item.id"
              class="question-card"
              shadow="hover"
            >
              <div class="question-header-row">
                <div class="question-meta">
                  <span class="question-number">{{ index + 1 }}</span>
                  <el-tag :type="getQuestionTypeTag(item.questionType)" size="small">
                    {{ getQuestionTypeName(item.questionType) }}
                  </el-tag>
                  <span class="question-score">{{ item.score || 1 }}分</span>
                </div>
                <el-button
                  type="danger"
                  link
                  size="small"
                  :icon="Delete"
                  @click="handleRemove(item.id)"
                >移除</el-button>
              </div>
              <p class="question-content">{{ item.questionContent }}</p>
              <div class="options-list" v-if="item.questionOptions && parseOptions(item.questionOptions).length > 0">
                <div
                  v-for="(opt, idx) in parseOptions(item.questionOptions)"
                  :key="idx"
                  class="option-item"
                  :class="{ 'correct-option': isCorrectOption(item, idx) }"
                >
                  <span class="option-label">{{ optLabel(idx) }}</span>
                  <span class="option-content">{{ opt.optionContent || opt }}</span>
                </div>
              </div>
              <div class="answer-row">
                <span class="answer-label">答案：</span>
                <el-tag type="success" size="small">{{ item.correctAnswer }}</el-tag>
              </div>
            </el-card>
          </div>
        </div>
      </el-col>

      <!-- 右侧：题库题目 -->
      <el-col :span="12">
        <div class="section-card">
          <div class="section-header">
            <h3>题库题目</h3>
            <div class="header-actions">
              <el-input
                v-model="searchKeyword"
                placeholder="搜索题目"
                size="small"
                clearable
                @keyup.enter="fetchBankQuestions"
                style="width: 180px"
              />
              <el-button type="primary" size="small" :icon="Search" @click="fetchBankQuestions">搜索</el-button>
            </div>
          </div>

          <!-- 筛选条件 -->
          <div class="filter-bar">
            <el-select v-model="filterType" placeholder="题型" size="small" clearable style="width: 120px">
              <el-option label="单选题" :value="1" />
              <el-option label="多选题" :value="2" />
              <el-option label="判断题" :value="3" />
              <el-option label="填空题" :value="4" />
              <el-option label="简答题" :value="5" />
            </el-select>
            <el-select v-model="filterDifficulty" placeholder="难度" size="small" clearable style="width: 100px">
              <el-option label="简单" :value="1" />
              <el-option label="中等" :value="2" />
              <el-option label="困难" :value="3" />
            </el-select>
            <el-button type="primary" size="small" @click="fetchBankQuestions">筛选</el-button>
          </div>

          <el-empty v-if="!bankLoading && bankQuestions.length === 0" description="暂无可用题目" :image-size="80" />

          <div v-loading="bankLoading" class="question-list">
            <el-card
              v-for="item in bankQuestions"
              :key="item.id"
              class="question-card"
              shadow="hover"
            >
              <div class="question-header-row">
                <div class="question-meta">
                  <el-tag :type="getQuestionTypeTag(item.questionType)" size="small">
                    {{ getQuestionTypeName(item.questionType) }}
                  </el-tag>
                  <el-tag v-if="isLinked(item.id)" type="success" size="small">已添加</el-tag>
                </div>
                <el-button
                  type="primary"
                  size="small"
                  :icon="Plus"
                  @click="handleAdd(item.id)"
                  :disabled="isLinked(item.id)"
                >{{ isLinked(item.id) ? '已添加' : '添加' }}</el-button>
              </div>
              <p class="question-content">{{ item.questionContent }}</p>
              <div class="options-list" v-if="item.questionOptions && parseOptions(item.questionOptions).length > 0">
                <div
                  v-for="(opt, idx) in parseOptions(item.questionOptions)"
                  :key="idx"
                  class="option-item"
                >
                  <span class="option-label">{{ optLabel(idx) }}</span>
                  <span class="option-content">{{ opt.optionContent || opt }}</span>
                </div>
              </div>
            </el-card>
          </div>

          <!-- 分页 -->
          <div class="pagination-bar" v-if="bankTotal > 0">
            <el-pagination
              v-model:current-page="pageCurrent"
              v-model:page-size="pageSize"
              :total="bankTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              @size-change="fetchBankQuestions"
              @current-change="fetchBankQuestions"
            />
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft,
  Delete,
  Plus,
  Search
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getLessonQuestions, addQuestionToLesson, removeQuestionFromLesson } from '@/api/study'
import { getQuestionList } from '@/api/exam'

const route = useRoute()
const router = useRouter()
const lessonId = Number(route.query.lessonId)
const courseId = Number(route.query.courseId)
const courseName = ref(route.query.courseName as string || '')
const lessonName = ref(route.query.lessonName as string || '')

// 已关联题目
const linkedLoading = ref(false)
const linkedQuestions = ref<any[]>([])

// 题库题目
const bankLoading = ref(false)
const bankQuestions = ref<any[]>([])
const bankTotal = ref(0)
const pageCurrent = ref(1)
const pageSize = ref(10)
const searchKeyword = ref('')
const filterType = ref<number | undefined>(undefined)
const filterDifficulty = ref<number | undefined>(undefined)

const parseRes = (res: any): any => {
  if (!res) return null
  if (res.data && res.data.code === 200) return res.data.data !== null ? res.data.data : {}
  if (typeof res.code === 'number' && res.code === 200) return res.data !== null ? res.data : {}
  return null
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

const fetchLinkedQuestions = async () => {
  if (!lessonId || isNaN(lessonId)) return
  linkedLoading.value = true
  try {
    const res = await getLessonQuestions(lessonId) as any
    const data = parseRes(res)
    if (Array.isArray(data)) {
      linkedQuestions.value = data.map((q: any) => ({
        ...q,
        questionOptions: parseOptions(q.questionOptions)
      }))
    } else {
      linkedQuestions.value = []
    }
  } catch (e: any) {
    console.error('获取已关联题目失败:', e)
    ElMessage.error('获取已关联题目失败')
  } finally {
    linkedLoading.value = false
  }
}

const fetchBankQuestions = async () => {
  bankLoading.value = true
  try {
    const params: any = {
      current: pageCurrent.value,
      size: pageSize.value
    }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (filterType.value !== undefined) params.questionType = filterType.value
    if (filterDifficulty.value !== undefined) params.difficultyLevel = filterDifficulty.value

    const res = await getQuestionList(params) as any
    const data = parseRes(res)
    if (data?.records && Array.isArray(data.records)) {
      bankQuestions.value = data.records.map((q: any) => ({
        ...q,
        questionOptions: parseOptions(q.questionOptions)
      }))
      bankTotal.value = data.total || 0
    } else if (Array.isArray(data)) {
      bankQuestions.value = data.map((q: any) => ({
        ...q,
        questionOptions: parseOptions(q.questionOptions)
      }))
      bankTotal.value = data.length
    } else {
      bankQuestions.value = []
      bankTotal.value = 0
    }
  } catch (e: any) {
    console.error('获取题库题目失败:', e)
    ElMessage.error('获取题库题目失败')
  } finally {
    bankLoading.value = false
  }
}

const handleAdd = async (questionId: number) => {
  try {
    const order = linkedQuestions.value.length + 1
    const res = await addQuestionToLesson(lessonId, questionId, order)
    if (isSuccess(res)) {
      ElMessage.success('添加成功')
      fetchLinkedQuestions()
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '添加失败')
  }
}

const handleRemove = async (questionId: number) => {
  try {
    const res = await removeQuestionFromLesson(lessonId, questionId)
    if (isSuccess(res)) {
      ElMessage.success('移除成功')
      fetchLinkedQuestions()
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '移除失败')
  }
}

const isLinked = (questionId: number): boolean => {
  return linkedQuestions.value.some((q: any) => q.id === questionId)
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
  return names[type] || '未知'
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

const isCorrectOption = (item: any, optionIndex: number): boolean => {
  if (!item.correctAnswer) return false
  const label = optLabel(optionIndex)
  return item.correctAnswer.split(',').map((a: string) => a.trim()).includes(label)
}

const goBack = () => {
  if (courseId && !isNaN(courseId)) {
    router.push(`/course/${courseId}`)
  } else {
    router.back()
  }
}

onMounted(() => {
  if (!lessonId || isNaN(lessonId)) {
    ElMessage.error('课时ID无效')
    return
  }
  fetchLinkedQuestions()
  fetchBankQuestions()
})
</script>

<style scoped>
.lesson-questions-container {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-left h2 {
  margin: 0;
}

.section-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  min-height: 600px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.section-header h3 {
  margin: 0;
  font-size: 16px;
}

.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.filter-bar {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 12px;
}

.question-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 70vh;
  overflow-y: auto;
  padding-right: 4px;
}

.question-card {
  border-radius: 6px;
}

.question-card :deep(.el-card__body) {
  padding: 12px;
}

.question-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.question-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.question-number {
  font-weight: 600;
  color: #409EFF;
  min-width: 24px;
}

.question-score {
  color: #f56c6c;
  font-size: 13px;
}

.question-content {
  font-size: 14px;
  line-height: 1.6;
  margin: 0 0 8px 0;
  color: #303133;
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 8px;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border-radius: 4px;
  background: #f5f7fa;
  font-size: 13px;
}

.option-label {
  font-weight: 600;
  color: #606266;
  min-width: 20px;
}

.option-content {
  flex: 1;
  color: #303133;
}

.correct-option {
  background: #f0f9eb;
  border: 1px solid #b3e19d;
}

.answer-row {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.answer-label {
  color: #606266;
}

.pagination-bar {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}
</style>