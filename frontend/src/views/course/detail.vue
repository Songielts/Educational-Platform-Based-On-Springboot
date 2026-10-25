<template>
  <div class="course-detail-container">
    <!-- 课程封面和基本信息 -->
    <div class="course-header">
      <div class="course-cover">
        <img
          v-if="coverImageSrc"
          :src="coverImageSrc"
          alt="课程封面"
          @error="handleImageError"
        />
        <div v-else class="cover-placeholder">
          <el-icon :size="48"><Picture /></el-icon>
          <span>暂无封面</span>
        </div>
      </div>
      <div class="course-info">
        <h1>{{ course.courseName || course.name || '未命名课程' }}</h1>
        <p class="course-desc">{{ course.description || '暂无描述' }}</p>
        <div class="course-meta">
          <span class="meta-item">
            <el-icon><User /></el-icon>
            {{ course.teacherName || '未知讲师' }}
          </span>
          <span class="meta-item">
            <el-icon><View /></el-icon>
            {{ course.studentCount || 0 }} 人学习
          </span>
          <span class="meta-item">
            <el-icon><Timer /></el-icon>
            {{ course.durationHours || 0 }} 小时
          </span>
        </div>
        <div class="course-actions" v-if="isStudent">
          <el-button type="primary" size="large" @click="startLearning">立即学习</el-button>
          <el-button :icon="Star" @click="toggleFavorite">{{ isFavorited ? '已收藏' : '收藏' }}</el-button>
        </div>
        <div class="teacher-actions" v-if="isTeacher">
          <el-button type="primary" :icon="Plus" @click="showAddChapter = true">添加章节</el-button>
          <el-button type="warning" @click="handlePublish" v-if="course.status === 0">提交审核</el-button>
        </div>
      </div>
    </div>

    <div class="course-content">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="课程目录" name="chapters">
          <div class="chapter-list">
            <div v-for="chapter in chapterList" :key="chapter.id" class="chapter-item">
              <div class="chapter-header">
                <div class="chapter-title">{{ chapter.chapterName || chapter.name }}</div>
                <div class="chapter-meta">
                  <el-tag size="small" type="info">{{ (chapter.lessons || []).length }} 课时</el-tag>
                  <el-button
                    v-if="isTeacher"
                    type="primary"
                    link
                    size="small"
                    :icon="Plus"
                    @click.stop="showAddLesson = true; currentChapterId = chapter.id"
                  >+课时</el-button>
                </div>
              </div>
              <div class="lesson-list">
                <div
                  v-for="lesson in (chapter.lessons || [])"
                  :key="lesson.id"
                  class="lesson-item"
                >
                  <div class="lesson-main" @click="goToStudy(lesson.id)">
                    <el-icon class="lesson-icon"><VideoPlay /></el-icon>
                    <span class="lesson-name">{{ lesson.lessonName || lesson.name || videoFileName(lesson.videoUrl) }}</span>
                    <el-tag v-if="lesson.isCompleted" type="success" size="small">已完成</el-tag>
                    <span class="lesson-duration">{{ lesson.duration || '' }}</span>
                  </div>
                  <!-- ==================== 关键修改：教师操作按钮组 ==================== -->
                  <div class="lesson-actions" v-if="isTeacher">
                    <el-button
                      type="primary"
                      link
                      size="small"
                      :icon="Plus"
                      @click.stop="openQuestionDialog(lesson)"
                    >+题目</el-button>
                    <el-button
                      type="info"
                      link
                      size="small"
                      @click.stop="goToLessonQuestions(lesson)"
                    >题目管理</el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="课程介绍" name="intro">
          <div class="intro-content">{{ course.description || '暂无介绍' }}</div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- ==================== 新增：课时题目导入弹窗 ==================== -->
    <el-dialog
      v-model="showQuestionDialog"
      :title="`为「${currentLesson?.lessonName || currentLesson?.name}」导入题目`"
      width="800px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div v-loading="dialogLoading">
        <!-- 顶部筛选 -->
        <div class="dialog-filter">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索题目内容"
            size="small"
            clearable
            style="width: 200px"
            @keyup.enter="fetchBankQuestions"
          />
          <el-select v-model="filterType" placeholder="题型" size="small" clearable style="width: 120px">
            <el-option label="单选题" :value="1" />
            <el-option label="多选题" :value="2" />
            <el-option label="判断题" :value="3" />
            <el-option label="填空题" :value="4" />
            <el-option label="简答题" :value="5" />
          </el-select>
          <el-button type="primary" size="small" @click="fetchBankQuestions">搜索</el-button>
          <el-tag type="success" v-if="selectedQuestionIds.length > 0">
            已选 {{ selectedQuestionIds.length }} 题
          </el-tag>
        </div>

        <!-- 已关联题目提示 -->
        <div v-if="linkedQuestionIds.length > 0" class="linked-hint">
          <el-tag type="info" size="small">该课时已关联 {{ linkedQuestionIds.length }} 题</el-tag>
        </div>

        <!-- 题库列表 -->
        <el-empty v-if="!dialogLoading && bankQuestions.length === 0" description="暂无可用题目" />
        <div v-else class="dialog-question-list">
          <div
            v-for="item in bankQuestions"
            :key="item.id"
            class="dialog-question-item"
            :class="{ 'is-linked': isLinked(item.id), 'is-selected': selectedQuestionIds.includes(item.id) }"
            @click="toggleSelect(item.id)"
          >
            <div class="item-checkbox">
              <el-checkbox
                :model-value="selectedQuestionIds.includes(item.id)"
                :disabled="isLinked(item.id)"
                @click.stop
                @change="toggleSelect(item.id)"
              />
            </div>
            <div class="item-content">
              <div class="item-header">
                <el-tag :type="getQuestionTypeTag(item.questionType)" size="small">
                  {{ getQuestionTypeName(item.questionType) }}
                </el-tag>
                <el-tag v-if="isLinked(item.id)" type="success" size="small">已关联</el-tag>
              </div>
              <p class="item-text">{{ item.questionContent }}</p>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div class="dialog-pagination" v-if="bankTotal > 0">
          <el-pagination
            v-model:current-page="pageCurrent"
            v-model:page-size="pageSize"
            :total="bankTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, prev, pager, next"
            @current-change="fetchBankQuestions"
          />
        </div>
      </div>

      <template #footer>
        <el-button @click="showQuestionDialog = false">取消</el-button>
        <el-button
          type="primary"
          @click="handleBatchAdd"
          :loading="batchAdding"
          :disabled="selectedQuestionIds.length === 0"
        >
          导入 {{ selectedQuestionIds.length }} 道题目
        </el-button>
      </template>
    </el-dialog>

    <!-- ==================== 新增：添加章节弹窗 ==================== -->
    <el-dialog
      v-model="showAddChapter"
      title="添加章节"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="chapterForm" label-width="80px">
        <el-form-item label="章节名称" required>
          <el-input v-model="chapterForm.name" placeholder="请输入章节名称" />
        </el-form-item>
        <el-form-item label="章节序号">
          <el-input-number v-model="chapterForm.sortOrder" :min="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddChapter = false">取消</el-button>
        <el-button type="primary" @click="handleAddChapter" :loading="chapterSubmitting">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- ==================== 新增：添加课时弹窗 ==================== -->
    <el-dialog
      v-model="showAddLesson"
      title="添加课时"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="lessonForm" label-width="80px">
        <el-form-item label="所属章节">
          <el-select v-model="lessonForm.chapterId" placeholder="请选择章节" style="width: 100%">
            <el-option
              v-for="ch in chapterList"
              :key="ch.id"
              :label="ch.chapterName || ch.name"
              :value="ch.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="上传视频">
          <el-upload
            class="video-uploader"
            action="/api/upload/video"
            :headers="uploadHeaders"
            accept="video/*"
            :limit="1"
            :on-success="handleVideoSuccess"
            :on-error="handleVideoError"
            :before-upload="beforeVideoUpload"
          >
            <el-button type="primary">点击上传视频</el-button>
            <template #tip>
              <div class="el-upload__tip">支持 MP4、AVI、MOV 等视频格式，单个文件不超过 1GB</div>
            </template>
          </el-upload>
          <el-tag v-if="lessonForm.videoUrl" type="success" size="small" style="margin-top: 8px">
            {{ lessonForm.videoUrl.split('/').pop() }}
          </el-tag>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddLesson = false">取消</el-button>
        <el-button type="primary" @click="handleAddLesson" :loading="lessonSubmitting">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { User, View, Timer, VideoPlay, Star, Plus, Document, Picture } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getCourseDetail, getCourseChapters, addChapter, addLesson } from '@/api/course'
