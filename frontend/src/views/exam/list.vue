<template>
  <div class="exam-list-container">
    <h2>考试中心</h2>
    
    <el-empty description="暂无考试" v-if="!loading && examList.length === 0" />
    
    <div class="exam-list" v-loading="loading" v-else>
      <el-card v-for="exam in examList" :key="exam.id" class="exam-card" shadow="hover">
        <div class="exam-header">
          <h3>{{ exam.examName }}</h3>
          <el-tag :type="getStatusType(exam.status)">{{ exam.statusName }}</el-tag>
        </div>
        <div class="exam-info">
          <p><el-icon><User /></el-icon> 出卷人: {{ exam.teacherName }}</p>
          <p><el-icon><Timer /></el-icon> 时长: {{ exam.timeLimit }}分钟</p>
          <p><el-icon><Document /></el-icon> 题数: {{ exam.questionCount }}题</p>
          <p><el-icon><Trophy /></el-icon> 总分: {{ exam.totalScore }}分</p>
        </div>
        <div class="exam-actions">
          <el-button type="primary" @click="startExam(exam.id)" v-if="exam.status === 1">开始考试</el-button>
          <el-button @click="viewDetail(exam.id)">查看详情</el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getStudentExamList } from '@/api/exam'
import { useRouter } from 'vue-router'
import { User, Timer, Document, Trophy } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()

const loading = ref(false)
const examList = ref<any[]>([])

const getStatusType = (status: number) => {
  const types: Record<number, string> = {
    0: 'info',
    1: 'success',
    2: 'danger'
  }
  return types[status] || 'info'
}

const fetchExams = async () => {
  loading.value = true
  try {
    const res = await getStudentExamList({ current: 1, size: 20 })
    if (res.data.code === 200) {
      examList.value = res.data.data.records || []
    } else {
      ElMessage.error(res.data.message || '获取考试列表失败')
    }
  } catch (error) {
    console.error('获取考试列表失败:', error)
    ElMessage.error('获取考试列表失败')
  } finally {
    loading.value = false
  }
}


const startExam = (id: number) => {
  router.push(`/take-exam/${id}`)
}

const viewDetail = (id: number) => {
  router.push(`/exam/${id}`)
}

onMounted(() => {
  fetchExams()
})
</script>

<style scoped>
.exam-list-container {
  padding: 20px;
}

.exam-list-container h2 {
  margin-bottom: 20px;
}

.exam-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.exam-card {
  cursor: pointer;
}

.exam-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.exam-header h3 {
  margin: 0;
}

.exam-info {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  margin-bottom: 15px;
}

.exam-info p {
  margin: 0;
  color: #606266;
  display: flex;
  align-items: center;
  gap: 5px;
}

.exam-actions {
  display: flex;
  gap: 10px;
}
</style>