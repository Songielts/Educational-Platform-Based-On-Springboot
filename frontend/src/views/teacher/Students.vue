<template>
  <div class="class-students-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <el-button link @click="router.back()">
        <el-icon><ArrowLeft /></el-icon>返回
      </el-button>
      <h2>班级学生管理</h2>
      <el-tag v-if="classInfo" type="success">{{ classInfo.className }}</el-tag>
    </div>
    
    <!-- 学生列表 -->
    <el-card>
      <template #header>
        <div class="card-header">
          <span>学生列表 ({{ studentList.length }}/{{ classInfo?.maxStudents || 50 }})</span>
          <el-button type="primary" @click="showAddDialog = true">
            <el-icon><Plus /></el-icon>添加学生
          </el-button>
        </div>
      </template>
      
      <!-- 空状态 -->
      <el-empty v-if="studentList.length === 0" description="暂无学生，点击上方按钮添加" />
      
      <!-- 学生表格 -->
      <el-table v-else :data="studentList" stripe>
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="realName" label="学生姓名" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="joinTime" label="加入时间">
          <template #default="{ row }">
            {{ formatDate(row.joinTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button type="danger" link @click="removeStudent(row.studentId)">
              <el-icon><Delete /></el-icon>移除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    
    <!-- 添加学生对话框 -->
    <el-dialog v-model="showAddDialog" title="添加学生" width="500px">
      <el-form :model="addForm" label-width="80px">
        <el-form-item label="选择学生">
          <el-select
            v-model="addForm.studentId"
            filterable
            remote
            clearable
            placeholder="请输入学生用户名搜索"
            :remote-method="searchStudents"
            :loading="searchLoading"
            style="width: 100%"
          >
            <el-option
              v-for="student in studentOptions"
              :key="student.id"
              :label="`${student.realName || student.username} (${student.username})`"
              :value="student.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="addStudent" :loading="addLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Plus, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getClassDetail, getClassStudents, searchStudents as searchStudentsApi, addStudentToClass, removeStudentFromClass } from '@/api/class'

const route = useRoute()
const router = useRouter()
const classId = Number(route.params.classId)

const classInfo = ref<any>(null)
const studentList = ref<any[]>([])
const showAddDialog = ref(false)
const searchLoading = ref(false)
const addLoading = ref(false)
const studentOptions = ref<any[]>([])

const addForm = ref({
  studentId: null as number | null
})

// 获取班级信息
const fetchClassInfo = async () => {
  try {
    const res = await getClassDetail(classId)
    if (res.data.code === 200) {
      classInfo.value = res.data.data
    }
  } catch (error) {
    console.error('获取班级信息失败:', error)
    ElMessage.error('获取班级信息失败')
  }
}

// 获取学生列表
const fetchStudents = async () => {
  try {
    const res = await getClassStudents(classId)
    if (res.data.code === 200) {
      studentList.value = res.data.data || []
    }
  } catch (error) {
    console.error('获取学生列表失败:', error)
    ElMessage.error('获取学生列表失败')
  }
}

// 搜索学生
const searchStudents = async (query: string) => {
  if (!query || query.length < 1) {
    studentOptions.value = []
    return
  }
  
  searchLoading.value = true
  try {
    const res = await searchStudentsApi(query)
    if (res.data.code === 200) {
      // 过滤掉已经在班级中的学生
      const existingIds = studentList.value.map(s => s.studentId)
      studentOptions.value = (res.data.data || []).filter(
        (s: any) => !existingIds.includes(s.id)
      )
    }
  } catch (error) {
    console.error('搜索学生失败:', error)
  } finally {
    searchLoading.value = false
  }
}

// 添加学生
const addStudent = async () => {
  if (!addForm.value.studentId) {
    ElMessage.warning('请选择学生')
    return
  }
  
  addLoading.value = true
  try {
    const res = await addStudentToClass(classId, addForm.value.studentId)
    if (res.data.code === 200) {
      ElMessage.success('添加成功')
      showAddDialog.value = false
      addForm.value.studentId = null
      studentOptions.value = []
      fetchStudents()
    } else {
      ElMessage.error(res.data.message || '添加失败')
    }
  } catch (error: any) {
    console.error('添加学生失败:', error)
    ElMessage.error(error.response?.data?.message || '添加失败')
  } finally {
    addLoading.value = false
  }
}

// 移除学生
const removeStudent = (studentId: number) => {
  ElMessageBox.confirm('确定要移除该学生吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await removeStudentFromClass(classId, studentId)
      if (res.data.code === 200) {
        ElMessage.success('移除成功')
        fetchStudents()
      } else {
        ElMessage.error(res.data.message || '移除失败')
      }
    } catch (error: any) {
      console.error('移除学生失败:', error)
      ElMessage.error(error.response?.data?.message || '移除失败')
    }
  })
}

// 格式化日期
const formatDate = (date: string) => {
  if (!date) return '-'
  return new Date(date).toLocaleString()
}

onMounted(() => {
  fetchClassInfo()
  fetchStudents()
})
</script>

<style scoped>
.class-students-container {
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>