import { getLessonQuestions, addQuestionToLesson } from '@/api/study'
import { getQuestionList } from '@/api/exam'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const courseId = Number(route.params.id)

const course = ref<any>({})
const chapterList = ref<any[]>([])
const activeTab = ref('chapters')
const isFavorited = ref(false)
const loading = ref(false)
const coverImageSrc = ref('')

// 弹窗控制
const showAddChapter = ref(false)
const showAddLesson = ref(false)
const currentChapterId = ref<number | null>(null)

// ==================== 新增：章节/课时表单 ====================
const chapterForm = reactive({
  name: '',
  sortOrder: 1,
  courseId: courseId
})

const lessonForm = reactive({
  name: '',
  chapterId: null as number | null,
  videoUrl: '',
  courseId: courseId
})

const chapterSubmitting = ref(false)
const lessonSubmitting = ref(false)

// 上传视频时携带 token
const uploadHeaders = computed(() => {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
})

// 视频上传前校验
const beforeVideoUpload = (file: File) => {
  // 先检查 token
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.error('未登录，请先登录')
    return false
  }

  const isVideo = file.type.startsWith('video/')
  const isLt1G = file.size / 1024 / 1024 <= 1024

  if (!isVideo) {
    ElMessage.error('请上传视频文件')
    return false
  }
  if (!isLt1G) {
    ElMessage.error('视频大小不能超过 1GB')
    return false
  }
  return true
}

