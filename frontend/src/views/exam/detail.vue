<template>
  <div class="exam-detail-container" v-loading="loading">
    <el-card v-if="exam">
      <template #header>
        <div class="exam-header">
          <h2>{{ exam.examName }}</h2>
          <el-tag :type="getStatusType(exam.status)">{{ exam.statusName }}</el-tag>
        </div>
      </template>
      
      <div class="exam-info">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="出卷人">{{ exam.teacherName }}</el-descriptions-item>
          <el-descriptions-item label="考试类型">{{ exam.examTypeName }}</el-descriptions-item>
          <el-descriptions-item label="考试时长">{{ exam.timeLimit }}分钟</el-descriptions-item>
          <el-descriptions-item label="题目数量">{{ exam.questionCount }}题</el-descriptions-item>
          <el-descriptions-item label="总分">{{ exam.totalScore }}分</el-descriptions-item>
          <el-descriptions-item label="及格分">{{ exam.passScore }}分</el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ exam.startTime }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">{{ exam.endTime }}</el-descriptions-item>
        </el-descriptions>
      </div>
      
      <div class="exam-actions">
        <el-button type="primary" size="large" @click="startExam" v-if="exam.status === 1">开始考试</el-button>
        <el-button size="large" @click="$router.push('/exams')">返回列表</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getExamDetail } from '@/api/exam'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const examId = Number(route.params.id)

const loading = ref(false)
const exam = ref<any>(null)

const getStatusType = (status: number) => {
  const types: Record<number, string> = {
    0: 'info',
    1: 'success',
    2: 'danger'
  }
  return types[status] || 'info'
}

const fetchExamDetail = async () => {
  loading.value = true
  try {
    const res = await getExamDetail(examId)
    // 修复：code 在 res.data 中，而不是 res 上
    if (res.data.code === 200) {
      exam.value = res.data.data
    }
  } catch (error) {
    console.error('获取考试详情失败:', error)
    ElMessage.error('获取考试详情失败')
  } finally {
    loading.value = false
  }
}

const startExam = () => {
  router.push(`/take-exam/${examId}`)
}

onMounted(() => {
  fetchExamDetail()
})
</script>

<style scoped>
.exam-detail-container {
  padding: 20px;
}

.exam-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.exam-header h2 {
  margin: 0;
}

.exam-info {
  margin: 20px 0;
}

.exam-actions {
  display: flex;
  gap: 15px;
  justify-content: center;
  margin-top: 30px;
}
</style>
