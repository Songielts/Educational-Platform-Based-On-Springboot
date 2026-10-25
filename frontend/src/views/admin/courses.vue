<template>
  <div class="admin-courses-container">
    <div class="page-header">
      <h2>课程审核</h2>
      <el-input
        v-model="searchKeyword"
        placeholder="搜索课程名称"
        style="width: 250px"
        clearable
        @keyup.enter="handleSearch"
      >
        <template #append>
          <el-button :icon="Search" @click="handleSearch" />
        </template>
      </el-input>
    </div>

    <el-table :data="courseList" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="courseName" label="课程名称" min-width="150" show-overflow-tooltip />
      <el-table-column prop="teacherName" label="教师" width="100" />
      <el-table-column prop="subjectName" label="学科" width="80" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">{{ row.statusName }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="160" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="viewDetail(row)">查看</el-button>
          <el-button type="success" link size="small" @click="handleApprove(row.id)" v-if="row.status === 0 || row.status === 1">通过</el-button>
          <el-button type="danger" link size="small" @click="handleReject(row.id)" v-if="row.status === 0 || row.status === 1">拒绝</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-section">
      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        layout="total, prev, pager, next"
        size="small"
        @current-change="fetchCourses"
      />
    </div>

    <!-- 拒绝原因对话框 -->
    <el-dialog v-model="rejectVisible" title="拒绝原因" width="400px">
      <el-input
        v-model="rejectReason"
        type="textarea"
        :rows="3"
        placeholder="请输入拒绝原因（可选）"
      />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReject">确认拒绝</el-button>
      </template>
    </el-dialog>

    <!-- 课程详情对话框 -->
    <el-dialog v-model="detailVisible" title="课程详情" width="600px">
      <el-descriptions :column="1" border v-if="currentCourse">
        <el-descriptions-item label="课程名称">{{ currentCourse.courseName }}</el-descriptions-item>
        <el-descriptions-item label="课程描述">{{ currentCourse.description || '暂无描述' }}</el-descriptions-item>
        <el-descriptions-item label="教师">{{ currentCourse.teacherName }}</el-descriptions-item>
        <el-descriptions-item label="学科">{{ currentCourse.subjectName }}</el-descriptions-item>
        <el-descriptions-item label="教育阶段">{{ currentCourse.educationStageName }}</el-descriptions-item>
        <el-descriptions-item label="难度">{{ currentCourse.difficultyLevelName }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentCourse.status)">{{ currentCourse.statusName }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="success" @click="handleApprove(currentCourse?.id)" v-if="currentCourse?.status === 0 || currentCourse?.status === 1">通过</el-button>
        <el-button type="danger" @click="handleReject(currentCourse?.id)" v-if="currentCourse?.status === 0 || currentCourse?.status === 1">拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { getPendingCourses, auditCourse } from '@/api/course'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()

const loading = ref(false)
const courseList = ref<any[]>([])
const searchKeyword = ref('')
const detailVisible = ref(false)
const rejectVisible = ref(false)
const currentCourse = ref<any>(null)
const rejectReason = ref('')
const rejectId = ref<number | string>('')

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const getStatusType = (status: number) => {
  const types: Record<number, string> = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
  return types[status] || 'info'
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
    const res = await getPendingCourses({
      keyword: searchKeyword.value || undefined,
      current: pagination.current,
      size: pagination.size
    }) as any

    console.log('[AdminCourses] 原始响应:', res)
    const data = parseRes(res)
    console.log('[AdminCourses] 解析结果:', data)

    if (data) {
      if (data.records && Array.isArray(data.records)) {
        courseList.value = data.records
        pagination.total = data.total || 0
      } else if (Array.isArray(data)) {
        courseList.value = data
        pagination.total = data.length
      } else {
        courseList.value = []
        pagination.total = 0
      }
    } else {
      courseList.value = []
      pagination.total = 0
    }

    console.log('[AdminCourses] 课程列表:', courseList.value)
  } catch (error: any) {
    console.error('[AdminCourses] 获取数据失败:', error)
    ElMessage.error(error?.message || '获取数据失败')
    courseList.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  fetchCourses()
}

const viewDetail = (row: any) => {
  currentCourse.value = row
  detailVisible.value = true
}

const handleApprove = async (id?: number | string) => {
  if (!id) return
  try {
    await ElMessageBox.confirm('确定要通过该课程的审核吗？', '提示', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    })
    const res = await auditCourse(id, { approved: true })
    if (isSuccess(res)) {
      ElMessage.success('审核通过')
      detailVisible.value = false
      fetchCourses()
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '审核失败')
    }
  }
}

const handleReject = (id?: number | string) => {
  if (!id) return
  rejectId.value = id
  rejectReason.value = ''
  rejectVisible.value = true
}

const confirmReject = async () => {
  try {
    const res = await auditCourse(rejectId.value, { approved: false, reason: rejectReason.value })
    if (isSuccess(res)) {
      ElMessage.success('已拒绝')
      rejectVisible.value = false
      detailVisible.value = false
      fetchCourses()
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '操作失败')
  }
}

onMounted(() => {
  fetchCourses()
})
</script>

<style scoped>
.admin-courses-container { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; }
.pagination-section { display: flex; justify-content: center; margin-top: 20px; }
</style>