// 视频上传成功
const handleVideoSuccess = (res: any) => {
  console.log('[上传成功] 响应:', res)
  if (res.code === 200 && res.data) {
    lessonForm.videoUrl = res.data
    ElMessage.success('视频上传成功')
  } else {
    ElMessage.error(res.message || '视频上传失败')
  }
}

// 视频上传失败
const handleVideoError = (err: any) => {
  console.error('[上传失败] 完整错误:', err)
  // 检查是否是 401 导致的重定向
  if (typeof err === 'string' && err.includes('401')) {
    ElMessage.error('上传未授权，请检查登录状态或后端是否放行 /api/upload/**')
  } else {
    ElMessage.error('视频上传失败，请检查网络或后端接口')
  }
}

// 打开添加课时弹窗时自动设置章节
watch(showAddLesson, (val) => {
  if (val && currentChapterId.value) {
    lessonForm.chapterId = currentChapterId.value
  }
})

// 使用 userStore 判断角色（比 localStorage 更可靠）
const isTeacher = computed(() => userStore.isTeacher)
const isStudent = computed(() => userStore.isStudent)

// ==================== 新增：题目导入弹窗状态 ====================
const showQuestionDialog = ref(false)
const dialogLoading = ref(false)
const batchAdding = ref(false)
const currentLesson = ref<any>(null)
const bankQuestions = ref<any[]>([])
const bankTotal = ref(0)
const pageCurrent = ref(1)
const pageSize = ref(10)
const searchKeyword = ref('')
const filterType = ref<number | undefined>(undefined)
const linkedQuestionIds = ref<number[]>([])
const selectedQuestionIds = ref<number[]>([])

const parseRes = (res: any): any => {
  if (!res) return null
  if (res.data && res.data.code === 200) return res.data.data
  if (res.code === 200) return res.data
  return null
}

const isSuccess = (res: any): boolean => {
  if (!res) return false
  if (res.data && res.data.code === 200) return true
  if (typeof res.code === 'number' && res.code === 200) return true
  return false
}

const fetchCourseDetail = async () => {
  try {
    const res = await getCourseDetail(courseId)
    const data = parseRes(res)
    if (data) {
      course.value = data
      coverImageSrc.value = data.coverUrl || data.cover || data.coverImage || ''
    }
  } catch (e) {
    ElMessage.error('获取课程详情失败')
  }
}

const fetchChapters = async () => {
  try {
    const res = await getCourseChapters(courseId)
    const data = parseRes(res)
    if (Array.isArray(data)) chapterList.value = data
  } catch (e) {
    console.error('获取章节失败:', e)
  }
}

const startLearning = () => {
  for (const ch of chapterList.value) {
    const lessons = ch.lessons || []
    if (lessons.length > 0) {
      router.push(`/study/${lessons[0].id}`)
      return
    }
  }
  ElMessage.warning('课程暂无内容')
}

const goToStudy = (lessonId: number) => {
  router.push(`/study/${lessonId}`)
}

