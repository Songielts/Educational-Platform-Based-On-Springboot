<template>
  <div class="course-detail-container">
    <!-- 课程头部信息 -->
    <div class="course-header">
      <el-row :gutter="20">
        <el-col :span="10">
          <el-image :src="getImageUrl(course.coverImage)" class="course-cover" fit="cover" />
        </el-col>
        <el-col :span="14">
          <div class="course-meta">
            <h1 class="course-title">{{ course.courseName }}</h1>
            <p class="course-desc">{{ course.description || '暂无描述' }}</p>
            <div class="course-stats">
              <span><el-icon><User /></el-icon> {{ course.teacherName || '未知教师' }}</span>
              <span><el-icon><View /></el-icon> {{ course.viewCount || 0 }} 次浏览</span>
              <span><el-icon><Timer /></el-icon> {{ chapterList.length }} 章 {{ totalLessons }} 课时</span>
            </div>
            <div class="course-tags">
              <el-tag type="info" size="small">{{ course.educationStageName }}</el-tag>
              <el-tag :type="getDifficultyType(course.difficultyLevel)" size="small">{{ course.difficultyLevelName }}</el-tag>
              <el-tag :type="getStatusType(course.status)" size="small">{{ course.statusName }}</el-tag>
            </div>
            <!-- 教师操作按钮 -->
            <div class="teacher-actions" v-if="isTeacher">
              <el-button type="primary" :icon="Plus" @click="showAddChapter = true">添加章节</el-button>
              <el-button type="success" :icon="Plus" @click="showAddLesson = true" v-if="chapterList.length > 0">添加课时</el-button>
              <el-button type="warning" @click="handlePublish" v-if="course.status === 0">提交审核</el-button>
              <el-button type="info" :icon="Document" @click="goToQuestionManage" v-if="chapterList.length > 0">题目管理</el-button>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 课程目录 -->
    <div class="course-content">
      <div class="content-header">
        <h3>课程目录</h3>
        <span class="content-tip" v-if="isTeacher && chapterList.length === 0">暂无章节，请点击上方"添加章节"按钮</span>
      </div>

      <el-empty description="暂无章节" v-if="chapterList.length === 0" />

      <el-collapse v-model="activeChapters" v-else>
        <el-collapse-item
          v-for="chapter in chapterList"
          :key="chapter.id"
          :name="chapter.id"
        >
          <template #title>
            <div class="chapter-title">
              <span class="chapter-name">{{ chapter.chapterName || chapter.name || '未命名章节' }}</span>
              <span class="chapter-count">{{ (chapter.lessons || []).length }} 课时</span>
              <el-button
                v-if="isTeacher"
                type="primary"
                link
                size="small"
                @click.stop="openAddLesson(chapter.id)"
              >+ 课时</el-button>
              <el-button
                v-if="isTeacher"
                type="warning"
                link
                size="small"
                @click.stop="openEditChapter(chapter)"
              >编辑</el-button>
              <el-button
                v-if="isTeacher"
                type="danger"
                link
                size="small"
                @click.stop="handleDeleteChapter(chapter.id)"
              >删除</el-button>
            </div>
          </template>

          <div class="lesson-list">
            <div
              v-for="lesson in (chapter.lessons || [])"
              :key="lesson.id"
              class="lesson-item"
            >
              <div class="lesson-main" @click="goToStudy(lesson.id)">
                <el-icon class="lesson-icon"><VideoPlay /></el-icon>
                <span class="lesson-name">{{ lesson.lessonName || lesson.name || '未命名课时' }}</span>
                <el-tag v-if="lesson.isCompleted" type="success" size="small">已完成</el-tag>
                <span class="lesson-duration">{{ lesson.duration || '' }}</span>
              </div>
              <div class="lesson-actions" v-if="isTeacher">
                <el-button type="primary" link size="small" @click.stop="goToLessonQuestions(lesson)">题目</el-button>
                <el-button type="danger" link size="small" @click.stop="handleDeleteLesson(lesson.id)">删除</el-button>
              </div>
            </div>
            <el-empty v-if="!(chapter.lessons || []).length" description="暂无课时" :image-size="60" />
          </div>
        </el-collapse-item>
      </el-collapse>
    </div>

    <!-- 添加章节对话框 -->
    <el-dialog v-model="showAddChapter" title="添加章节" width="500px">
      <el-form :model="chapterForm" ref="chapterFormRef" :rules="chapterRules" label-width="80px">
        <el-form-item label="章节名称" prop="name">
          <el-input v-model="chapterForm.name" placeholder="请输入章节名称" />
        </el-form-item>
        <el-form-item label="章节描述">
          <el-input v-model="chapterForm.description" type="textarea" :rows="3" placeholder="请输入章节描述" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="chapterForm.sortOrder" :min="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddChapter = false">取消</el-button>
        <el-button type="primary" @click="submitChapter">确认</el-button>
      </template>
    </el-dialog>

    <!-- 编辑章节对话框（新增） -->
    <el-dialog v-model="showEditChapter" title="编辑章节" width="500px">
      <el-form :model="editChapterForm" ref="editChapterFormRef" :rules="chapterRules" label-width="80px">
        <el-form-item label="章节名称" prop="name">
          <el-input v-model="editChapterForm.name" placeholder="请输入章节名称" />
        </el-form-item>
        <el-form-item label="章节描述">
          <el-input v-model="editChapterForm.description" type="textarea" :rows="3" placeholder="请输入章节描述" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="editChapterForm.sortOrder" :min="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditChapter = false">取消</el-button>
        <el-button type="primary" @click="submitEditChapter">保存</el-button>
      </template>
    </el-dialog>

    <!-- 添加课时对话框 -->
    <el-dialog v-model="showAddLesson" title="添加课时" width="500px">
      <el-form :model="lessonForm" ref="lessonFormRef" :rules="lessonRules" label-width="80px">
        <el-form-item label="所属章节" prop="chapterId">
          <el-select v-model="lessonForm.chapterId" placeholder="请选择章节" style="width: 100%">
            <el-option
              v-for="ch in chapterList"
              :key="ch.id"
              :label="ch.chapterName || ch.name"
              :value="ch.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="课时名称" prop="name">
          <el-input v-model="lessonForm.name" placeholder="请输入课时名称" />
        </el-form-item>
        <el-form-item label="视频上传">
          <el-upload
            class="video-uploader"
            action="/api/upload/video"
            accept=".mp4,.webm,.mov"
            :show-file-list="false"
            :on-success="handleVideoSuccess"
            :on-error="handleVideoError"
            :before-upload="beforeVideoUpload"
          >
            <el-button type="primary">选择视频</el-button>
          </el-upload>
          <div v-if="lessonForm.videoUrl" class="video-info">
            <el-tag type="success" size="small">视频已上传</el-tag>
            <span class="video-url">{{ lessonForm.videoUrl }}</span>
          </div>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="lessonForm.sortOrder" :min="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddLesson = false">取消</el-button>
        <el-button type="primary" @click="submitLesson">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import { User, View, Timer, VideoPlay, Document } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getCourseDetail,
  getCourseChapters,
  publishCourse,
  addChapter,
  addLesson,
  deleteChapter,
  deleteLesson,
  updateChapter
} from '@/api/course'

