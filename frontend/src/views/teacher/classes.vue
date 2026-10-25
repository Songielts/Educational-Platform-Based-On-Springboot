<template>
  <div class="teacher-classes-container">
    <div class="page-header">
      <h2>班级管理</h2>
      <el-button type="primary" :icon="Plus" @click="showAddDialog = true">创建班级</el-button>
    </div>
    
    <el-empty description="暂无班级" v-if="!loading && (!classList || classList.length === 0)" />
    
    <el-row :gutter="20" v-loading="loading" v-else>
      <el-col :span="8" v-for="cls in classList" :key="cls.id" class="class-item">
        <el-card shadow="hover">
          <div class="class-header">
            <h4>{{ cls.className }}</h4>
            <el-tag :type="cls.status === 1 ? 'success' : 'info'">
              {{ cls.status === 1 ? '开放' : '关闭' }}
            </el-tag>
          </div>
          <p class="class-code">班级编码: {{ cls.classCode }}</p>
          <p class="class-desc">{{ cls.description || '暂无描述' }}</p>
          <div class="class-stats">
            <span>学生数: {{ cls.currentStudents }}/{{ cls.maxStudents }}</span>
          </div>
          <div class="class-actions">
            <el-button type="primary" link @click="generateJoinCode(cls.id)">生成邀请码</el-button>
            <el-button type="primary" link @click="manageStudents(cls.id)">管理学生</el-button>
            <el-button type="danger" link @click="deleteClass(cls.id)">删除</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 创建班级对话框 -->
    <el-dialog v-model="showAddDialog" title="创建班级" width="500px">
      <el-form :model="addForm" :rules="addRules" ref="addFormRef">
        <el-form-item label="班级编码" prop="classCode">
          <el-input v-model="addForm.classCode" placeholder="请输入班级编码" />
        </el-form-item>
        <el-form-item label="班级名称" prop="className">
          <el-input v-model="addForm.className" placeholder="请输入班级名称" />
        </el-form-item>
        <el-form-item label="班级描述">
          <el-input v-model="addForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="最大学生数" prop="maxStudents">
          <el-input-number v-model="addForm.maxStudents" :min="1" :max="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="createClass">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { getTeacherClasses, createClass as apiCreateClass, deleteClass as apiDeleteClass, generateJoinCode as apiGenerateJoinCode } from '@/api/class'
import { ElMessage, ElMessageBox } from 'element-plus'
import router from '@/router'

const loading = ref(false)
const classList = ref<any[]>([])
const showAddDialog = ref(false)
const addFormRef = ref()

const addForm = reactive({
  classCode: '',
  className: '',
  description: '',
  maxStudents: 50
})

const addRules = {
  classCode: [{ required: true, message: '请输入班级编码', trigger: 'blur' }],
  className: [{ required: true, message: '请输入班级名称', trigger: 'blur' }],
  maxStudents: [{ required: true, message: '请输入最大学生数', trigger: 'blur' }]
}

const fetchClasses = async () => {
  loading.value = true
  classList.value = []
  
  try {
    console.log('开始获取班级列表...')
    const res = await getTeacherClasses({ current: 1, size: 20 })
    console.log('完整响应:', res)
    
    // 检查 res 是否存在
    if (!res) {
      console.error('响应为空')
      ElMessage.error('获取班级列表失败')
      return
    }
    
    // 提取数据（适配多种格式）
    let responseData = res.data || res
    console.log('提取的响应数据:', responseData)
    
    if (responseData.code === 200) {
      // 获取 records
      let records = []
      if (responseData.data && Array.isArray(responseData.data.records)) {
        records = responseData.data.records
      } else if (Array.isArray(responseData.records)) {
        records = responseData.records
      } else if (Array.isArray(responseData.data)) {
        records = responseData.data
      }
      
      console.log('班级记录:', records)
      classList.value = records
    } else {
      console.error('返回错误:', responseData.message)
      ElMessage.error(responseData.message || '获取班级列表失败')
    }
  } catch (error: any) {
    console.error('获取班级列表异常:', error)
    console.error('错误响应:', error.response?.data)
    ElMessage.error('获取班级列表失败')
  } finally {
    loading.value = false
  }
}

const createClass = async () => {
  const valid = await addFormRef.value?.validate().catch(() => false)
  if (!valid) return
  
  try {
    await apiCreateClass(addForm)
    ElMessage.success('创建成功')
    showAddDialog.value = false
    fetchClasses()
    // 重置表单
    addForm.classCode = ''
    addForm.className = ''
    addForm.description = ''
    addForm.maxStudents = 50
  } catch (error) {
    console.error('创建失败:', error)
  }
}

const generateJoinCode = async (classId: number) => {
  try {
    const res = await apiGenerateJoinCode(classId, 24)
    console.log('生成邀请码响应:', res)
    
    // 适配后端返回格式
    const responseData = res.data || res
    
    if (responseData.code === 200) {
      // 邀请码在 data 字段中
      const joinCode = responseData.data
      
      ElMessageBox.alert(`班级邀请码: ${joinCode}`, '邀请码', {
        confirmButtonText: '复制',
        callback: () => {
          navigator.clipboard.writeText(joinCode)
          ElMessage.success('已复制到剪贴板')
        }
      })
    } else {
      ElMessage.error(responseData.message || '生成邀请码失败')
    }
  } catch (error) {
    console.error('生成邀请码失败:', error)
    ElMessage.error('生成邀请码失败')
  }
}

const manageStudents = (classId: number) => {
  router.push(`/teacher/class/${classId}/students`)
}

const deleteClass = (id: number) => {
  ElMessageBox.confirm('确定要删除这个班级吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await apiDeleteClass(id)
      ElMessage.success('删除成功')
      fetchClasses()
    } catch (error) {
      console.error('删除失败:', error)
    }
  })
}

onMounted(() => {
  fetchClasses()
})
</script>

<style scoped>
.teacher-classes-container {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
}

.class-item {
  margin-bottom: 20px;
}

.class-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.class-header h4 {
  margin: 0;
}

.class-code {
  color: #909399;
  font-size: 13px;
  margin-bottom: 10px;
}

.class-desc {
  color: #606266;
  margin-bottom: 10px;
  min-height: 40px;
}

.class-stats {
  margin-bottom: 15px;
  color: #409EFF;
}

.class-actions {
  display: flex;
  gap: 10px;
}
</style>