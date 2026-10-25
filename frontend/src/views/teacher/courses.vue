<template>
  <div class="teacher-courses-container">
    <div class="page-header">
      <h2>课程管理</h2>
      <el-button type="primary" :icon="Plus" @click="showAddDialog = true">创建课程</el-button>
    </div>

    <el-empty description="暂无课程" v-if="!loading && (!courseList || courseList.length === 0)" />

    <el-row :gutter="20" v-loading="loading" v-else>
      <el-col :span="6" v-for="course in courseList" :key="course.id" class="course-item">
        <el-card shadow="hover" :body-style="{ padding: '0' }">
          <div class="card-inner">
            <img :src="getImageUrl(course.coverImage)" class="course-cover" />
            <div class="course-info">
              <h4 class="course-name" :title="course.courseName">{{ course.courseName }}</h4>
              <el-tag :type="getStatusType(course.status)" size="small">{{ course.statusName || getStatusName(course.status) }}</el-tag>
              <div class="course-meta">
                <span>{{ course.subjectName || '' }}</span>
                <span>{{ course.viewCount || 0 }}浏览</span>
              </div>
            </div>
            <div class="course-actions">
              <el-button type="primary" link size="small" @click="viewCourse(course.id)">查看</el-button>
              <el-button type="warning" link size="small" @click="handleEdit(course)">编辑</el-button>
              <el-button type="success" link size="small" @click="handlePublish(course.id)" v-if="course.status === 0">发布</el-button>
              <el-button type="danger" link size="small" @click="handleDelete(course.id)">删除</el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 创建课程对话框 -->
    <el-dialog v-model="showAddDialog" title="创建课程" width="600px">
      <el-form :model="addForm" :rules="addRules" ref="addFormRef" label-width="100px">
        <el-form-item label="课程名称" prop="courseName">
          <el-input v-model="addForm.courseName" placeholder="请输入课程名称" />
        </el-form-item>
        <el-form-item label="课程封面">
          <el-upload
            class="cover-uploader"
            action="/api/upload/image"
            :show-file-list="false"
            :on-success="handleCoverSuccess"
          >
            <!-- 修复：封面预览使用 getImageUrl -->
            <img v-if="addForm.coverImage" :src="getImageUrl(addForm.coverImage)" class="cover-preview" />
            <el-icon v-else class="cover-uploader-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="学科">
          <el-select v-model="addForm.subjectId" placeholder="请选择学科">
            <el-option label="语文" :value="1" />
            <el-option label="数学" :value="2" />
            <el-option label="英语" :value="3" />
            <el-option label="物理" :value="4" />
            <el-option label="化学" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="教育阶段">
          <el-select v-model="addForm.educationStage" placeholder="请选择教育阶段">
            <el-option label="小学" :value="1" />
            <el-option label="初中" :value="2" />
            <el-option label="高中" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="addForm.difficultyLevel" placeholder="请选择难度">
            <el-option label="简单" :value="1" />
            <el-option label="中等" :value="2" />
            <el-option label="困难" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程描述">
          <el-input v-model="addForm.description" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- 编辑课程对话框 -->
    <el-dialog v-model="showEditDialog" title="编辑课程" width="600px">
      <el-form :model="editForm" :rules="editRules" ref="editFormRef" label-width="100px">
        <el-form-item label="课程名称" prop="courseName">
          <el-input v-model="editForm.courseName" placeholder="请输入课程名称" />
        </el-form-item>
        <el-form-item label="课程封面">
          <el-upload
            class="cover-uploader"
            action="/api/upload/image"
            :show-file-list="false"
            :on-success="handleEditCoverSuccess"
          >
            <img v-if="editForm.coverImage" :src="getImageUrl(editForm.coverImage)" class="cover-preview" />
            <el-icon v-else class="cover-uploader-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="学科">
          <el-select v-model="editForm.subjectId" placeholder="请选择学科">
            <el-option label="语文" :value="1" />
            <el-option label="数学" :value="2" />
            <el-option label="英语" :value="3" />
            <el-option label="物理" :value="4" />
            <el-option label="化学" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="教育阶段">
          <el-select v-model="editForm.educationStage" placeholder="请选择教育阶段">
            <el-option label="小学" :value="1" />
            <el-option label="初中" :value="2" />
            <el-option label="高中" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="editForm.difficultyLevel" placeholder="请选择难度">
            <el-option label="简单" :value="1" />
            <el-option label="中等" :value="2" />
            <el-option label="困难" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程描述">
          <el-input v-model="editForm.description" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import {
  getTeacherCourses,
  createCourse,
  deleteCourse,
  publishCourse,
  updateCourse
} from '@/api/course'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

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
const showAddDialog = ref(false)
const addFormRef = ref<FormInstance>()