const route = useRoute()
const router = useRouter()
const courseId = Number(route.params.id)

// 统一处理课程封面图片路径
const getImageUrl = (url: string | undefined): string => {
  if (!url) return '/images/default-course.png'
  if (url.startsWith('http')) return url
  const normalized = url.replace(/\/+/g, '/')
  if (normalized.startsWith('/uploads/')) {
    return '/api/upload/file/stream?path=' + encodeURIComponent(normalized)
  }
  if (normalized.startsWith('/images/')) return normalized
  if (normalized === '/default-course.png') return '/images/default-course.png'
  return '/images/' + normalized.replace(/^\/+/, '')
}

// 当前用户角色（从localStorage或userStore获取）
const userRole = ref(localStorage.getItem('userRole') || '')
const isTeacher = computed(() => userRole.value === 'TEACHER')

const loading = ref(false)
const course = ref<any>({})
const chapterList = ref<any[]>([])
const activeChapters = ref<number[]>([])

// 添加章节
const showAddChapter = ref(false)
const chapterFormRef = ref<FormInstance>()
const chapterForm = reactive({ name: '', description: '', sortOrder: 1 })
const chapterRules: FormRules = { name: [{ required: true, message: '请输入章节名称', trigger: 'blur' }] }

// 编辑章节（新增）
const showEditChapter = ref(false)
const editChapterFormRef = ref<FormInstance>()
const editChapterId = ref<number | null>(null)
const editChapterForm = reactive({ name: '', description: '', sortOrder: 1 })

