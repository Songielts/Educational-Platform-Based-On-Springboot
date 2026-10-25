<template>
  <div class="my-classes-container">
    <h2>我的班级</h2>
    
    <el-empty description="暂无加入的班级" v-if="!loading && classList.length === 0">
      <el-button type="primary" @click="showJoinDialog = true">加入班级</el-button>
    </el-empty>
    
    <el-row :gutter="20" v-loading="loading" v-else>
      <el-col :span="6" v-for="item in classList" :key="item.id" class="class-item">
        <el-card shadow="hover">
          <div class="class-header">
            <div class="class-icon">
              <el-icon><School /></el-icon>
            </div>
            <h4 class="class-name">{{ item.className }}</h4>
            <p class="class-code">代码: {{ item.classCode }}</p>
          </div>
          <div class="class-info">
            <div class="class-stats">
              <span><el-icon><User /></el-icon> 教师: {{ item.teacherName || '未知' }}</span>
              <span><el-icon><UserFilled /></el-icon> {{ item.currentStudents || 0 }}/{{ item.maxStudents || 0 }} 人</span>
            </div>
            <div class="class-actions">
              <el-button type="danger" link @click="leaveClass(item.id)">退出班级</el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-dialog v-model="showJoinDialog" title="加入班级" width="400px">
      <el-form :model="joinForm" :rules="joinRules" ref="joinFormRef">
        <el-form-item label="加入码" prop="joinCode">
          <el-input v-model="joinForm.joinCode" placeholder="请输入班级加入码" maxlength="8" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showJoinDialog = false">取消</el-button>
        <el-button type="primary" @click="handleJoin" :loading="joining">确认加入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { School, User, UserFilled } from '@element-plus/icons-vue'
import { getStudentClasses, joinClass, leaveClass as apiLeaveClass } from '@/api/class'

const loading = ref(false)
const classList = ref<any[]>([])
const showJoinDialog = ref(false)
const joining = ref(false)
const joinFormRef = ref()

const joinForm = reactive({
  joinCode: ''
})

const joinRules = {
  joinCode: [
    { required: true, message: '请输入加入码', trigger: 'blur' },
    { min: 6, max: 8, message: '加入码长度为6-8位', trigger: 'blur' }
  ]
}

const fetchMyClasses = async () => {
  loading.value = true
  try {
    const res = await getStudentClasses()
    if (res.data.code === 200) {
      classList.value = res.data.data || []
    } else {
      ElMessage.warning(res.data.message || '获取班级列表失败')
    }
  } catch (error) {
    ElMessage.error('获取我的班级失败')
  } finally {
    loading.value = false
  }
}

const handleJoin = async () => {
  const valid = await joinFormRef.value?.validate().catch(() => false)
  if (!valid) return
  
  joining.value = true
  try {
    const res = await joinClass(joinForm.joinCode)
    if (res.data.code === 200) {
      ElMessage.success('加入班级成功')
      showJoinDialog.value = false
      joinForm.joinCode = ''
      fetchMyClasses()
    } else {
      ElMessage.warning(res.data.message || '加入班级失败')
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '加入班级失败')
  } finally {
    joining.value = false
  }
}

const leaveClass = (classId: number) => {
  ElMessageBox.confirm('确定要退出这个班级吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await apiLeaveClass(classId)
      if (res.data.code === 200) {
        ElMessage.success('退出班级成功')
        fetchMyClasses()
      }
    } catch (error) {
      ElMessage.error('退出班级失败')
    }
  })
}

onMounted(() => {
  fetchMyClasses()
})
</script>

<style scoped>
.my-classes-container {
  padding: 20px;
}

.my-classes-container h2 {
  margin-bottom: 20px;
}

.class-item {
  margin-bottom: 20px;
}

.class-header {
  text-align: center;
  padding: 20px 0;
}

.class-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 15px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.class-icon .el-icon {
  font-size: 40px;
  color: #fff;
}

.class-name {
  margin: 0 0 8px 0;
  font-size: 16px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.class-code {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

.class-info {
  padding: 15px 0 0 0;
  border-top: 1px solid #ebeef5;
}

.class-stats {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 15px;
  font-size: 13px;
  color: #606266;
}

.class-stats span {
  display: flex;
  align-items: center;
  gap: 5px;
}

.class-stats .el-icon {
  color: #909399;
}

.class-actions {
  display: flex;
  justify-content: center;
}
</style>