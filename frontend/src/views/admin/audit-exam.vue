<template>
  <div class="admin-exams-container">
    <div class="page-header">
      <h2>考试审核</h2>
      <el-input
        v-model="searchKeyword"
        placeholder="搜索考试名称"
        style="width: 250px"
        clearable
        @keyup.enter="handleSearch"
      >
        <template #append>
          <el-button :icon="Search" @click="handleSearch" />
        </template>
      </el-input>
    </div>

    <el-table :data="examList" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="examName" label="考试名称" min-width="150" show-overflow-tooltip />
      <el-table-column prop="teacherName" label="教师" width="100" />
      <el-table-column prop="examTypeName" label="类型" width="80" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">{{ row.statusName }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="questionCount" label="题目数" width="80" />
      <el-table-column prop="createTime" label="创建时间" width="160" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="viewDetail(row)">查看</el-button>
          <el-button type="success" link size="small" @click="handleApprove(row.id)" v-if="row.status === 10">通过</el-button>
          <el-button type="danger" link size="small" @click="handleReject(row.id)" v-if="row.status === 10">拒绝</el-button>
          <el-button type="danger" link size="small" @click="handleDelete(row.id)">删除</el-button>
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
        @current-change="fetchExams"
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

    <!-- 考试详情对话框 -->
    <el-dialog v-model="detailVisible" title="考试详情" width="600px">
      <el-descriptions :column="1" border v-if="currentExam">
        <el-descriptions-item label="考试名称">{{ currentExam.examName }}</el-descriptions-item>
        <el-descriptions-item label="试卷类型">{{ currentExam.examTypeName }}</el-descriptions-item>
        <el-descriptions-item label="教师">{{ currentExam.teacherName }}</el-descriptions-item>
        <el-descriptions-item label="题目数量">{{ currentExam.questionCount }}</el-descriptions-item>
        <el-descriptions-item label="总分">{{ currentExam.totalScore }}</el-descriptions-item>
        <el-descriptions-item label="及格分">{{ currentExam.passScore }}</el-descriptions-item>
        <el-descriptions-item label="时间限制">{{ currentExam.timeLimit }}分钟</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentExam.status)">{{ currentExam.statusName }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="success" @click="handleApprove(currentExam?.id)" v-if="currentExam?.status === 10">通过</el-button>
        <el-button type="danger" @click="handleReject(currentExam?.id)" v-if="currentExam?.status === 10">拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPendingExams, auditExam as auditExamApi, adminDeleteExam as adminDeleteExamApi } from '@/api/exam'

const loading = ref(false)
const examList = ref<any[]>([])
const searchKeyword = ref('')
const detailVisible = ref(false)
const rejectVisible = ref(false)
const currentExam = ref<any>(null)
const rejectReason = ref('')
const rejectId = ref<number | string>('')

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const getStatusType = (status: number) => {
  const types: Record<number, string> = { 0: 'info', 10: 'warning', 1: 'success', 2: 'danger' }
  return types[status] || 'info'
}

// ==================== 关键修复：循环解包，兼容 AxiosResponse / Result / PageResult 多层嵌套 ====================
const fetchExams = async () => {
  loading.value = true
  try {
    const res: any = await getPendingExams({
      keyword: searchKeyword.value || undefined,
      current: pagination.current,
      size: pagination.size
    })

    console.log('[AdminExams] 原始响应:', res)

    let pageData: any = res
    while (pageData && typeof pageData === 'object' && !Array.isArray(pageData)) {
      if (pageData.records !== undefined) break
      if (pageData.data && typeof pageData.data === 'object') {
        pageData = pageData.data
      } else {
        break
      }
    }

    console.log('[AdminExams] 解包分页数据:', pageData)

    if (pageData) {
      if (pageData.records && Array.isArray(pageData.records)) {
        examList.value = pageData.records
        pagination.total = pageData.total || 0
      } else if (Array.isArray(pageData)) {
        examList.value = pageData
        pagination.total = pageData.length
      } else {
        examList.value = []
        pagination.total = 0
      }
    } else {
      examList.value = []
      pagination.total = 0
    }

    console.log('[AdminExams] 考试列表:', examList.value)
  } catch (error: any) {
    console.error('[AdminExams] 获取数据失败:', error)
    ElMessage.error(error?.message || '获取数据失败')
    examList.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  fetchExams()
}

const viewDetail = (row: any) => {
  currentExam.value = row
  detailVisible.value = true
}

const handleApprove = async (id?: number | string) => {
  if (!id) return
  try {
    await ElMessageBox.confirm('确定要通过该考试的审核吗？', '提示', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    })
    await auditExamApi(Number(id), { approved: true })
    ElMessage.success('审核通过')
    detailVisible.value = false
    fetchExams()
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
    await auditExamApi(Number(rejectId.value), { approved: false, reason: rejectReason.value })
    ElMessage.success('已拒绝')
    rejectVisible.value = false
    detailVisible.value = false
    fetchExams()
  } catch (error: any) {
    ElMessage.error(error?.message || '操作失败')
  }
}

const handleDelete = async (id?: number | string) => {
  if (!id) return
  try {
    await ElMessageBox.confirm('确定要删除这个考试吗？删除后不可恢复。', '提示', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    })
    await adminDeleteExamApi(Number(id))
    ElMessage.success('删除成功')
    fetchExams()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '删除失败')
    }
  }
}

onMounted(() => {
  fetchExams()
})
</script>

<style scoped>
.admin-exams-container { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; }
.pagination-section { display: flex; justify-content: center; margin-top: 20px; }
</style>