// 添加课时
const showAddLesson = ref(false)
const lessonFormRef = ref<FormInstance>()
const lessonForm = reactive({ chapterId: null as number | null, name: '', videoUrl: '', sortOrder: 1 })
const lessonRules: FormRules = {
  chapterId: [{ required: true, message: '请选择章节', trigger: 'change' }],
  name: [{ required: true, message: '请输入课时名称', trigger: 'blur' }]
}

const totalLessons = computed(() => {
  return chapterList.value.reduce((sum, ch) => sum + (ch.lessons || []).length, 0)
})

const getStatusType = (status: number) => {
  const types: Record<number, string> = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
  return types[status] || 'info'
}

const getDifficultyType = (level: number) => {
  if (level === 1) return 'success'
  if (level === 3) return 'danger'
  return 'warning'
}

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

const fetchCourse = async () => {
  try {
    const res = await getCourseDetail(courseId) as any
    const data = parseRes(res)
    if (data) course.value = data
  } catch (e: any) {
    ElMessage.error('获取课程信息失败')
  }
}

const fetchChapters = async () => {
  try {
    const res = await getCourseChapters(courseId) as any
    const data = parseRes(res)
    if (Array.isArray(data)) {
      chapterList.value = data
      if (data.length > 0) activeChapters.value = [data[0].id]
    }
  } catch (e: any) {
    console.error('获取章节失败:', e)
  }
}

const handlePublish = async () => {
  try {
    const res = await publishCourse(courseId)
    if (isSuccess(res)) {
      ElMessage.success('已提交审核')
      fetchCourse()
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '提交失败')
  }
}

const openAddLesson = (chapterId?: number) => {
  lessonForm.chapterId = chapterId || null
  lessonForm.name = ''
  lessonForm.videoUrl = ''
  lessonForm.sortOrder = 1
  showAddLesson.value = true
}

// ==================== 添加章节（修复字段名）====================
const submitChapter = async () => {
  const valid = await chapterFormRef.value?.validate().catch(() => false)
  if (!valid) return
  try {
    const res = await addChapter({
      courseId: Number(courseId),
      chapterName: chapterForm.name,       // 修复：后端 DTO 字段为 chapterName
      description: chapterForm.description,
      sortOrder: chapterForm.sortOrder
    })
    if (isSuccess(res)) {
      ElMessage.success('章节添加成功')
      showAddChapter.value = false
      chapterForm.name = ''
      chapterForm.description = ''
      chapterForm.sortOrder = 1
      fetchChapters()
    } else {
      ElMessage.error('添加失败')
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '添加失败')
  }
}

// ==================== 视频上传回调 ====================
const beforeVideoUpload = (file: File) => {
  const isVideo = ['video/mp4', 'video/webm', 'video/quicktime'].includes(file.type)
  const isLt1G = file.size / 1024 / 1024 <= 1024
  if (!isVideo) {
    ElMessage.error('请上传视频文件（MP4/WebM/MOV）')
  }
  if (!isLt1G) {
    ElMessage.error('视频大小不能超过 1GB')
  }
  return isVideo && isLt1G
}

const handleVideoSuccess = (res: any) => {
  const data = parseRes(res)
  if (data) {
    lessonForm.videoUrl = typeof data === 'string' ? data : (data.url || '')
    ElMessage.success('视频上传成功')
  } else {
    ElMessage.error('视频上传失败')
  }
}

const handleVideoError = () => {
  ElMessage.error('视频上传失败，请重试')
}

// ==================== 编辑章节（新增）====================
const openEditChapter = (chapter: any) => {
  editChapterId.value = chapter.id
  editChapterForm.name = chapter.chapterName || chapter.name || ''
  editChapterForm.description = chapter.description || ''
  editChapterForm.sortOrder = chapter.sortOrder || chapter.sort_order || 1
  showEditChapter.value = true
}

const submitEditChapter = async () => {
  const valid = await editChapterFormRef.value?.validate().catch(() => false)
  if (!valid || !editChapterId.value) return
  try {
    const res = await updateChapter(editChapterId.value, {
      courseId: Number(courseId),       // ← 补上课程ID
      chapterName: editChapterForm.name,
      description: editChapterForm.description,
      sortOrder: editChapterForm.sortOrder
    })
    if (isSuccess(res)) {
      ElMessage.success('章节修改成功')
      showEditChapter.value = false
      fetchChapters()
    } else {
      ElMessage.error('修改失败')
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '修改失败')
  }
}