const addForm = reactive({
  courseName: '',
  coverImage: '',
  subjectId: undefined as number | undefined,
  educationStage: undefined as number | undefined,
  difficultyLevel: 1,
  description: ''
})

const addRules: FormRules = {
  courseName: [{ required: true, message: '请输入课程名称', trigger: 'blur' }]
}

// ==================== 编辑课程（恢复）====================
const showEditDialog = ref(false)
const editFormRef = ref<FormInstance>()
const editCourseId = ref<number | null>(null)

const editForm = reactive({
  courseName: '',
  coverImage: '',
  subjectId: undefined as number | undefined,
  educationStage: undefined as number | undefined,
  difficultyLevel: 1,
  description: ''
})

const editRules: FormRules = {
  courseName: [{ required: true, message: '请输入课程名称', trigger: 'blur' }]
}

const getStatusType = (status: number) => {
  const types: Record<number, string> = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
  return types[status] || 'info'
}

const getStatusName = (status: number) => {
  const names: Record<number, string> = { 0: '草稿', 1: '审核中', 2: '已发布', 3: '已下架' }
  return names[status] || '未知'
}

// ==================== 统一解析后端响应 ====================
const parseRes = (res: any): any => {
  if (!res) return null
  if (res.data && typeof res.data.code === 'number' && res.data.code === 200) {
    return res.data.data !== null && res.data.data !== undefined ? res.data.data : {}
  }
  if (typeof res.code === 'number' && res.code === 200) {
    return res.data !== null && res.data !== undefined ? res.data : {}
  }
  if (Array.isArray(res) || (typeof res === 'object' && res !== null && !res.code)) return res
  return null
}

const isSuccess = (res: any): boolean => {
  if (!res) return false
  if (res.data && res.data.code === 200) return true
  if (typeof res.code === 'number' && res.code === 200) return true
  return false
}

const fetchCourses = async () => {
  loading.value = true
  try {
    const res = await getTeacherCourses({ current: 1, size: 20 }) as any
    console.log('[fetchCourses] 原始响应:', res)

    const data = parseRes(res)
    console.log('[fetchCourses] 解析结果:', data)

    let courses: any[] = []
    if (data?.records && Array.isArray(data.records)) {
      courses = data.records
    } else if (Array.isArray(data)) {
      courses = data
    }

    courseList.value = courses.map((c: any) => ({
      ...c,
      courseName: c.courseName || c.course_name || '未命名课程',
      coverImage: c.coverImage || c.cover_image || '/default-course.png',
      status: typeof c.status === 'number' ? c.status : 0,
      subjectName: c.subjectName || '',
      viewCount: c.viewCount || c.view_count || 0,
      statusName: c.statusName || getStatusName(c.status)
    }))

    console.log('[fetchCourses] 最终课程列表:', courseList.value)
  } catch (error) {
    console.error('[fetchCourses] 获取课程列表失败:', error)
    courseList.value = []
  } finally {
    loading.value = false
  }
}

const handleCoverSuccess = (res: any) => {
  const data = parseRes(res)
  if (data) addForm.coverImage = typeof data === 'string' ? data : (data.url || '')
}

