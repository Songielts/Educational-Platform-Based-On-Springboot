<template>
  <div class="my-courses-container">
    <h2>我的课程</h2>
    
    <el-empty description="暂无学习中的课程" v-if="!loading && courseList.length === 0">
      <el-button type="primary" @click="$router.push('/courses')">去选课</el-button>
    </el-empty>
    
    <el-row :gutter="20" v-loading="loading" v-else>
      <el-col :span="6" v-for="course in courseList" :key="course.courseId || course.id" class="course-item">
        <el-card shadow="hover">
          <img :src="getImageUrl(course.coverImage)" class="course-cover" @click="goToCourse(course)" />
          <div class="course-info">
            <h4 class="course-name" @click="goToCourse(course)">{{ course.courseName }}</h4>
            <el-progress :percentage="course.completionRate || 0" :format="formatProgress" />
            <div class="course-stats">
              <span>已完成 {{ course.completedLessons || 0 }}/{{ course.totalLessons || 0 }} 课时</span>
              <span class="last-study">{{ course.lastStudyTimeStr || '未学习' }}</span>
            </div>
            <el-button type="primary" @click="continueStudy(course)">继续学习</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getStudentCourses } from '@/api/course'

const router = useRouter()

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

const loading = ref(false)
const courseList = ref<any[]>([])

const formatProgress = (percentage: number) => `${percentage}%`

// ==================== 统一解析后端响应 ====================
const parseRes = (res: any): any => {
  if (!res) return null
  if (res.data && typeof res.data.code === 'number' && res.data.code === 200) return res.data.data
  if (typeof res.code === 'number' && res.code === 200) return res.data
  if (Array.isArray(res) || (typeof res === 'object' && res !== null && !res.code)) return res
  return null
}

const fetchMyCourses = async () => {
  loading.value = true
  try {
    const res = await getStudentCourses() as any
    console.log('[fetchMyCourses] 原始响应:', res)

    const data = parseRes(res)
    console.log('[fetchMyCourses] 解析结果:', data)

    if (Array.isArray(data)) {
      courseList.value = data
    } else {
      courseList.value = []
      console.warn('[fetchMyCourses] 暂无课程数据')
    }
  } catch (error: any) {
    console.error('[fetchMyCourses] 获取我的课程失败:', error)
    ElMessage.error(error?.message || '获取我的课程失败')
    courseList.value = []
  } finally {
    loading.value = false
  }
}

const getCourseId = (course: any): number | null => {
  const id = course.courseId || course.id
  return id ? Number(id) : null
}

const goToCourse = (course: any) => {
  const courseId = getCourseId(course)
  if (!courseId || isNaN(courseId)) {
    ElMessage.error('课程ID无效')
    return
  }
  router.push(`/course/${courseId}`)
}

const continueStudy = (course: any) => {
  const courseId = getCourseId(course)
  if (!courseId || isNaN(courseId)) {
    ElMessage.error('课程ID无效')
    return
  }
  router.push(`/course/${courseId}`)
}

onMounted(() => {
  fetchMyCourses()
})
</script>

<style scoped>
.my-courses-container {
  padding: 20px;
}

.my-courses-container h2 {
  margin-bottom: 20px;
}

.course-item {
  margin-bottom: 20px;
}

.course-cover {
  width: 100%;
  height: 160px;
  object-fit: cover;
  border-radius: 4px;
  cursor: pointer;
}

.course-info {
  padding: 15px 0 0 0;
}

.course-name {
  margin: 0 0 15px 0;
  font-size: 16px;
  color: #303133;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.course-name:hover {
  color: #409EFF;
}

.course-stats {
  display: flex;
  justify-content: space-between;
  margin: 10px 0;
  font-size: 13px;
  color: #909399;
}

.last-study {
  color: #409EFF;
}
</style>