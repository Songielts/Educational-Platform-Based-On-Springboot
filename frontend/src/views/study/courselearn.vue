<template>
  <div class="course-learn">
    <el-container>
      <!-- 左侧章节列表 -->
      <el-aside width="300px" class="chapter-sidebar">
        <div class="sidebar-header">
          <h3>{{ courseInfo.courseName }}</h3>
          <el-progress 
            :percentage="courseProgress" 
            :format="progressFormat"
            :status="courseProgress === 100 ? 'success' : ''"
          />
        </div>
        
        <el-collapse v-model="activeChapters">
          <el-collapse-item
            v-for="chapter in chapterList"
            :key="chapter.id"
            :title="chapter.chapterName"
            :name="chapter.id"
          >
            <div class="video-list">
              <div
                v-for="video in chapter.videos"
                :key="video.id"
                class="video-item"
                :class="{ 
                  active: currentVideo?.id === video.id,
                  completed: video.isCompleted 
                }"
                @click="selectVideo(chapter, video)"
              >
                <el-icon v-if="video.isCompleted" class="status-icon completed">
                  <CircleCheck />
                </el-icon>
                <el-icon v-else-if="video.currentTime > 0" class="status-icon in-progress">
                  <VideoPlay />
                </el-icon>
                <el-icon v-else class="status-icon">
                  <VideoPlay />
                </el-icon>
                <span class="video-name">{{ video.videoName }}</span>
                <span v-if="video.videoDuration" class="duration">
                  {{ formatDuration(video.videoDuration) }}
                </span>
              </div>
            </div>
          </el-collapse-item>
        </el-collapse>
      </el-aside>
      
      <!-- 右侧视频播放区 -->
      <el-main class="video-main">
        <div v-if="currentVideo" class="video-container">
          <h2 class="video-title">{{ currentVideo.videoName }}</h2>
          <VideoPlayer
            :video-id="currentVideo.id"
            :course-id="courseId"
            :video-url="currentVideo.videoUrl"
          />
          
          <div class="video-info">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="所属章节">
                {{ currentChapter?.chapterName }}
              </el-descriptions-item>
              <el-descriptions-item label="视频时长">
                {{ formatDuration(currentVideo.videoDuration) }}
              </el-descriptions-item>
            </el-descriptions>
            
            <!-- 题目练习按钮 -->
            <div class="practice-action">
              <el-button type="primary" @click="openPractice" :loading="practiceLoading">
                <el-icon><EditPen /></el-icon> 题目练习
              </el-button>
              <el-tag v-if="practiceCount > 0" type="success">{{ practiceCount }}道题</el-tag>
              <el-tag v-else type="info">暂无练习</el-tag>
            </div>
          </div>
        </div>
        
        <el-empty v-else description="请选择视频开始学习" />
      </el-main>
    </el-container>
    
    <!-- 题目练习弹窗 -->
    <el-dialog v-model="practiceVisible" title="课时练习" width="800px" :close-on-click-modal="false">
      <div v-loading="practiceLoading" class="practice-container">
        <el-empty v-if="practiceQuestions.length === 0" description="暂无练习题目" />
        
        <div v-else>
          <div v-for="(q, index) in practiceQuestions" :key="q.id" class="practice-question">
            <div class="question-title">
              第{{ index + 1 }}题 
              <el-tag size="small">{{ q.questionTypeName }}</el-tag>
              <span class="question-score">({{ q.score }}分)</span>
            </div>
            <div class="question-content">{{ q.questionContent }}</div>
            
            <!-- 单选题 -->
            <el-radio-group 
              v-if="q.questionType === 1" 
              v-model="practiceAnswers[q.id]"
              class="options-group"
            >
              <el-radio v-for="opt in q.questionOptions" :key="opt.key" :value="opt.key">
                {{ opt.key }}. {{ opt.value }}
              </el-radio>
            </el-radio-group>
            
            <!-- 多选题 -->
            <el-checkbox-group 
              v-else-if="q.questionType === 2" 
              v-model="practiceAnswersMulti[q.id]"
              class="options-group"
            >
              <el-checkbox v-for="opt in q.questionOptions" :key="opt.key" :value="opt.key">
                {{ opt.key }}. {{ opt.value }}
              </el-checkbox>
            </el-checkbox-group>
            
            <!-- 判断题 -->
            <el-radio-group 
              v-else-if="q.questionType === 3" 
              v-model="practiceAnswers[q.id]"
              class="options-group"
            >
              <el-radio value="A">正确</el-radio>
              <el-radio value="B">错误</el-radio>
            </el-radio-group>
            
            <!-- 答题结果展示 -->
            <div v-if="q.isCorrect !== undefined" class="answer-result" :class="q.isCorrect === 1 ? 'correct' : 'wrong'">
              <p><strong>你的答案：</strong>{{ q.studentAnswer || '未作答' }}</p>
              <p><strong>正确答案：</strong>{{ q.correctAnswer }}</p>
              <p v-if="q.answerAnalysis"><strong>解析：</strong>{{ q.answerAnalysis }}</p>
            </div>
          </div>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="practiceVisible = false">关闭</el-button>
        <el-button type="primary" @click="submitPractice" :loading="submittingPractice" v-if="hasUnanswered">
          提交答案
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { CircleCheck, VideoPlay, EditPen } from '@element-plus/icons-vue'
import VideoPlayer from '@/components/VideoPlayer/index.vue'
import { getVideoList } from '@/api/video'
import { getCourseProgress } from '@/api/video'
import { getCourseDetail } from '@/api/course'
import { getPracticeQuestions, submitPracticeAnswers } from '@/api/study'
import { ElMessage } from 'element-plus'