// ==================== 修改：题目管理跳转（修复404）====================
const goToLessonQuestions = (lesson: any) => {
  router.push({
    path: `/teacher/lessons/${lesson.id}/questions`,
    query: {
      courseId: courseId,
      courseName: course.value.courseName || '',
      lessonName: lesson.lessonName || lesson.name || ''
    }
  })
}

const goToQuestionManage = () => {
  for (const ch of chapterList.value) {
    const lessons = ch.lessons || []
    if (lessons.length > 0) {
      router.push({
        path: `/teacher/lessons/${lessons[0].id}/questions`,
        query: {
          courseId: courseId,
          courseName: course.value.courseName || '',
          lessonName: lessons[0].lessonName || lessons[0].name || ''
        }
      })
      return
    }
  }
  ElMessage.warning('请先添加课时')
}

// ==================== 新增：题目导入弹窗逻辑 ====================
const openQuestionDialog = async (lesson: any) => {
  currentLesson.value = lesson
  selectedQuestionIds.value = []
  linkedQuestionIds.value = []
  searchKeyword.value = ''
  filterType.value = undefined
  pageCurrent.value = 1
  showQuestionDialog.value = true

  // 并行获取：已关联题目 + 题库
  dialogLoading.value = true
  try {
    await Promise.all([
      fetchLinkedQuestions(lesson.id),
      fetchBankQuestions()
    ])
  } finally {
    dialogLoading.value = false
  }
}

const fetchLinkedQuestions = async (lessonId: number) => {
  try {
    const res = await getLessonQuestions(lessonId) as any
    const data = parseRes(res)
    if (Array.isArray(data)) {
      linkedQuestionIds.value = data.map((q: any) => q.id)
    }
  } catch (e) {
    console.error('获取已关联题目失败:', e)
  }
}

const fetchBankQuestions = async () => {
  try {
    const params: any = {
      current: pageCurrent.value,
      size: pageSize.value
    }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (filterType.value !== undefined) params.questionType = filterType.value

    const res = await getQuestionList(params) as any
    const data = parseRes(res)
    if (data?.records && Array.isArray(data.records)) {
      bankQuestions.value = data.records
      bankTotal.value = data.total || 0
    } else if (Array.isArray(data)) {
      bankQuestions.value = data
      bankTotal.value = data.length
    } else {
      bankQuestions.value = []
      bankTotal.value = 0
    }
  } catch (e) {
    console.error('获取题库题目失败:', e)
    ElMessage.error('获取题库题目失败')
  }
}

const isLinked = (questionId: number): boolean => {
  return linkedQuestionIds.value.includes(questionId)
}

const toggleSelect = (questionId: number) => {
  if (isLinked(questionId)) return
  const idx = selectedQuestionIds.value.indexOf(questionId)
  if (idx > -1) {
    selectedQuestionIds.value.splice(idx, 1)
  } else {
    selectedQuestionIds.value.push(questionId)
  }
}

