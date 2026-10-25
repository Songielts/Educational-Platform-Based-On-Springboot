<template>
  <div class="course-learn-container">
    <el-row :gutter="20">
      <!-- 左侧：视频和练习区域 -->
      <el-col :span="18">
        <!-- 视频播放区域 -->
        <div class="video-section">
          <div class="video-player" v-if="lesson.videoUrl">
            <video
              ref="videoRef"
              :src="lesson.videoUrl"
              controls
              preload="metadata"
              class="main-video"
              @timeupdate="handleTimeUpdate"
              @ended="handleVideoEnded"
              @play="handleVideoPlay"
              @pause="handleVideoPause"
              @error="handleVideoError"
              @stalled="handleVideoStalled"
              @loadedmetadata="handleVideoLoadedMetadata"
            />
          </div>
          <el-empty v-else description="暂无视频" :image-size="120" />
        </div>

        <!-- 课时信息和操作 -->
        <div class="lesson-info-bar">
          <div class="lesson-title-section">
            <h2>{{ lesson.lessonName || lesson.name || '未命名课时' }}</h2>
            <p class="lesson-desc" v-if="lesson.content">{{ lesson.content }}</p>
          </div>
          <div class="lesson-actions">
            <el-button
              type="primary"
              :icon="EditPen"
              @click="showNoteDialog = true"
            >写笔记</el-button>
            <el-button
              type="success"
              :icon="Document"
              @click="openPractice"
              :loading="practiceLoading"
            >课时练习</el-button>
            <el-button
              type="warning"
              :icon="CircleCheck"
              @click="markAsCompleted"
              v-if="!isCompleted"
            >标记完成</el-button>
            <el-tag v-else type="success">已完成</el-tag>
          </div>
        </div>

        <!-- 笔记列表 -->
        <div class="notes-section" v-if="noteList.length > 0">
          <h3>
            <el-icon><Notebook /></el-icon>
            我的笔记
          </h3>
          <div class="note-list">
            <div v-for="note in noteList" :key="note.id" class="note-item">
              <p class="note-content">{{ note.content }}</p>
              <div class="note-meta">
                <span v-if="note.timestamp" class="note-timestamp" @click="seekVideo(note.timestamp)">
                  <el-icon><VideoPlay /></el-icon>
                  {{ formatTime(note.timestamp) }}
                </span>
                <span class="note-time">{{ note.createTime }}</span>
                <el-button type="danger" link size="small" @click="handleDeleteNote(note.id)">删除</el-button>
              </div>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 右侧：课程目录 -->
      <el-col :span="6">
        <div class="catalog-sidebar">
          <h3>课程目录</h3>
          <div class="chapter-list">
            <div
              v-for="chapter in chapterList"
              :key="chapter.id"
              class="chapter-item"
            >
              <div class="chapter-title">{{ chapter.chapterName || chapter.name }}</div>
              <div class="lesson-sublist">
                <div
                  v-for="item in (chapter.lessons || [])"
                  :key="item.id"
                  class="lesson-subitem"
                  :class="{ active: item.id === lessonId, completed: item.isCompleted }"
                  @click="switchLesson(item.id)"
                >
                  <el-icon v-if="item.isCompleted"><CircleCheck /></el-icon>
                  <el-icon v-else><VideoPlay /></el-icon>
                  <span class="subitem-name">{{ item.lessonName || item.name }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 练习弹窗 -->
    <el-dialog
      v-model="showPracticeDialog"
      title="课时练习"
      width="700px"
      :close-on-click-modal="false"
    >
      <div v-loading="practiceLoading">
        <el-empty v-if="!practiceQuestions.length" description="暂无练习题目" />
        <div v-else class="practice-content">
          <div class="practice-progress">
            题目 {{ currentQuestionIndex + 1 }} / {{ practiceQuestions.length }}
          </div>

          <div class="question-card" v-if="currentQuestion">
            <div class="question-header">
              <el-tag :type="getQuestionTypeTag(currentQuestion.questionType)">
                {{ getQuestionTypeName(currentQuestion.questionType) }}
              </el-tag>
              <span class="question-score">{{ currentQuestion.score || 1 }}分</span>
            </div>
            <p class="question-content">{{ currentQuestion.questionContent }}</p>

            <!-- 单选题 -->
            <template v-if="currentQuestion.questionType === 1">
              <el-radio-group v-model="answers[currentQuestion.id]" class="option-group">
                <el-radio
                  v-for="(opt, optIdx) in (currentQuestion.questionOptions || [])"
                  :key="optIdx"
                  :label="optLabel(optIdx)"
                  class="option-item"
                >
                  {{ optLabel(optIdx) }}. {{ opt.optionContent || opt }}
                </el-radio>
              </el-radio-group>
            </template>

            <!-- 多选题 -->
            <template v-else-if="currentQuestion.questionType === 2">
              <el-checkbox-group v-model="multiAnswers[currentQuestion.id]" class="option-group">
                <el-checkbox
                  v-for="(opt, optIdx) in (currentQuestion.questionOptions || [])"
                  :key="optIdx"
                  :label="optLabel(optIdx)"
                  class="option-item"
                >
                  {{ optLabel(optIdx) }}. {{ opt.optionContent || opt }}
                </el-checkbox>
              </el-checkbox-group>
            </template>

            <!-- 判断题 -->
            <template v-else-if="currentQuestion.questionType === 3">
              <el-radio-group v-model="answers[currentQuestion.id]" class="option-group">
                <el-radio label="A" class="option-item">正确</el-radio>
                <el-radio label="B" class="option-item">错误</el-radio>
              </el-radio-group>
            </template>

            <!-- 填空题 -->
            <template v-else-if="currentQuestion.questionType === 4">
              <el-input
                v-model="answers[currentQuestion.id]"
                type="textarea"
                :rows="3"
                placeholder="请输入答案"
              />
            </template>

            <!-- 简答题 -->
            <template v-else>
              <el-input
                v-model="answers[currentQuestion.id]"
                type="textarea"
                :rows="5"
                placeholder="请输入答案"
              />
            </template>
          </div>

          <div class="practice-actions">
            <el-button
              @click="currentQuestionIndex--"
              :disabled="currentQuestionIndex === 0"
            >上一题</el-button>
            <el-button
              type="primary"
              @click="currentQuestionIndex++"
              v-if="currentQuestionIndex < practiceQuestions.length - 1"
            >下一题</el-button>
            <el-button
              type="success"
              @click="submitPractice"
              v-if="currentQuestionIndex === practiceQuestions.length - 1"
              :loading="submitting"
            >提交答案</el-button>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 练习结果弹窗 -->
    <el-dialog v-model="showResultDialog" title="练习结果" width="500px" :close-on-click-modal="false">
      <div class="practice-result">
        <el-result
          :icon="resultData.score >= resultData.totalScore * 0.6 ? 'success' : 'warning'"
          :title="resultData.score >= resultData.totalScore * 0.6 ? '练习完成' : '继续加油'"
          :sub-title="`得分 ${resultData.score} / ${resultData.totalScore}`"
        />
        <div class="result-detail" v-if="resultData.details && resultData.details.length > 0">
          <h4>答题详情</h4>
          <div
            v-for="(detailItem, detailIdx) in resultData.details"
            :key="detailIdx"
            class="result-item"
          >
            <span>第{{ n(detailIdx) + 1 }}题</span>
            <el-tag :type="detailItem.correct ? 'success' : 'danger'" size="small">
              {{ detailItem.correct ? '正确' : '错误' }}
            </el-tag>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="showResultDialog = false">关闭</el-button>
        <el-button type="primary" @click="retryPractice">重新练习</el-button>
      </template>
    </el-dialog>

    <!-- 写笔记弹窗 -->
    <el-dialog v-model="showNoteDialog" title="写笔记" width="500px">
      <el-form :model="noteForm" ref="noteFormRef" :rules="noteRules">
        <el-form-item prop="content">
          <el-input
            v-model="noteForm.content"
            type="textarea"
            :rows="5"
            placeholder="记录你的学习心得..."
          />
        </el-form-item>
        <el-form-item label="关联时间点" v-if="videoRef">
          <el-input-number v-model="noteForm.timestamp" :min="0" :precision="0" placeholder="秒" />
          <span class="time-hint">视频当前位置 {{ formatTime(currentVideoTime) }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showNoteDialog = false">取消</el-button>
        <el-button type="primary" @click="submitNote">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  EditPen,
  Document,
  CircleCheck,
  Notebook,
  VideoPlay
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getLessonDetail, getCourseChapters } from '@/api/course'
import {
  getLessonProgress,
  updateLessonProgress,
  getLessonNotes,
  addNote,
  deleteNote,
  getPracticeQuestions,
  submitPracticeAnswers
} from '@/api/study'

const route = useRoute()
const router = useRouter()
const lessonId = Number(route.params.lessonId)
const courseId = ref<number>(0)

// 将 v-for 的 string|number 索引安全转为 number
const n = (val: string | number): number => typeof val === 'number' ? val : parseInt(val, 10)
// 选项标签 A/B/C/D
const optLabel = (val: string | number): string => {
  const labels = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H']
  return labels[n(val)] || String(val)
}

// 视频相关
const videoRef = ref<HTMLVideoElement | null>(null)
const currentVideoTime = ref(0)
const isCompleted = ref(false)

// 30秒进度上报定时器
const reportTimer = ref<ReturnType<typeof setInterval> | null>(null)
const lastReportedPosition = ref(0)
const isVideoReady = ref(false)        // <-- 视频元数据是否加载完成
const videoDuration = ref(0)           // <-- 缓存的视频总时长（避免每次读取video.duration）
const lastReportTimestamp = ref(0)     // <-- 新增：上次上报时间戳（毫秒），防止1秒内多次上报
const progressDebounceTimer = ref<ReturnType<typeof setTimeout> | null>(null)  // <-- 防抖定时器

// 课时数据
const lesson = ref<any>({})
const chapterList = ref<any[]>([])
const loading = ref(false)

// 练习相关
const showPracticeDialog = ref(false)
const practiceLoading = ref(false)
const practiceQuestions = ref<any[]>([])
const currentQuestionIndex = ref(0)
const answers = ref<Record<number, string>>({})
const multiAnswers = ref<Record<number, string[]>>({})
const submitting = ref(false)
const showResultDialog = ref(false)
const resultData = ref<any>({ score: 0, totalScore: 0, details: [] })

// 笔记相关
const showNoteDialog = ref(false)
const noteFormRef = ref<FormInstance>()
const noteForm = reactive({ content: '', timestamp: 0 })
const noteRules: FormRules = {
  content: [{ required: true, message: '请输入笔记内容', trigger: 'blur' }]
}
const noteList = ref<any[]>([])

// 计算属性
const currentQuestion = computed(() => {
  return practiceQuestions.value[currentQuestionIndex.value] || null
})

// ==================== 统一解析 ====================
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

// ==================== 课时加载 ====================
const fetchLesson = async () => {
  try {
    const res = await getLessonDetail(lessonId) as any
    const data = parseRes(res)
    if (data) {
      lesson.value = data
      if (data.courseId) courseId.value = data.courseId
    }
  } catch (e: any) {
    ElMessage.error('获取课时信息失败')
  }
}

const fetchChapters = async () => {
  if (!courseId.value) return
  try {
    const res = await getCourseChapters(courseId.value) as any
    const data = parseRes(res)
    if (Array.isArray(data)) {
      chapterList.value = data
    }
  } catch (e: any) {
    console.error('获取章节失败:', e)
  }
}

const fetchProgress = async () => {
  try {
    const res = await getLessonProgress(lessonId) as any
    const data = parseRes(res)
    if (data) {
      isCompleted.value = data.completed || false
    }
  } catch (e) {
    // 进度不存在是正常的
  }
}

const fetchNotes = async () => {
  try {
    const res = await getLessonNotes(lessonId) as any
    const data = parseRes(res)
    if (Array.isArray(data)) {
      noteList.value = data
    }
  } catch (e) {
    console.error('获取笔记失败:', e)
  }
}

// ==================== 视频操作 ====================
const handleTimeUpdate = (e: Event) => {
  const video = e.target as HTMLVideoElement
  currentVideoTime.value = Math.floor(video.currentTime)
}

const handleVideoEnded = () => {
  isCompleted.value = true
  stopProgressReport()
  reportProgress(true)
}

const handleVideoPlay = () => {
  console.log('[视频播放] play事件触发, isVideoReady=' + isVideoReady.value + ', timer=' + !!reportTimer.value)
  if (reportTimer.value) return
  startProgressReport()
}

const handleVideoPause = () => {
  console.log('[视频暂停] pause事件触发')
  stopProgressReport()
}

const handleVideoLoadedMetadata = (e: Event) => {
  const video = e.target as HTMLVideoElement
  const duration = Math.floor(video.duration || 0)
  console.log(`[视频就绪] loadedmetadata触发, duration=${duration}s`)
  if (duration > 0) {
    videoDuration.value = duration
    isVideoReady.value = true
  }
}

const startProgressReport = () => {
  if (reportTimer.value) return

  // 获取视频时长：浏览器原生读取 > 课时数据(videoDuration/video_duration) > 0
  const browserDuration = Math.floor(videoRef.value?.duration || 0)
  const lessonDurationRaw = lesson.value?.videoDuration || lesson.value?.video_duration || 0
  const lessonDuration = Math.floor(Number(lessonDurationRaw) || 0)
  const finalDuration = browserDuration || lessonDuration

  videoDuration.value = finalDuration
  isVideoReady.value = finalDuration > 0

  console.log(`[进度上报] 启动定时器, 视频时长=${finalDuration}s (浏览器:${browserDuration}, 课时:${lessonDuration})`)

  // 立即上报一次（视频开始播放时）
  reportProgress(false)

  // 每30秒上报一次
  reportTimer.value = setInterval(() => {
    reportProgress(false)
  }, 30000)
}

const stopProgressReport = () => {
  if (reportTimer.value) {
    clearInterval(reportTimer.value)
    reportTimer.value = null
    console.log('[进度上报] 定时器已停止')
  }
}

const reportProgress = async (isCompletedFlag: boolean = false) => {
  if (!videoRef.value || !lessonId) return

  // 强制间隔限制：非完成状态下，距离上次上报必须 ≥ 25秒
  const now = Date.now()
  if (!isCompletedFlag && now - lastReportTimestamp.value < 25000) {
    console.log(`[进度上报] 跳过：距上次上报仅 ${now - lastReportTimestamp.value}ms，不足25秒`)
    return
  }

  // 实时读取当前播放位置
  const currentTime = Math.floor(videoRef.value.currentTime || 0)

  // 获取视频时长：实时浏览器读取 > 缓存 > 课时数据
  const browserDuration = Math.floor(videoRef.value.duration || 0)
  const cachedDuration = videoDuration.value || 0
  const lessonDurationRaw = lesson.value?.videoDuration || lesson.value?.video_duration || 0
  const lessonDuration = Math.floor(Number(lessonDurationRaw) || 0)
  const duration = browserDuration || cachedDuration || lessonDuration

  // 如果进度没有变化且不是完成状态，跳过本次上报
  if (currentTime === lastReportedPosition.value && !isCompletedFlag) {
    return
  }

  const progress = duration > 0 ? Math.min(Math.floor((currentTime / duration) * 100), 100) : 0
  const completed = isCompletedFlag || progress >= 95

  console.log(`[进度上报] 发送请求: lessonId=${lessonId}, currentTime=${currentTime}, duration=${duration}, progress=${progress}%`)

  try {
    await updateLessonProgress(lessonId, {
      courseId: courseId.value || lesson.value.courseId,
      lastPosition: currentTime,
      duration: duration,
      progress: progress,
      completed: completed
    })
    lastReportedPosition.value = currentTime
    lastReportTimestamp.value = now  // <-- 记录本次上报时间
    console.log(`[进度上报] 成功: 位置=${currentTime}s / ${duration}s, 进度=${progress}%`)
  } catch (e: any) {
    console.error('[进度上报失败]', e?.message || e)
  }
}

const handleVideoError = (e: Event) => {
  const video = e.target as HTMLVideoElement
  console.error('[视频错误]', {
    error: video.error,
    code: video.error?.code,
    message: video.error?.message,
    src: video.src
  })
  ElMessage.error('视频播放出错，请检查视频源或网络连接')
  stopProgressReport()
}

const handleVideoStalled = (e: Event) => {
  const video = e.target as HTMLVideoElement
  console.warn('[视频缓冲失败]', video.src)
  // 不立即停止上报，给视频一点缓冲时间
}

const seekVideo = (seconds: number) => {
  if (videoRef.value) {
    videoRef.value.currentTime = seconds
    videoRef.value.play()
  }
}

const markAsCompleted = async () => {
  try {
    const res = await updateLessonProgress(lessonId, { completed: true, progress: 100 })
    if (isSuccess(res)) {
      isCompleted.value = true
      ElMessage.success('已标记为完成')
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  }
}

// ==================== 练习功能 ====================
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
  } catch (e: any) {
    ElMessage.error('获取练习题目失败')
  } finally {
    practiceLoading.value = false
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

const submitPractice = async () => {
  submitting.value = true
  try {
    const finalAnswers: Record<number, string> = { ...answers.value }
    for (const [qid, vals] of Object.entries(multiAnswers.value)) {
      const numId = Number(qid)
      if (vals && vals.length > 0) {
        finalAnswers[numId] = vals.sort().join(',')
      }
    }

    const res = await submitPracticeAnswers(lessonId, finalAnswers) as any
    const data = parseRes(res)
    if (data) {
      resultData.value = {
        score: data.score || 0,
        totalScore: data.totalScore || practiceQuestions.value.reduce((s, q) => s + (q.score || 1), 0),
        details: data.details || []
      }
      showPracticeDialog.value = false
      showResultDialog.value = true
      ElMessage.success('练习提交成功')
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

const retryPractice = () => {
  showResultDialog.value = false
  currentQuestionIndex.value = 0
  answers.value = {}
  multiAnswers.value = {}
  openPractice()
}

// ==================== 笔记功能 ====================
const submitNote = async () => {
  const valid = await noteFormRef.value?.validate().catch(() => false)
  if (!valid) return

  try {
    const res = await addNote({
      lessonId,
      courseId: courseId.value,
      content: noteForm.content,
      timestamp: noteForm.timestamp || undefined
    })
    if (isSuccess(res)) {
      ElMessage.success('笔记保存成功')
      showNoteDialog.value = false
      noteForm.content = ''
      noteForm.timestamp = 0
      fetchNotes()
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  }
}

const handleDeleteNote = async (noteId: number) => {
  try {
    const res = await deleteNote(noteId)
    if (isSuccess(res)) {
      ElMessage.success('删除成功')
      fetchNotes()
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '删除失败')
  }
}

// ==================== 工具函数 ====================
const formatTime = (seconds: number): string => {
  if (!seconds && seconds !== 0) return ''
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

const switchLesson = (id: number) => {
  if (id === lessonId) return
  router.push(`/study/${id}`)
}

// ==================== 生命周期 ====================
onMounted(async () => {
  if (!lessonId || isNaN(lessonId)) {
    ElMessage.error('课时ID无效')
    return
  }
  await fetchLesson()
  await fetchChapters()
  await fetchProgress()
  await fetchNotes()
})

onBeforeUnmount(() => {
  stopProgressReport()
})
</script>

<style scoped>
.course-learn-container {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.video-section {
  background: #000;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 20px;
}

.main-video {
  width: 100%;
  height: 500px;
  display: block;
}

.lesson-info-bar {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.lesson-title-section h2 {
  margin: 0 0 10px 0;
  font-size: 20px;
}

.lesson-desc {
  color: #606266;
  font-size: 14px;
  margin: 0 0 16px 0;
}

.lesson-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.notes-section {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.notes-section h3 {
  margin: 0 0 16px 0;
  font-size: 16px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.note-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.note-item {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
}

.note-content {
  margin: 0 0 8px 0;
  font-size: 14px;
  line-height: 1.6;
}

.note-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #909399;
}

.note-timestamp {
  color: #409EFF;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
}

.note-timestamp:hover {
  text-decoration: underline;
}

.catalog-sidebar {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  max-height: 80vh;
  overflow-y: auto;
}

.catalog-sidebar h3 {
  margin: 0 0 12px 0;
  font-size: 16px;
}

.chapter-item {
  margin-bottom: 8px;
}

.chapter-title {
  font-weight: 600;
  font-size: 14px;
  padding: 8px 0;
  color: #303133;
}

.lesson-sublist {
  padding-left: 12px;
}

.lesson-subitem {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  color: #606266;
  transition: all 0.2s;
}

.lesson-subitem:hover {
  background: #f5f7fa;
  color: #409EFF;
}

.lesson-subitem.active {
  background: #ecf5ff;
  color: #409EFF;
  font-weight: 500;
}

.lesson-subitem.completed {
  color: #67c23a;
}

.subitem-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 练习弹窗样式 */
.practice-content {
  padding: 10px 0;
}

.practice-progress {
  text-align: center;
  margin-bottom: 16px;
  color: #606266;
  font-size: 14px;
}

.question-card {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.question-score {
  color: #f56c6c;
  font-weight: 600;
}

.question-content {
  font-size: 15px;
  line-height: 1.8;
  margin: 0 0 16px 0;
}

.option-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.option-item {
  padding: 8px 12px;
  border-radius: 4px;
  background: #fff;
}

.practice-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 20px;
}

/* 结果弹窗 */
.practice-result {
  padding: 10px 0;
}

.result-detail h4 {
  margin: 0 0 12px 0;
}

.result-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #ebeef5;
}

.time-hint {
  margin-left: 10px;
  color: #909399;
  font-size: 13px;
}
</style>