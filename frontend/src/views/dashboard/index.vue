<template>
  <div class="dashboard-container">
    <!-- 欢迎区域 -->
    <div class="welcome-section">
      <el-card>
        <div class="welcome-content">
          <div class="welcome-text">
            <!-- 修复1: 使用计算属性或可选链 + 类型断言 -->
            <h2>欢迎回来，{{ displayName }}!</h2>
            <p>今天是 {{ today }}，继续你的学习之旅吧！</p>
          </div>
          <!-- ... 其他代码保持不变 ... -->
        </div>
      </el-card>
    </div>
    <!-- ... 其他模板代码 ... -->
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import {
  Reading,
  Collection,
  EditPen,
  TrendCharts,
  View
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { getStudentStudyStat } from '@/api/study'
import { getPublicCourses } from '@/api/course'

// 修复2: 定义接口类型
interface UserInfo {
  realName?: string
  username?: string
  // 其他用户属性...
}

interface StudyStat {
  courseCount?: number
  completedCourseCount?: number
  totalStudyTimeStr?: string
  // 其他统计属性...
}

interface Course {
  id: number | string
  courseName: string
  teacherName?: string
  viewCount?: number
  lessonCount?: number
  coverImage?: string
  // 其他课程属性...
}

interface RecentCourse extends Course {
  courseId: number | string
  completionRate: number
  lastStudyTimeStr: string
}

// 修复3: 定义API响应类型
interface ApiResponse<T = any> {
  code: number
  message?: string
  data: T
}

const userStore = useUserStore()

// 修复4: 使用计算属性获取显示名称
const displayName = computed(() => {
  // 正确访问 ref 的 value
  return userStore.userInfo?.value?.realName || 
         userStore.userInfo?.value?.username || 
         '用户'
})

const today = computed(() => {
  const date = new Date()
  const weekDays = ['日', '一', '二', '三', '四', '五', '六']
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日 星期${weekDays[date.getDay()]}`
})

// 修复5: 添加类型定义
const studyStat = ref<StudyStat>({})
const recommendCourses = ref<Course[]>([])
const recentCourses = ref<RecentCourse[]>([])

const quickActions = computed(() => {
  // 修复6: 正确访问 isStudent 和 isTeacher
  if (userStore.isStudent?.value) {
    return [
      { name: '课程中心', icon: Reading, color: '#409EFF', path: '/courses' },
      { name: '我的课程', icon: Collection, color: '#67C23A', path: '/my-courses' },
      { name: '考试中心', icon: EditPen, color: '#E6A23C', path: '/exams' },
      { name: '学习统计', icon: TrendCharts, color: '#F56C6C', path: '/statistics' }
    ]
  } else if (userStore.isTeacher?.value) {
    return [
      { name: '班级管理', icon: Reading, color: '#409EFF', path: '/teacher/classes' },
      { name: '课程管理', icon: Collection, color: '#67C23A', path: '/teacher/courses' },
      { name: '题库管理', icon: EditPen, color: '#E6A23C', path: '/teacher/questions' },
      { name: '试卷管理', icon: TrendCharts, color: '#F56C6C', path: '/teacher/exams' }
    ]
  }
  return []
})

const fetchData = async () => {
  // 修复7: 正确访问 isStudent
  if (userStore.isStudent?.value) {
    try {
      // 修复8: 使用类型断言或正确的响应类型
      const statRes = await getStudentStudyStat() as unknown as ApiResponse<StudyStat>
      if (statRes.code === 200) {
        studyStat.value = statRes.data
      }
      
      // 修复9: 同样处理课程响应
      const courseRes = await getPublicCourses({ current: 1, size: 4 }) as unknown as ApiResponse<{ records: Course[] }>
      if (courseRes.code === 200) {
        recommendCourses.value = courseRes.data.records
      }
    } catch (error) {
      console.error('获取数据失败:', error)
    }
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
/* 样式保持不变 */
.dashboard-container {
  padding: 20px;
}

.welcome-section {
  margin-bottom: 20px;
}

.welcome-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.welcome-text h2 {
  margin: 0 0 10px 0;
  font-size: 24px;
  color: #303133;
}

.welcome-text p {
  margin: 0;
  color: #909399;
}

.welcome-stats {
  display: flex;
  gap: 40px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #409EFF;
}

.stat-label {
  margin-top: 5px;
  color: #909399;
  font-size: 14px;
}

.quick-actions {
  margin-bottom: 30px;
}

.action-card {
  cursor: pointer;
  transition: transform 0.3s;
}

.action-card:hover {
  transform: translateY(-5px);
}

.action-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
}

.action-name {
  margin-top: 15px;
  font-size: 16px;
  color: #606266;
}

.section {
  margin-bottom: 30px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.section-header h3 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.course-card {
  cursor: pointer;
  transition: transform 0.3s;
}

.course-card:hover {
  transform: translateY(-5px);
}

.course-cover {
  width: 100%;
  height: 150px;
  object-fit: cover;
  border-radius: 4px;
}

.course-info {
  padding: 15px 0 0 0;
}

.course-name {
  margin: 0 0 10px 0;
  font-size: 16px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.course-teacher {
  margin: 0 0 10px 0;
  color: #909399;
  font-size: 14px;
}

.course-meta {
  display: flex;
  justify-content: space-between;
  color: #909399;
  font-size: 13px;
}

.course-views {
  display: flex;
  align-items: center;
  gap: 5px;
}

.last-study {
  margin: 10px 0 0 0;
  color: #909399;
  font-size: 13px;
}
</style>