const route = useRoute()
const courseId = Number(route.params.courseId)

const courseInfo = ref<any>({})
const chapterList = ref<any[]>([])
const activeChapters = ref<number[]>([])
const currentVideo = ref<any>(null)
const currentChapter = ref<any>(null)
const courseProgress = ref(0)

// 题目练习相关
const practiceVisible = ref(false)
const practiceLoading = ref(false)
const submittingPractice = ref(false)
const practiceQuestions = ref<any[]>([])
const practiceAnswers = reactive<Record<number, string>>({})
const practiceAnswersMulti = reactive<Record<number, string[]>>({})
const practiceCount = ref(0)

const hasUnanswered = computed(() => {
  return practiceQuestions.value.some(q => q.isCorrect === undefined)
})

onMounted(async () => {
  await loadCourseInfo()
  await loadChapters()
  await loadCourseProgress()
})

const loadCourseInfo = async () => {
  const res = await getCourseDetail(courseId)
  courseInfo.value = res.data
}

const loadChapters = async () => {
  const res = await getVideoList(0)
  chapterList.value = res.data || []
  if (chapterList.value.length > 0) {
    activeChapters.value = [chapterList.value[0].id]
  }
}

const loadCourseProgress = async () => {
  const res = await getCourseProgress(courseId)
  if (res.data) {
    courseProgress.value = res.data.progress || 0
  }
}

const selectVideo = (chapter: any, video: any) => {
  currentChapter.value = chapter
  currentVideo.value = video
}

const progressFormat = (percentage: number) => {
  return percentage === 100 ? '已完成' : `${percentage}%`
}

const formatDuration = (seconds: number) => {
  if (!seconds) return '00:00'
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

const parseRes = (res: any): any => {
  if (!res) return null
  if (res.data && typeof res.data.code === 'number' && res.data.code === 200) return res.data.data
  if (typeof res.code === 'number' && res.code === 200) return res.data
  return null
}

const openPractice = async () => {
  if (!currentVideo.value) {
    ElMessage.warning('请先选择视频')
    return
  }
  practiceVisible.value = true
  await loadPracticeQuestions()
}

const loadPracticeQuestions = async () => {
  practiceLoading.value = true
  try {
    const res = await getPracticeQuestions(currentVideo.value.id)
    const data = parseRes(res)
    console.log('[courselearn] 练习题目:', data)
    if (data && Array.isArray(data)) {
      practiceQuestions.value = data
      practiceCount.value = data.length
      practiceQuestions.value.forEach(q => {
        if (q.questionType === 2) {
          practiceAnswersMulti[q.id] = []
        }
        if (q.studentAnswer) {
          practiceAnswers[q.id] = q.studentAnswer
        }
      })
    } else {
      practiceQuestions.value = []
      practiceCount.value = 0
    }
  } catch (error) {
    console.error('加载练习题目失败:', error)
    ElMessage.error('加载练习题目失败')
  } finally {
    practiceLoading.value = false
  }
}

const submitPractice = async () => {
  const answers: Record<number, string> = {}
  practiceQuestions.value.forEach(q => {
    if (q.questionType === 2) {
      answers[q.id] = (practiceAnswersMulti[q.id] || []).sort().join(',')
    } else {
      answers[q.id] = practiceAnswers[q.id] || ''
    }
  })
  
  submittingPractice.value = true
  try {
    await submitPracticeAnswers(currentVideo.value.id, answers)
    ElMessage.success('提交成功')
    await loadPracticeQuestions()
  } catch (error) {
    ElMessage.error('提交失败')
  } finally {
    submittingPractice.value = false
  }
}
</script>

<style scoped>
.course-learn {
  height: 100vh;
}

.chapter-sidebar {
  background: #f5f7fa;
  border-right: 1px solid #e4e7ed;
  overflow-y: auto;
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid #e4e7ed;
}

.sidebar-header h3 {
  margin: 0 0 15px 0;
  font-size: 16px;
}

.video-list {
  padding: 10px;
}

.video-item {
  display: flex;
  align-items: center;
  padding: 10px;
  margin-bottom: 5px;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.3s;
}

.video-item:hover {
  background: #e6f7ff;
}

.video-item.active {
  background: #1890ff;
  color: #fff;
}

.video-item.completed {
  color: #52c41a;
}

.status-icon {
  margin-right: 8px;
  font-size: 16px;
}

.status-icon.completed {
  color: #52c41a;
}

.status-icon.in-progress {
  color: #faad14;
}

.video-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.duration {
  font-size: 12px;
  color: #999;
}

.video-main {
  padding: 20px;
  background: #fff;
}

.video-title {
  margin-bottom: 20px;
  font-size: 20px;
}

.video-info {
  margin-top: 20px;
}

.practice-action {
  margin-top: 15px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.practice-container {
  max-height: 600px;
  overflow-y: auto;
}

.practice-question {
  margin-bottom: 20px;
  padding: 15px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}

.question-title {
  font-weight: 600;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.question-score {
  color: #f56c6c;
}

.question-content {
  margin-bottom: 15px;
  line-height: 1.6;
}

.options-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.answer-result {
  margin-top: 10px;
  padding: 10px;
  border-radius: 4px;
}

.answer-result.correct {
  background-color: #f0f9ff;
  color: #409eff;
}

.answer-result.wrong {
  background-color: #fef0f0;
  color: #f56c6c;
}
</style>