// ==================== 添加课时（修复字段名）====================
const submitLesson = async () => {
  const valid = await lessonFormRef.value?.validate().catch(() => false)
  if (!valid) return
  try {
    const res = await addLesson({
      courseId: Number(courseId),
      chapterId: lessonForm.chapterId,
      lessonName: lessonForm.name,         // 修复：后端 DTO 字段为 lessonName
      videoUrl: lessonForm.videoUrl,
      sortOrder: lessonForm.sortOrder
    })
    if (isSuccess(res)) {
      ElMessage.success('课时添加成功')
      showAddLesson.value = false
      fetchChapters()
    } else {
      ElMessage.error('添加失败')
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '添加失败')
  }
}

// ==================== 删除章节（恢复）====================
const handleDeleteChapter = async (chapterId: number) => {
  try {
    await ElMessageBox.confirm('确定要删除该章节吗？章节下所有课时也会被删除', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = await deleteChapter(chapterId)
    if (isSuccess(res)) {
      ElMessage.success('删除成功')
      fetchChapters()
    } else {
      ElMessage.error('删除失败')
    }
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.message || '删除失败')
    }
  }
}

// ==================== 删除课时（恢复）====================
const handleDeleteLesson = async (lessonId: number) => {
  try {
    await ElMessageBox.confirm('确定要删除该课时吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = await deleteLesson(lessonId)
    if (isSuccess(res)) {
      ElMessage.success('删除成功')
      fetchChapters()
    } else {
      ElMessage.error('删除失败')
    }
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.message || '删除失败')
    }
  }
}

const goToStudy = (lessonId: number) => {
  router.push(`/study/${lessonId}`)
}

// ==================== 题目管理（跳转到独立页面）====================
const goToLessonQuestions = (lesson: any) => {
  router.push({
    path: '/teacher/lesson-questions',
    query: {
      courseId: courseId,
      lessonId: lesson.id,
      courseName: course.value.courseName || '',
      lessonName: lesson.lessonName || lesson.name || ''
    }
  })
}

const goToQuestionManage = () => {
  // 默认打开第一个章节的第一个课时的题目管理页面
  for (const ch of chapterList.value) {
    const lessons = ch.lessons || []
    if (lessons.length > 0) {
      goToLessonQuestions(lessons[0])
      return
    }
  }
  ElMessage.warning('请先添加课时')
}

onMounted(() => {
  if (!courseId || isNaN(courseId)) {
    ElMessage.error('课程ID无效')
    return
  }
  fetchCourse()
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
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.05);
}

.course-cover {
  width: 100%;
  height: 280px;
  border-radius: 8px;
  object-fit: cover;
}

.course-meta {
  padding: 10px 0;
}

.course-title {
  margin: 0 0 12px 0;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.course-desc {
  margin: 0 0 16px 0;
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}

.course-stats {
  display: flex;
  gap: 20px;
  margin-bottom: 12px;
  font-size: 14px;
  color: #909399;
}

.course-stats span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.course-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.teacher-actions {
  display: flex;
  gap: 10px;
  margin-top: 10px;
}

.course-content {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.05);
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.content-header h3 {
  margin: 0;
  font-size: 18px;
}

.content-tip {
  font-size: 13px;
  color: #909399;
}

.chapter-title {
  display: flex;
  align-items: center;
  width: 100%;
  padding-right: 20px;
}

.chapter-name {
  flex: 1;
  font-weight: 500;
}

.chapter-count {
  color: #909399;
  font-size: 13px;
  margin-right: 12px;
}

.lesson-list {
  padding: 8px 16px;
}

.lesson-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-radius: 6px;
  transition: background 0.2s;
  gap: 8px;
}

.lesson-item:hover {
  background: #f5f7fa;
}

.lesson-main {
  display: flex;
  align-items: center;
  flex: 1;
  cursor: pointer;
  min-width: 0;
}

.lesson-icon {
  color: #409EFF;
  margin-right: 8px;
  font-size: 16px;
}

.lesson-name {
  flex: 1;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lesson-duration {
  color: #909399;
  font-size: 13px;
  margin-left: 8px;
}

.lesson-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

/* 视频上传 */
.video-uploader {
  display: inline-block;
}

.video-info {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.video-url {
  font-size: 12px;
  color: #909399;
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>