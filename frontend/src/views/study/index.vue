<template>
  <div class="study-container">
    <el-row :gutter="20" class="video-ai-section">
      <el-col :span="16">
        <div class="video-section">
          <div ref="dplayerRef" class="video-player"></div>
        </div>
      </el-col>
      <el-col :span="8">
        <AIChat v-if="lesson?.courseId" :course-id="lesson.courseId" :lesson-id="lessonId" :lesson-name="lesson.lessonName" />
        <el-empty v-else description="加载中..." />
      </el-col>
    </el-row>

    <div class="lesson-info-section">
      <h2>{{ lesson?.lessonName }}</h2>
      <div class="lesson-actions">
        <el-button :icon="Notebook" @click="showNoteDialog = true">记笔记</el-button>
        <el-button :icon="QuestionFilled" @click="showQuestionDialog = true">提问</el-button>
        <el-button :icon="Star" @click="toggleFavorite">收藏</el-button>
        <el-button type="success" :icon="Document" @click="openPractice" :loading="practiceLoading">课时练习</el-button>
        <el-button type="warning" :icon="CircleClose" @click="router.push('/wrong-questions')">错题本</el-button>
      </div>
    </div>

    <div class="questions-section">
      <div class="questions-header">
        <h3>
          <el-icon><Document /></el-icon>
          课时练习
          <el-tag :type="lessonQuestions.length > 0 ? 'primary' : 'info'" size="small">{{ lessonQuestions.length }} 题</el-tag>
        </h3>
        <div class="header-actions">
          <el-button
            v-if="isTeacher"
            type="warning"
            size="small"
            @click="openImportDialog"
            :icon="Plus"
          >导入习题</el-button>
          <el-button type="primary" size="small" @click="openPractice" :disabled="lessonQuestions.length === 0" :icon="VideoPlay">
            {{ lessonQuestions.length > 0 ? '开始练习' : '暂无题目' }}
          </el-button>
        </div>
      </div>
      <div v-if="questionLoading" class="loading-hint"><el-icon class="is-loading"><Loading /></el-icon> 加载题目中...</div>
      <div v-else-if="lessonQuestions.length === 0" class="empty-hint">
        <el-icon :size="32"><Document /></el-icon>
        <p>该课时暂无题目</p>
        <p class="hint-text">请教师先在课时管理中导入题目</p>
      </div>
      <div v-else class="question-preview-list">
        <div v-for="(q, idx) in lessonQuestions" :key="q.id" class="question-preview-item" @click="openPracticeFrom(idx)">
          <div class="preview-header">
            <span class="preview-index">{{ n(idx) + 1 }}</span>
            <el-tag :type="getQuestionTypeTag(q.questionType)" size="small">{{ getQuestionTypeName(q.questionType) }}</el-tag>
            <span v-if="q.score" class="preview-score">{{ q.score }}分</span>
          </div>
          <div class="preview-content">{{ q.questionContent }}</div>
          <div v-if="q.questionOptions && q.questionOptions.length > 0" class="preview-options">
            <div v-for="(opt, oIdx) in q.questionOptions" :key="oIdx" class="preview-option">
              <span class="opt-key">{{ extractOptionKey(opt, n(oIdx)) }}.</span>
              <span class="opt-text">{{ getOptionDisplayText(opt) }}</span>
            </div>
          </div>
          <div v-else-if="q.questionType === 1 || q.questionType === 2 || q.questionType === 3" class="preview-no-options">
            <el-tag type="warning" size="small">题目未设置选项</el-tag>
          </div>
          <div class="preview-footer">
            <el-link type="primary" :icon="VideoPlay">点击练习</el-link>
          </div>
        </div>
      </div>
    </div>

    <el-card class="chapters-card">
      <template #header><span>课程目录</span></template>
      <div class="chapters-list">
        <div v-for="(chapter, cIdx) in chapters" :key="chapter.id" class="chapter-item">
          <div class="chapter-header">
            <span class="chapter-number">第{{ cIdx + 1 }}章</span>
            <span class="chapter-name">{{ chapter.chapterName }}</span>
          </div>
          <div class="lessons-list" v-if="chapter.lessons">
            <div v-for="(l, lIdx) in chapter.lessons" :key="l.id" class="lesson-item" :class="{ active: l.id === lessonId, completed: l.isCompleted }" @click="switchLesson(l.id)">
              <span class="lesson-number">{{ cIdx + 1 }}.{{ Number(lIdx) + 1 }}</span>
              <span class="lesson-name">{{ l.lessonName }}</span>
              <el-icon v-if="l.isCompleted" class="completed-icon"><CircleCheck /></el-icon>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 导入习题对话框 -->
    <el-dialog v-model="showImportDialog" title="从题库导入习题" width="800px" :close-on-click-modal="false">
      <div class="import-search-section">
        <el-input
          v-model="importKeyword"
          placeholder="输入关键字搜索题目内容"
          clearable
          style="width: 300px;"
          @keyup.enter="searchImportQuestions"
        >
          <template #append>
            <el-button :icon="Search" @click="searchImportQuestions">搜索</el-button>
          </template>
        </el-input>
        <el-select v-model="importQuestionType" placeholder="题型筛选" clearable style="width: 140px;" @change="searchImportQuestions">
          <el-option label="单选题" :value="1" />
          <el-option label="多选题" :value="2" />
          <el-option label="判断题" :value="3" />
          <el-option label="填空题" :value="4" />
          <el-option label="简答题" :value="5" />
        </el-select>
        <el-tag v-if="importSelectedIds.length > 0" type="success">已选 {{ importSelectedIds.length }} 题</el-tag>
      </div>

      <div v-loading="importLoading" class="import-question-list">
        <el-empty v-if="importQuestions.length === 0" description="请搜索题目" />
        <el-table
          v-else
          :data="importQuestions"
          @selection-change="handleImportSelectionChange"
          style="width: 100%"
          size="small"
          max-height="400"
        >
          <el-table-column type="selection" width="45" />
          <el-table-column label="题目内容" min-width="250">
            <template #default="{ row }">
              <div class="import-q-content">{{ row.questionContent }}</div>
            </template>
          </el-table-column>
          <el-table-column label="题型" width="80">
            <template #default="{ row }">
              <el-tag :type="getQuestionTypeTag(row.questionType)" size="small">{{ getQuestionTypeName(row.questionType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="难度" width="70">
            <template #default="{ row }">
              <el-tag v-if="row.difficultyLevel === 1" type="success" size="small">简单</el-tag>
              <el-tag v-else-if="row.difficultyLevel === 2" type="warning" size="small">中等</el-tag>
              <el-tag v-else-if="row.difficultyLevel === 3" type="danger" size="small">困难</el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <template #footer>
        <el-button @click="showImportDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmImport" :loading="importSubmitting" :disabled="importSelectedIds.length === 0">
          导入 {{ importSelectedIds.length > 0 ? `(${importSelectedIds.length}题)` : '' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showNoteDialog" title="学习笔记" width="600px">
      <el-form :model="noteForm">
        <el-form-item label="笔记标题"><el-input v-model="noteForm.noteTitle" placeholder="请输入笔记标题" /></el-form-item>
        <el-form-item label="笔记内容">
          <el-input v-model="noteForm.noteContent" type="textarea" :rows="6" placeholder="记录你的学习心得..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showNoteDialog = false">取消</el-button>
        <el-button type="primary" @click="saveNote">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showQuestionDialog" title="提问" width="600px">
      <el-form :model="questionForm">
        <el-form-item label="问题标题"><el-input v-model="questionForm.title" placeholder="请输入问题标题" /></el-form-item>
        <el-form-item label="问题内容">
          <el-input v-model="questionForm.content" type="textarea" :rows="6" placeholder="详细描述你的问题..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showQuestionDialog = false">取消</el-button>
        <el-button type="primary" @click="submitQuestion">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showPracticeDialog" title="课时练习" width="750px" :close-on-click-modal="false">
      <div v-loading="practiceLoading">
        <el-empty v-if="!practiceQuestions.length" description="暂无练习题目" />
        <div v-else class="practice-body">
          <div class="practice-progress-bar">
            <el-progress :percentage="Math.round(((currentQuestionIndex + 1) / practiceQuestions.length) * 100)" :format="() => `${currentQuestionIndex + 1} / ${practiceQuestions.length}`" />
          </div>
          <div class="question-card" v-if="currentQuestion">
            <div class="question-type-row">
              <el-tag :type="getQuestionTypeTag(currentQuestion.questionType)">{{ getQuestionTypeName(currentQuestion.questionType) }}</el-tag>
              <span class="score-tag">{{ currentQuestion.score || 1 }}分</span>
            </div>
            <p class="question-text">{{ currentQuestion.questionContent }}</p>
            <template v-if="currentQuestion.questionType === 1">
              <el-radio-group v-model="answers[currentQuestion.id]" class="option-list">
                <el-radio v-for="(opt, optIdx) in (currentQuestion.questionOptions || [])" :key="optIdx" :value="extractOptionKey(opt, n(optIdx))" class="option-row">
                  <span class="opt-label">{{ extractOptionKey(opt, n(optIdx)) }}.</span>
                  <span class="opt-content">{{ getOptionDisplayText(opt) }}</span>
                </el-radio>
              </el-radio-group>
            </template>
            <template v-else-if="currentQuestion.questionType === 2">
              <el-checkbox-group v-model="multiAnswers[currentQuestion.id]" class="option-list">
                <el-checkbox v-for="(opt, optIdx) in (currentQuestion.questionOptions || [])" :key="optIdx" :value="extractOptionKey(opt, n(optIdx))" class="option-row">
                  <span class="opt-label">{{ extractOptionKey(opt, n(optIdx)) }}.</span>
                  <span class="opt-content">{{ getOptionDisplayText(opt) }}</span>
                </el-checkbox>
              </el-checkbox-group>
            </template>
            <template v-else-if="currentQuestion.questionType === 3">
              <el-radio-group v-model="answers[currentQuestion.id]" class="option-list">
                <el-radio value="A" class="option-row"><span class="opt-label">A.</span><span class="opt-content">正确</span></el-radio>
                <el-radio value="B" class="option-row"><span class="opt-label">B.</span><span class="opt-content">错误</span></el-radio>
              </el-radio-group>
            </template>
            <template v-else-if="currentQuestion.questionType === 4">
              <el-input v-model="answers[currentQuestion.id]" type="textarea" :rows="3" placeholder="请输入答案，多个空用逗号分隔" />
            </template>
            <template v-else>
              <el-input v-model="answers[currentQuestion.id]" type="textarea" :rows="5" placeholder="请输入答案" />
            </template>
          </div>
          <div class="practice-nav">
            <el-button @click="currentQuestionIndex--" :disabled="currentQuestionIndex === 0">上一题</el-button>
            <el-button type="primary" @click="currentQuestionIndex++" v-if="currentQuestionIndex < practiceQuestions.length - 1">下一题</el-button>
            <el-button type="success" @click="submitPractice" v-if="currentQuestionIndex === practiceQuestions.length - 1" :loading="submitting">提交答案</el-button>
          </div>
          <div class="question-nav-dots">
            <span v-for="(_, idx) in practiceQuestions" :key="idx" class="nav-dot" :class="{ active: idx === currentQuestionIndex, answered: isAnswered(idx) }" @click="currentQuestionIndex = idx">{{ n(idx) + 1 }}</span>
          </div>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="showResultDialog" title="练习结果" width="700px" :close-on-click-modal="false">
      <div class="result-body">
        <div class="result-summary">
          <el-result
            :icon="resultData.score >= resultData.totalScore * 0.6 ? 'success' : 'warning'"
            :title="resultData.score >= resultData.totalScore * 0.6 ? '练习完成！' : '继续加油！'"
          >
            <template #sub-title>
              <div class="score-display">
                得分 <span class="score-num">{{ resultData.score }}</span> / {{ resultData.totalScore }} 分
                <el-tag :type="resultData.score >= resultData.totalScore * 0.6 ? 'success' : 'danger'" size="small" style="margin-left: 10px;">
                  正确 {{ resultData.correctCount }} / {{ resultData.totalCount }} 题
                </el-tag>
              </div>
            </template>
          </el-result>
        </div>
        <div class="result-questions" v-if="resultData.questionResults && resultData.questionResults.length > 0">
          <el-divider />
          <h4 class="result-section-title">答题详情</h4>
          <div v-for="(item, idx) in resultData.questionResults" :key="idx" class="result-q-item" :class="{ correct: item.isCorrect, wrong: !item.isCorrect }">
            <div class="result-q-header">
              <span class="result-q-index">第{{ n(idx) + 1 }}题</span>
              <el-tag :type="item.isCorrect ? 'success' : 'danger'" size="small">{{ item.isCorrect ? '✓ 正确' : '✗ 错误' }}</el-tag>
              <el-tag :type="getQuestionTypeTag(item.questionType)" size="small" style="margin-left: 8px;">{{ getQuestionTypeName(item.questionType) }}</el-tag>
            </div>
            <div class="result-q-content">{{ item.questionContent }}</div>
            <div class="result-q-answer">
              <div class="answer-row">
                <span class="answer-label">你的答案：</span>
                <span :class="['answer-value', item.isCorrect ? 'correct-text' : 'wrong-text']">{{ formatStudentAnswer(item.studentAnswer, item.questionType) || '未作答' }}</span>
              </div>
              <div class="answer-row" v-if="!item.isCorrect">
                <span class="answer-label">正确答案：</span>
                <span class="answer-value correct-text">{{ formatCorrectAnswer(item.correctAnswer, item.questionType) }}</span>
              </div>
              <div class="answer-row analysis" v-if="item.answerAnalysis">
                <span class="answer-label">答案解析：</span>
                <span class="analysis-text">{{ item.answerAnalysis }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="showResultDialog = false">关闭</el-button>
        <el-button type="primary" @click="retryPractice">重新练习</el-button>
        <el-button type="warning" @click="router.push('/wrong-questions')">查看错题本</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Notebook, QuestionFilled, Star, CircleCheck, CircleClose, Document, Loading, VideoPlay, Plus, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getLessonDetail, getCourseChapters } from '@/api/course'
import { updateVideoProgress, addNote, getPracticeQuestions, submitPracticeAnswers, addQuestionToLesson } from '@/api/study'
import { getQuestionList } from '@/api/exam'
import AIChat from '@/components/AIChat/index.vue'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const lessonId = Number(route.params.lessonId)

const isTeacher = computed(() => userStore.isTeacher || userStore.isAdmin)
const isStudent = computed(() => !isTeacher.value)

const dplayerRef = ref<HTMLElement>()
const lesson = ref<any>(null)
const chapters = ref<any[]>([])
const showNoteDialog = ref(false)
const showQuestionDialog = ref(false)
const noteForm = ref({ noteTitle: '', noteContent: '' })
const questionForm = ref({ title: '', content: '' })

// ==================== 30秒心跳定时器 ====================
let progressTimer: any = null
let lastReportTime = 0

const n = (val: string | number): number => typeof val === 'number' ? val : parseInt(val, 10)

const parseRes = (res: any): any => {
  if (!res) return null
  if (res.data && res.data.code === 200) return res.data.data !== null ? res.data.data : {}
  if (typeof res.code === 'number' && res.code === 200) return res.data !== null ? res.data : {}
  return null
}

const extractOptionKey = (opt: any, fallbackIdx: number): string => {
  if (!opt || typeof opt !== 'object') return String.fromCharCode(65 + fallbackIdx)
  const key = opt.key || opt.optionKey || opt.label || opt.optionLabel || opt.id || opt.optionId
  return key ? String(key) : String.fromCharCode(65 + fallbackIdx)
}

const getOptionDisplayText = (opt: any): string => {
  if (!opt) return ''
  if (typeof opt === 'string') return opt
  const text = opt.value || opt.optionContent || opt.content || opt.text || opt.label || opt.optionText || opt.title || opt.optionValue || opt.name || opt.description
  if (text) return String(text)
  try { return JSON.stringify(opt) } catch { return String(opt) }
}

const parseOptions = (options: any): any[] => {
  if (!options) return []
  if (Array.isArray(options)) return options
  if (typeof options === 'string') {
    try {
      const parsed = JSON.parse(options)
      return Array.isArray(parsed) ? parsed : []
    } catch { return [] }
  }
  return []
}

const getQuestionTypeName = (type: number): string => {
  const names: Record<number, string> = { 1: '单选题', 2: '多选题', 3: '判断题', 4: '填空题', 5: '简答题' }
  return names[type] || '未知题型'
}

const getQuestionTypeTag = (type: number): string => {
  const tags: Record<number, string> = { 1: 'primary', 2: 'success', 3: 'warning', 4: 'info', 5: '' }
  return tags[type] || ''
}

const formatStudentAnswer = (answer: string, type: number): string => {
  if (!answer) return ''
  if (type === 3) return answer.toUpperCase() === 'A' ? '正确 (A)' : answer.toUpperCase() === 'B' ? '错误 (B)' : answer
  if (type === 2) return answer.replace(/,/g, '')
  return answer
}

const formatCorrectAnswer = (answer: string, type: number): string => {
  if (!answer) return '暂无'
  if (type === 3) return answer.toUpperCase() === 'A' ? '正确 (A)' : answer.toUpperCase() === 'B' ? '错误 (B)' : answer
  if (type === 2) return answer.replace(/,/g, '')
  return answer
}

// ==================== 视频播放器初始化（修复30秒心跳） ====================
const initPlayer = (videoUrl: string) => {
  if (!dplayerRef.value) return

  const fullVideoUrl = `http://localhost:8080/api/upload/video/stream?path=${encodeURIComponent(videoUrl)}`

  dplayerRef.value.innerHTML = `
    <div style="color: #909399; text-align: center; padding-top: 150px;">
      <p style="margin-top: 10px;">正在加载视频...</p>
    </div>
  `
  const video = document.createElement('video')
  video.src = fullVideoUrl
  video.controls = true
  video.style.width = '100%'
  video.style.height = '100%'
  video.preload = 'metadata'

  let errorTimer: any = null

  video.addEventListener('loadedmetadata', () => {
    if (errorTimer) clearTimeout(errorTimer)
  })
  video.addEventListener('canplay', () => {
    if (errorTimer) clearTimeout(errorTimer)
  })
  video.addEventListener('error', (e) => {
    const videoEl = e.target as HTMLVideoElement
    const errorCode = videoEl.error?.code
    const errorMsg = videoEl.error?.message || '未知错误'
    console.error('[initPlayer] ❌ 视频错误:', errorCode, errorMsg)
    if (errorCode === 1) return
    errorTimer = setTimeout(() => {
      if (video.readyState < 1) {
        dplayerRef.value!.innerHTML = `
          <div style="color: #f56c6c; text-align: center; padding-top: 150px;">
            <p>视频加载失败 (${errorCode})</p>
            <p style="font-size: 12px; color: #909399;">${errorMsg}</p>
            <p style="font-size: 12px; color: #c0c4cc; margin-top: 8px;">${fullVideoUrl}</p>
            <a href="${fullVideoUrl}" target="_blank" style="color: #409EFF; display: inline-block; margin-top: 10px;">测试直接访问</a>
          </div>
        `
      }
    }, 1500)
  })

  dplayerRef.value.innerHTML = ''
  dplayerRef.value.appendChild(video)

  // ========== 30秒心跳：使用 setInterval 实现 ==========
  if (isStudent.value) {
    lastReportTime = 0

    progressTimer = setInterval(() => {
      if (!video.paused && !video.ended && video.currentTime > 0) {
        const current = Math.floor(video.currentTime)
        // 只有播放位置变化了才上报（避免暂停时重复上报）
        if (current !== lastReportTime) {
          lastReportTime = current
          reportProgress(video.currentTime, video.duration)
        }
      }
    }, 30000)

    // 视频结束时额外上报一次最终进度
    video.addEventListener('ended', () => {
      reportProgress(video.currentTime, video.duration)
    })
  }
}

// ==================== 上报学习进度（修复版） ====================
const reportProgress = async (currentTime: number, duration: number) => {
  if (!isStudent.value || !lesson.value?.courseId) return
  try {
    await updateVideoProgress({
      lessonId: lessonId,
      courseId: lesson.value.courseId,
      currentPosition: Math.floor(currentTime),
      totalDuration: Math.floor(duration),
      progressPercent: duration > 0 ? Math.floor((currentTime / duration) * 100) : 0,
      studyTime: 30 // 每次上报累计30秒
    })
  } catch {
    // 静默失败，不影响播放
  }
}

const fetchLessonDetail = async () => {
  try {
    const res = await getLessonDetail(lessonId) as any
    const responseData = res.data || res
    if (responseData.code === 200) {
      lesson.value = responseData.data

      // 兼容多种字段命名
      const videoUrl = lesson.value?.videoUrl || lesson.value?.video_url || lesson.value?.VideoUrl

      if (videoUrl) {
        nextTick(() => initPlayer(videoUrl))
      }
    }
  } catch {
    ElMessage.error('获取课时详情失败')
  }
}

const fetchChapters = async () => {
  if (!lesson.value?.courseId) return
  try {
    const res = await getCourseChapters(lesson.value.courseId) as any
    const responseData = res.data || res
    if (responseData.code === 200) chapters.value = responseData.data || []
  } catch { ElMessage.error('获取章节失败') }
}

const lessonQuestions = ref<any[]>([])
const questionLoading = ref(false)

const fetchLessonQuestions = async () => {
  questionLoading.value = true
  try {
    const res = await getPracticeQuestions(lessonId) as any
    const data = parseRes(res)
    if (Array.isArray(data) && data.length > 0) {
      lessonQuestions.value = data.map((q: any) => ({
        ...q,
        questionOptions: parseOptions(q.questionOptions)
      }))
    } else {
      lessonQuestions.value = []
    }
  } catch {
    ElMessage.error('获取课时题目失败')
  } finally {
    questionLoading.value = false
  }
}

const showPracticeDialog = ref(false)
const practiceLoading = ref(false)
const practiceQuestions = ref<any[]>([])
const currentQuestionIndex = ref(0)
const answers = ref<Record<number, string>>({})
const multiAnswers = ref<Record<number, string[]>>({})
const submitting = ref(false)
const showResultDialog = ref(false)
const resultData = ref<any>({ score: 0, totalScore: 0, correctCount: 0, totalCount: 0, questionResults: [] })

const currentQuestion = computed(() => practiceQuestions.value[currentQuestionIndex.value] || null)

const isAnswered = (idx: number): boolean => {
  const q = practiceQuestions.value[idx]
  if (!q) return false
  if (q.questionType === 2) return !!(multiAnswers.value[q.id] && multiAnswers.value[q.id].length > 0)
  return !!answers.value[q.id]
}

const openPracticeFrom = (index: number) => {
  if (lessonQuestions.value.length === 0) return
  practiceQuestions.value = [...lessonQuestions.value]
  currentQuestionIndex.value = index
  answers.value = {}
  multiAnswers.value = {}
  showResultDialog.value = false
  showPracticeDialog.value = true
}

const openPractice = async () => {
  showPracticeDialog.value = true
  practiceLoading.value = true
  practiceQuestions.value = []
  currentQuestionIndex.value = 0
  answers.value = {}
  multiAnswers.value = {}
  try {
    const res = await getPracticeQuestions(lessonId) as any
    const data = parseRes(res)
    if (Array.isArray(data) && data.length > 0) {
      practiceQuestions.value = data.map((q: any) => ({
        ...q,
        questionOptions: parseOptions(q.questionOptions)
      }))
    }
  } catch { ElMessage.error('获取练习题目失败') }
  finally { practiceLoading.value = false }
}

const submitPractice = async () => {
  submitting.value = true
  try {
    const finalAnswers: Record<number, string> = { ...answers.value }
    for (const [qid, vals] of Object.entries(multiAnswers.value)) {
      const numId = Number(qid)
      if (vals && vals.length > 0) finalAnswers[numId] = vals.sort().join(',')
    }

    try { await submitPracticeAnswers(lessonId, finalAnswers) } catch { }

    let totalScore = 0
    let obtainScore = 0
    let correctCount = 0
    const questionResults: any[] = []

    for (const q of practiceQuestions.value) {
      const studentAnswer = finalAnswers[q.id] || ''
      const correctAnswer = q.correctAnswer || ''
      const isCorrect = checkAnswerCorrect(q, studentAnswer)
      const qScore = q.score || 1
      totalScore += Number(qScore)
      if (isCorrect) { obtainScore += Number(qScore); correctCount++ }

      questionResults.push({
        questionId: q.id,
        questionContent: q.questionContent,
        questionType: q.questionType,
        studentAnswer: studentAnswer,
        correctAnswer: correctAnswer,
        isCorrect: isCorrect,
        answerAnalysis: q.answerAnalysis || '',
        score: qScore,
        obtainScore: isCorrect ? qScore : 0
      })
    }

    resultData.value = {
      score: obtainScore,
      totalScore: totalScore,
      correctCount: correctCount,
      totalCount: practiceQuestions.value.length,
      questionResults: questionResults
    }

    showPracticeDialog.value = false
    showResultDialog.value = true
    ElMessage.success(`练习完成！得分 ${obtainScore}/${totalScore}`)
  } catch (e: any) {
    ElMessage.error(e?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

const checkAnswerCorrect = (question: any, studentAnswer: string): boolean => {
  if (!studentAnswer || !studentAnswer.trim()) return false
  const correctAnswer = question.correctAnswer || ''
  if (!correctAnswer) return false
  const answer = studentAnswer.trim()
  if (question.questionType === 2) {
    let correctParts: string[]
    if (correctAnswer.includes(',')) {
      correctParts = correctAnswer.split(',').map((s: string) => s.trim()).filter(Boolean)
    } else {
      correctParts = correctAnswer.split('').map((s: string) => s.trim()).filter(Boolean)
    }
    const correctSet = new Set<string>(correctParts)
    const studentParts = answer.split(',').map((s: string) => s.trim()).filter(Boolean)
    const studentSet = new Set<string>(studentParts)
    if (correctSet.size !== studentSet.size) return false
    for (const c of correctSet) { if (!studentSet.has(c)) return false }
    return true
  }
  if (question.questionType === 1 || question.questionType === 3) {
    return answer.toUpperCase() === correctAnswer.toUpperCase()
  }
  return answer.toLowerCase() === correctAnswer.toLowerCase()
}

const retryPractice = () => {
  showResultDialog.value = false
  currentQuestionIndex.value = 0
  answers.value = {}
  multiAnswers.value = {}
  openPractice()
}

// ==================== 导入习题功能 ====================
const showImportDialog = ref(false)
const importKeyword = ref('')
const importQuestionType = ref<number | undefined>(undefined)
const importQuestions = ref<any[]>([])
const importSelectedIds = ref<number[]>([])
const importLoading = ref(false)
const importSubmitting = ref(false)

const openImportDialog = () => {
  showImportDialog.value = true
  importKeyword.value = ''
  importQuestionType.value = undefined
  importQuestions.value = []
  importSelectedIds.value = []
}

const searchImportQuestions = async () => {
  importLoading.value = true
  try {
    const res = await getQuestionList({
      keyword: importKeyword.value || undefined,
      questionType: importQuestionType.value,
      current: 1,
      size: 50
    }) as any
    const data = parseRes(res)
    if (data?.records && Array.isArray(data.records)) {
      importQuestions.value = data.records
    } else if (Array.isArray(data)) {
      importQuestions.value = data
    } else {
      importQuestions.value = []
    }
  } catch {
    ElMessage.error('搜索题目失败')
    importQuestions.value = []
  } finally {
    importLoading.value = false
  }
}

const handleImportSelectionChange = (selection: any[]) => {
  importSelectedIds.value = selection.map((item) => item.id)
}

const confirmImport = async () => {
  if (importSelectedIds.value.length === 0) {
    ElMessage.warning('请至少选择一道题目')
    return
  }
  importSubmitting.value = true
  let successCount = 0
  let failCount = 0
  for (const questionId of importSelectedIds.value) {
    try {
      await addQuestionToLesson(lessonId, questionId)
      successCount++
    } catch {
      failCount++
    }
  }
  importSubmitting.value = false
  if (successCount > 0) {
    ElMessage.success(`成功导入 ${successCount} 道题目${failCount > 0 ? `，${failCount} 道失败` : ''}`)
    showImportDialog.value = false
    await fetchLessonQuestions()
  } else {
    ElMessage.error('导入失败，请检查网络后重试')
  }
}

const switchLesson = (id: number) => { if (id !== lessonId) router.push(`/study/${id}`) }

const saveNote = async () => {
  if (!noteForm.value.noteContent.trim()) { ElMessage.warning('请输入笔记内容'); return }
  if (!lesson.value?.courseId) { ElMessage.error('课程信息无效'); return }
  try {
    await addNote({ courseId: lesson.value.courseId, lessonId: lessonId, noteTitle: noteForm.value.noteTitle, noteContent: noteForm.value.noteContent })
    ElMessage.success('笔记保存成功'); showNoteDialog.value = false; noteForm.value = { noteTitle: '', noteContent: '' }
  } catch { ElMessage.error('保存笔记失败') }
}

const submitQuestion = () => {
  if (!questionForm.value.title.trim()) { ElMessage.warning('请输入问题标题'); return }
  if (!questionForm.value.content.trim()) { ElMessage.warning('请输入问题内容'); return }
  ElMessage.success('问题提交成功'); showQuestionDialog.value = false; questionForm.value = { title: '', content: '' }
}

const toggleFavorite = () => { ElMessage.success('收藏功能开发中') }

onMounted(async () => {
  await fetchLessonDetail()
  await fetchChapters()
  await fetchLessonQuestions()
})

onUnmounted(() => {
  if (progressTimer) {
    clearInterval(progressTimer)
    progressTimer = null
  }
})
</script>

<style scoped>
.study-container { padding: 20px; }
.video-ai-section { margin-bottom: 20px; }
.video-section { background-color: #000; border-radius: 8px; overflow: hidden; }
.video-player { width: 100%; height: 500px; }
.lesson-info-section { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.lesson-info-section h2 { margin: 0; }
.lesson-actions { display: flex; gap: 10px; flex-wrap: wrap; }
.questions-section { background: #fff; border-radius: 8px; padding: 20px; margin-bottom: 20px; box-shadow: 0 2px 12px rgba(0,0,0,0.05); }
.questions-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.questions-header h3 { display: flex; align-items: center; gap: 8px; margin: 0; font-size: 16px; color: #303133; }
.header-actions { display: flex; gap: 8px; }
.question-preview-list { display: flex; flex-direction: column; gap: 12px; }
.question-preview-item { border: 1px solid #ebeef5; border-radius: 8px; padding: 14px 16px; cursor: pointer; transition: all 0.2s; }
.question-preview-item:hover { border-color: #409EFF; background: #ecf5ff; }
.preview-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.preview-index { width: 26px; height: 26px; display: flex; align-items: center; justify-content: center; background: #f0f2f5; border-radius: 50%; font-size: 13px; font-weight: 600; color: #606266; }
.preview-score { color: #f56c6c; font-size: 13px; }
.preview-content { font-size: 14px; color: #303133; line-height: 1.6; margin-bottom: 8px; }
.preview-options { display: flex; flex-direction: column; gap: 4px; margin-bottom: 10px; padding-left: 4px; }
.preview-option { display: flex; gap: 6px; font-size: 13px; color: #606266; }
.opt-key { color: #909399; min-width: 18px; }
.opt-text { color: #606266; }
.preview-footer { display: flex; justify-content: flex-end; }
.loading-hint, .empty-hint { display: flex; flex-direction: column; align-items: center; padding: 30px 0; gap: 8px; color: #909399; }
.empty-hint .hint-text { font-size: 12px; color: #c0c4cc; }
@keyframes rotating { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.is-loading { animation: rotating 2s linear infinite; }
.practice-body { padding: 10px 0; }
.practice-progress-bar { margin-bottom: 20px; }
.question-card { background: #f5f7fa; border-radius: 8px; padding: 20px; margin-bottom: 16px; }
.question-type-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.score-tag { color: #f56c6c; font-weight: 600; }
.question-text { font-size: 15px; line-height: 1.8; margin: 0 0 16px 0; }
.option-list { display: flex; flex-direction: column; gap: 8px; }
.option-row { padding: 10px 14px; border-radius: 6px; background: #fff; border: 1px solid #e4e7ed; width: 100%; margin: 0; }
.option-row:hover { border-color: #409EFF; }
.opt-label { font-weight: 600; color: #409EFF; margin-right: 4px; }
.opt-content { color: #303133; }
.practice-nav { display: flex; justify-content: center; gap: 16px; margin: 16px 0; }
.question-nav-dots { display: flex; justify-content: center; gap: 8px; flex-wrap: wrap; margin-top: 12px; padding-top: 12px; border-top: 1px solid #ebeef5; }
.nav-dot { width: 30px; height: 30px; display: flex; align-items: center; justify-content: center; border-radius: 50%; font-size: 12px; cursor: pointer; background: #f0f2f5; color: #909399; transition: all 0.2s; }
.nav-dot:hover { background: #d9ecff; }
.nav-dot.active { background: #409EFF; color: #fff; font-weight: 600; }
.nav-dot.answered { background: #e1f3d8; color: #67c23a; border: 1px solid #67c23a; }
.result-body { padding: 10px 0; }
.result-summary { text-align: center; }
.score-display { font-size: 15px; color: #606266; }
.score-num { font-size: 24px; font-weight: 700; color: #f56c6c; }
.result-section-title { margin: 0 0 16px 0; font-size: 16px; color: #303133; }
.result-q-item { border: 1px solid #ebeef5; border-radius: 8px; padding: 14px 16px; margin-bottom: 12px; }
.result-q-item.correct { border-left: 4px solid #67c23a; }
.result-q-item.wrong { border-left: 4px solid #f56c6c; }
.result-q-header { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.result-q-index { font-weight: 600; color: #303133; }
.result-q-content { font-size: 14px; color: #303133; line-height: 1.6; margin-bottom: 10px; }
.result-q-answer { background: #f5f7fa; border-radius: 6px; padding: 10px 12px; }
.answer-row { display: flex; gap: 6px; margin-bottom: 6px; font-size: 13px; }
.answer-row:last-child { margin-bottom: 0; }
.answer-label { color: #909399; white-space: nowrap; }
.answer-value { font-weight: 500; }
.correct-text { color: #67c23a; }
.wrong-text { color: #f56c6c; }
.answer-row.analysis { margin-top: 8px; padding-top: 8px; border-top: 1px dashed #dcdfe6; }
.analysis-text { color: #409EFF; line-height: 1.6; }
.chapters-card { margin-top: 20px; }
.chapter-item { margin-bottom: 15px; }
.chapter-header { display: flex; align-items: center; gap: 10px; padding: 8px; background-color: #f5f7fa; border-radius: 4px; margin-bottom: 8px; }
.chapter-number { font-weight: bold; color: #409EFF; }
.lessons-list { padding-left: 15px; }
.lesson-item { display: flex; align-items: center; gap: 10px; padding: 8px; border-bottom: 1px solid #ebeef5; cursor: pointer; }
.lesson-item:hover, .lesson-item.active { background-color: #ecf5ff; color: #409EFF; }
.lesson-item.completed { color: #67c23a; }
.completed-icon { color: #67c23a; }
.lesson-number { color: #909399; min-width: 40px; }
.lesson-name { flex: 1; }
.import-search-section { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
.import-question-list { min-height: 200px; }
.import-q-content { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 350px; }
</style>