const handleBatchAdd = async () => {
  if (!currentLesson.value?.id || selectedQuestionIds.value.length === 0) return

  batchAdding.value = true
  let successCount = 0
  let failCount = 0

  for (const questionId of selectedQuestionIds.value) {
    try {
      const order = linkedQuestionIds.value.length + successCount + 1
      const res = await addQuestionToLesson(currentLesson.value.id, questionId, order)
      if (isSuccess(res)) {
        successCount++
        linkedQuestionIds.value.push(questionId)
      } else {
        failCount++
      }
    } catch (e) {
      failCount++
    }
  }

  batchAdding.value = false
  selectedQuestionIds.value = []

  if (successCount > 0) {
    ElMessage.success(`成功导入 ${successCount} 道题目${failCount > 0 ? `，${failCount} 道失败` : ''}`)
  } else {
    ElMessage.error('导入失败，请检查网络')
  }
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

const videoFileName = (url?: string): string => {
  if (!url) return '新课时'
  const name = url.split('/').pop()?.split('.')[0]
  return name || '新课时'
}

const handlePublish = () => {
  ElMessage.success('提交审核成功')
}

const toggleFavorite = () => {
  isFavorited.value = !isFavorited.value
  ElMessage.success(isFavorited.value ? '收藏成功' : '取消收藏')
}

// ==================== 新增：添加章节/课时提交方法 ====================
const handleAddChapter = async () => {
  if (!chapterForm.name.trim()) {
    ElMessage.warning('请输入章节名称')
    return
  }
  chapterSubmitting.value = true
  try {
    const res = await addChapter({
      courseId: courseId,
      chapterName: chapterForm.name,
      sortOrder: chapterForm.sortOrder
    })
    if (isSuccess(res)) {
      ElMessage.success('章节添加成功')
      chapterForm.name = ''
      chapterForm.sortOrder = 1
      showAddChapter.value = false
      fetchChapters()
    } else {
      ElMessage.error('添加失败')
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '添加失败')
  } finally {
    chapterSubmitting.value = false
  }
}

const handleAddLesson = async () => {
  if (!lessonForm.chapterId) {
    ElMessage.warning('请选择所属章节')
    return
  }

  // 从视频URL提取文件名作为课时名称
  const videoFileName = lessonForm.videoUrl
    ? lessonForm.videoUrl.split('/').pop()?.split('.')[0] || '新课时'
    : '新课时'

  const payload = {
    courseId: courseId,
    chapterId: lessonForm.chapterId,
    lessonName: videoFileName,
    videoUrl: lessonForm.videoUrl || undefined
  }

  lessonSubmitting.value = true
  try {
    const res = await addLesson(payload)
    if (isSuccess(res)) {
      ElMessage.success('课时添加成功')
      lessonForm.videoUrl = ''
      lessonForm.chapterId = null
      showAddLesson.value = false
      fetchChapters()
    } else {
      const msg = res?.data?.message || '添加失败'
      ElMessage.error(msg)
    }
  } catch (e: any) {
    console.error('[handleAddLesson] 异常:', e)
    if (!e?.handledByInterceptor) {
      ElMessage.error(e?.message || '添加失败')
    }
  } finally {
    lessonSubmitting.value = false
  }
}

// 图片加载失败处理（解决TypeScript类型问题）
const handleImageError = () => {
  coverImageSrc.value = '' // 清空，显示 "暂无封面" 占位
}

onMounted(() => {
  fetchCourseDetail()
  fetchChapters()
})
</script>

<style scoped>
.course-detail-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.course-header {
  display: flex;
  gap: 24px;
  margin-bottom: 24px;
}

.course-cover {
  width: 360px;
  height: 200px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
  background: #f0f2f5;
}

.course-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #909399;
  font-size: 14px;
}

.course-info {
  flex: 1;
}

.course-info h1 {
  margin: 0 0 12px 0;
  font-size: 24px;
}

.course-desc {
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 12px;
}

.course-meta {
  display: flex;
  gap: 20px;
  margin-bottom: 16px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #909399;
}

.course-actions {
  display: flex;
  gap: 10px;
}

.teacher-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.course-content {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
}

.chapter-item {
  margin-bottom: 16px;
}

.chapter-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 8px;
}

.chapter-title {
  font-weight: 600;
  font-size: 16px;
  color: #303133;
}

.chapter-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.lesson-list {
  padding-left: 12px;
}

.lesson-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.lesson-item:hover {
  background-color: #f5f7fa;
}

.lesson-main {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}

.lesson-icon {
  color: #409EFF;
}

.lesson-name {
  font-size: 14px;
  color: #606266;
}

.lesson-duration {
  font-size: 12px;
  color: #909399;
  margin-left: auto;
}

.lesson-actions {
  display: flex;
  gap: 8px;
}

.intro-content {
  color: #606266;
  line-height: 1.8;
  font-size: 14px;
}

/* ==================== 弹窗样式 ==================== */
.dialog-filter {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.linked-hint {
  margin-bottom: 10px;
}

.dialog-question-list {
  max-height: 50vh;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.dialog-question-item {
  display: flex;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.dialog-question-item:hover {
  border-color: #409EFF;
  background-color: #f5f7fa;
}

.dialog-question-item.is-linked {
  opacity: 0.6;
  cursor: not-allowed;
  background-color: #f0f9eb;
  border-color: #b3e19d;
}

.dialog-question-item.is-selected:not(.is-linked) {
  border-color: #409EFF;
  background-color: #ecf5ff;
}

.item-checkbox {
  display: flex;
  align-items: center;
  padding-top: 2px;
}

.item-content {
  flex: 1;
  min-width: 0;
}

.item-header {
  display: flex;
  gap: 8px;
  margin-bottom: 6px;
}

.item-text {
  font-size: 13px;
  color: #303133;
  line-height: 1.5;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
}

.dialog-pagination {
  margin-top: 12px;
  display: flex;
  justify-content: center;
}

/* 视频上传区域样式 */
.video-uploader {
  display: flex;
  flex-direction: column;
}

.video-uploader .el-upload__tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>