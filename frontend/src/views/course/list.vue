<template>
  <div class="course-list-container">
    <!-- 未加入班级提示 -->
    <el-alert
      v-if="!loading && !inAnyClass && userStore.isStudent"
      title="您需要先加入班级才能选课"
      type="warning"
      show-icon
      :closable="false"
      class="class-warning"
    >
      <template #default>
        <span>请联系您的老师获取班级加入码，</span>
        <el-button type="primary" link @click="$router.push('/my-classes')">去加入班级</el-button>
      </template>
    </el-alert>

    <!-- 搜索栏 -->
    <div class="search-section">
      <el-input
        v-model="searchForm.keyword"
        placeholder="搜索课程名称"
        class="search-input"
        clearable
        @keyup.enter="handleSearch"
      >
        <template #append>
          <el-button :icon="Search" @click="handleSearch">搜索</el-button>
        </template>
      </el-input>
      
      <el-select v-model="searchForm.subjectId" placeholder="学科" clearable @change="handleSearch">
        <el-option label="语文" :value="1" />
        <el-option label="数学" :value="2" />
        <el-option label="英语" :value="3" />
        <el-option label="物理" :value="4" />
        <el-option label="化学" :value="5" />
      </el-select>
      
      <el-select v-model="searchForm.educationStage" placeholder="教育阶段" clearable @change="handleSearch">
        <el-option label="小学" :value="1" />
        <el-option label="初中" :value="2" />
        <el-option label="高中" :value="3" />
      </el-select>
    </div>
    
    <!-- 课程列表 -->
    <el-row :gutter="20" v-loading="loading">
      <el-col :span="6" v-for="course in courseList" :key="course.id" class="course-item">
        <el-card shadow="hover">
          <!-- 修复：使用 getImageUrl 统一处理封面图片路径 -->
          <img :src="getImageUrl(course.coverImage)" class="course-cover" @click="goToDetail(course.id)" />
          <div class="course-info">
            <h4 class="course-name">{{ course.courseName }}</h4>
            <p class="course-teacher">{{ course.teacherName }}</p>
            <div class="course-meta">
              <span class="course-views">
                <el-icon><View /></el-icon>
                {{ course.viewCount || 0 }}
              </span>
              <span class="course-lessons">{{ course.lessonCount || 0 }}课时</span>
            </div>
            <div class="course-tags">
              <el-tag size="small" v-if="course.educationStageName">{{ course.educationStageName }}</el-tag>
              <el-tag size="small" type="success" v-if="course.difficultyLevelName">{{ course.difficultyLevelName }}</el-tag>
            </div>
            <!-- 选课按钮 -->
            <div class="course-actions" v-if="userStore.isStudent">
              <el-button 
                type="primary" 
                :disabled="!inAnyClass || selectedCourses.includes(course.id)"
                @click.stop="handleSelectCourse(course.id)"
              >
                {{ selectedCourses.includes(course.id) ? '已选课' : '选课' }}
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 空状态 -->
    <el-empty v-if="!loading && courseList.length === 0" description="暂无课程" />
    
    <!-- 分页 -->
    <div class="pagination-section" v-if="pagination.total > 0">
      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[8, 12, 16, 20]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, View } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getPublicCourses, selectCourse } from '@/api/course'
import { checkStudentInAnyClass } from '@/api/class'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

// ==================== 图片 URL 统一处理（新增）====================
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
const inAnyClass = ref(false)
const courseList = ref<any[]>([])
const selectedCourses = ref<number[]>([])

const searchForm = reactive({
  keyword: '',
  subjectId: undefined as number | undefined,
  educationStage: undefined as number | undefined
})

const pagination = reactive({
  current: 1,
  size: 12,
  total: 0
})

// 检查学生是否加入了班级
const checkInClass = async () => {
  if (!userStore.isStudent) return
  try {
    const res = await checkStudentInAnyClass()
    if (res.data.code === 200) {
      inAnyClass.value = res.data.data
    }
  } catch (error) {
    console.error('检查班级状态失败:', error)
  }
}

const fetchCourses = async () => {
  loading.value = true
  try {
    const res = await getPublicCourses({
      keyword: searchForm.keyword || undefined,
      subjectId: searchForm.subjectId,
      educationStage: searchForm.educationStage,
      current: pagination.current,
      size: pagination.size
    })
    if (res.data.code === 200) {
      courseList.value = res.data.data.records || []
      pagination.total = res.data.data.total || 0
    }
  } catch (error) {
    console.error('获取课程列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 选课（使用course.ts中的selectCourse）
const handleSelectCourse = async (courseId: number) => {
  if (!inAnyClass.value) {
    ElMessage.warning('您需要先加入班级才能选课')
    return
  }
  
  try {
    const res = await selectCourse(courseId)
    if (res.data.code === 200) {
      ElMessage.success('选课成功')
      selectedCourses.value.push(courseId)
    } else {
      ElMessage.warning(res.data.message || '选课失败')
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '选课失败')
  }
}

const handleSearch = () => {
  pagination.current = 1
  fetchCourses()
}

const handleSizeChange = (val: number) => {
  pagination.size = val
  fetchCourses()
}

const handleCurrentChange = (val: number) => {
  pagination.current = val
  fetchCourses()
}

const goToDetail = (id: number) => {
  router.push(`/course/${id}`)
}

onMounted(() => {
  checkInClass()
  fetchCourses()
})
</script>

<style scoped>
.course-list-container {
  padding: 20px;
}

.class-warning {
  margin-bottom: 20px;
}

.search-section {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
}

.search-input {
  width: 300px;
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
  margin: 0 0 8px 0;
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
  margin-bottom: 10px;
}

.course-views {
  display: flex;
  align-items: center;
  gap: 5px;
}

.course-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.course-actions {
  margin-top: 10px;
}

.pagination-section {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>