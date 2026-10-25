<template>
  <div class="admin-users-container">
    <h2>用户管理</h2>
    
    <div class="filter-section">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索用户名/姓名"
        style="width: 300px"
        clearable
        @keyup.enter="fetchUsers"
      >
        <template #append>
          <el-button :icon="Search" @click="fetchUsers" />
        </template>
      </el-input>
      <el-select v-model="filterUserType" placeholder="用户类型" clearable @change="fetchUsers">
        <el-option label="学生" :value="1" />
        <el-option label="教师" :value="2" />
        <el-option label="管理员" :value="3" />
      </el-select>
    </div>
    
    <el-table :data="userList" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="realName" label="真实姓名" />
      <el-table-column prop="email" label="邮箱" />
      <el-table-column prop="userType" label="用户类型" width="100">
        <template #default="{ row }">
          <el-tag :type="getUserTypeType(row.userType)">{{ getUserTypeName(row.userType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button type="primary" link @click="toggleStatus(row)">
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
          <el-button type="danger" link @click="resetPassword(row.id)">重置密码</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <div class="pagination-section">
      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        layout="total, prev, pager, next"
        @current-change="fetchUsers"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserList, toggleUserStatus, resetUserPassword } from '@/api/user'

const loading = ref(false)
const userList = ref<any[]>([])
const searchKeyword = ref('')
const filterUserType = ref<number | undefined>(undefined)

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const getUserTypeType = (type: number) => {
  const types: Record<number, string> = {
    1: 'info',
    2: 'success',
    3: 'danger'
  }
  return types[type] || 'info'
}

const getUserTypeName = (type: number) => {
  const names: Record<number, string> = {
    1: '学生',
    2: '老师',
    3: '管理员'
  }
  return names[type] || '未知'
}

const fetchUsers = async () => {
  loading.value = true
  try {
    const res = await getUserList({
      keyword: searchKeyword.value,
      userType: filterUserType.value,
      current: pagination.current,
      size: pagination.size
    })
    if (res.data.code === 200) {
      userList.value = res.data.data.records || []
      pagination.total = res.data.data.total || 0
    } else {
      ElMessage.error(res.data.message || '获取用户列表失败')
    }
  } catch (error) {
    console.error('获取用户列表失败:', error)
    ElMessage.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

const toggleStatus = async (row: any) => {
  const action = row.status === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${action}该用户吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const res = await toggleUserStatus(row.id)
    if (res.data.code === 200) {
      ElMessage.success(`${action}成功`)
      fetchUsers()
    } else {
      ElMessage.error(res.data.message || '操作失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('切换用户状态失败:', error)
      ElMessage.error('操作失败')
    }
  }
}

const resetPassword = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要重置该用户的密码吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const res = await resetUserPassword(id)
    if (res.data.code === 200) {
      ElMessage.success(`密码已重置为: ${res.data.data}`)
    } else {
      ElMessage.error(res.data.message || '重置密码失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('重置密码失败:', error)
      ElMessage.error('重置密码失败')
    }
  }
}

onMounted(() => {
  fetchUsers()
})
</script>

<style scoped>
.admin-users-container {
  padding: 20px;
}

.admin-users-container h2 {
  margin-bottom: 20px;
}

.filter-section {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
}

.pagination-section {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>