const handleCreate = async () => {
  const valid = await addFormRef.value?.validate().catch(() => false)
  if (!valid) return

  try {
    const res = await createCourse({ ...addForm, status: 0, isPublic: 0 })
    if (isSuccess(res)) {
      ElMessage.success('创建成功，等待管理员审核')
      showAddDialog.value = false
      Object.assign(addForm, { courseName: '', coverImage: '', subjectId: undefined, educationStage: undefined, difficultyLevel: 1, description: '' })
      fetchCourses()
    } else {
      ElMessage.error('创建失败')
    }
  } catch (error: any) {
    console.error('[handleCreate] 创建失败:', error)
    ElMessage.error(error?.message || '创建失败')
  }
}

// ==================== 编辑课程方法 ====================
const handleEdit = (course: any) => {
  editCourseId.value = course.id
  editForm.courseName = course.courseName || ''
  editForm.coverImage = course.coverImage || ''
  editForm.subjectId = course.subjectId || course.subject_id
  editForm.educationStage = course.educationStage || course.education_stage
  editForm.difficultyLevel = course.difficultyLevel || course.difficulty_level || 1
  editForm.description = course.description || ''
  showEditDialog.value = true
}

const handleEditCoverSuccess = (res: any) => {
  const data = parseRes(res)
  if (data) editForm.coverImage = typeof data === 'string' ? data : (data.url || '')
}

const submitEdit = async () => {
  const valid = await editFormRef.value?.validate().catch(() => false)
  if (!valid || !editCourseId.value) return

  try {
    const res = await updateCourse(editCourseId.value, { ...editForm })
    if (isSuccess(res)) {
      ElMessage.success('更新成功')
      showEditDialog.value = false
      fetchCourses()
    } else {
      ElMessage.error('更新失败')
    }
  } catch (error: any) {
    console.error('[submitEdit] 更新失败:', error)
    ElMessage.error(error?.message || '更新失败')
  }
}

const viewCourse = (id: number) => {
  router.push(`/course/${id}`)
}

const handlePublish = async (id: number) => {
  try {
    const res = await publishCourse(id)
    if (isSuccess(res)) {
      ElMessage.success('已提交审核')
      fetchCourses()
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '发布失败')
  }
}

const handleDelete = (id: number) => {
  ElMessageBox.confirm('确定要删除这个课程吗？', '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    .then(async () => {
      try {
        const res = await deleteCourse(id)
        if (isSuccess(res)) {
          ElMessage.success('删除成功')
          fetchCourses()
        }
      } catch (error: any) {
        ElMessage.error(error?.message || '删除失败')
      }
    }).catch(() => {})
}

onMounted(() => {
  fetchCourses()
})
</script>

<style scoped>
.teacher-courses-container { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; }
.course-item { margin-bottom: 20px; }
.card-inner { display: flex; flex-direction: column; height: 300px; padding: 12px; box-sizing: border-box; }
.course-cover { width: 100%; height: 130px; object-fit: cover; border-radius: 4px; flex-shrink: 0; }
.course-info { flex: 1; padding-top: 8px; overflow: hidden; }
.course-name { margin: 0 0 6px 0; font-size: 14px; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.course-meta { display: flex; justify-content: space-between; margin-top: 6px; font-size: 12px; color: #909399; }
.course-actions { display: flex; flex-wrap: wrap; gap: 6px; padding-top: 8px; border-top: 1px solid #ebeef5; flex-shrink: 0; }
.cover-uploader { border: 1px dashed #d9d9d9; border-radius: 6px; cursor: pointer; overflow: hidden; width: 200px; height: 120px; }
.cover-uploader:hover { border-color: #409EFF; }
.cover-uploader-icon { font-size: 28px; color: #8c939d; width: 200px; height: 120px; text-align: center; line-height: 120px; }
.cover-preview { width: 200px; height: 120px; object-fit: cover; }
</style>