<template>
  <div class="profile-center-container">
    <h2>个人中心</h2>

    <el-row :gutter="20" class="profile-row">
      <el-col :span="8" class="profile-col">
        <el-card class="avatar-card" style="height: 100%;">
          <div class="avatar-section">
            <el-avatar :size="120" :src="avatarDisplayUrl" @error="avatarDisplayUrl = ''">
              {{ userInfo.realName?.charAt(0) || userInfo.username?.charAt(0) || 'U' }}
            </el-avatar>
            <div class="user-name">{{ userInfo.realName || userInfo.username }}</div>
            <el-tag :type="roleTagType" size="large">{{ userInfo.userTypeName }}</el-tag>
          </div>

          <el-divider />

          <div class="info-list">
            <div class="info-item">
              <span class="info-label">用户名</span>
              <span class="info-value">{{ userInfo.username || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">真实姓名</span>
              <span class="info-value">{{ userInfo.realName || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">性别</span>
              <span class="info-value">{{ genderText }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">邮箱</span>
              <span class="info-value">{{ userInfo.email || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">手机号</span>
              <span class="info-value">{{ userInfo.phone || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">注册时间</span>
              <span class="info-value">{{ formatDate(userInfo.createTime) }}</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="16" class="profile-col">
        <el-row :gutter="20" v-if="userStore.isStudent">
          <el-col :span="8" v-for="stat in statistics" :key="stat.title">
            <el-card class="stat-card" shadow="hover">
              <div class="stat-icon" :style="{ color: stat.color }">
                <el-icon :size="32"><component :is="stat.icon" /></el-icon>
              </div>
              <div class="stat-num" :style="{ color: stat.color }">{{ stat.value }}</div>
              <div class="stat-title">{{ stat.title }}</div>
            </el-card>
          </el-col>
        </el-row>

        <el-card class="detail-card" :class="{ 'no-margin': !userStore.isStudent }" style="height: 100%;">
          <template #header>
            <span>详细信息</span>
            <el-button type="primary" link @click="router.push('/profile')">
              <el-icon><Edit /></el-icon> 去修改
            </el-button>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="用户ID">{{ userInfo.id || '-' }}</el-descriptions-item>
            <el-descriptions-item label="账号状态">
              <el-tag :type="userInfo.status === 1 ? 'success' : 'danger'">
                {{ userInfo.status === 1 ? '正常' : '禁用' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="用户类型">{{ userInfo.userTypeName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ userInfo.email || '-' }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ userInfo.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="性别">{{ genderText }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { Edit, Reading, EditPen, Trophy } from '@element-plus/icons-vue'
import { getStudentExamRecords } from '@/api/exam'
import { getStudentCourses } from '@/api/course'
import { getNoteList } from '@/api/study'

const router = useRouter()
const userStore = useUserStore()

const userInfo = computed(() => (userStore.userInfo || {}) as any)
const genderText = computed(() => {
  const g = userInfo.value.gender
  return g === 1 ? '男' : g === 2 ? '女' : '保密'
})
const roleTagType = computed(() => {
  const type = userInfo.value.userType
  return type === 1 ? 'success' : type === 2 ? 'warning' : type === 3 ? 'danger' : 'info'
})
const formatDate = (date: string | undefined) => {
  return date ? new Date(date).toLocaleDateString('zh-CN') : '-'
}

// 头像处理
const avatarDisplayUrl = ref('')

const extractAvatarPath = (avatar: any): string => {
  if (!avatar) return ''
  if (typeof avatar === 'object') return avatar.value || avatar.url || avatar.path || ''
  return String(avatar)
}

const getAvatarDisplayUrl = (avatar: any): string => {
  const path = extractAvatarPath(avatar)
  if (!path) return ''
  if (path.startsWith('http')) return path
  const cleanPath = path.startsWith('/') ? path : `/${path}`
  return `/api${cleanPath}`
}

const loadAvatar = () => {
  avatarDisplayUrl.value = getAvatarDisplayUrl(userInfo.value.avatar)
}

// 统计数据
const statistics = ref([
  { title: '我的课程', value: 0, icon: 'Reading', color: '#409EFF' },
  { title: '参加考试', value: 0, icon: 'EditPen', color: '#67C23A' },
  { title: '学习笔记', value: 0, icon: 'Edit', color: '#E6A23C' },
  { title: '获得积分', value: 0, icon: 'Trophy', color: '#F56C6C' }
])

const parseRes = (res: any): any => {
  if (!res) return null
  if (res.data?.code === 200) return res.data.data
  if (res.code === 200) return res.data
  return null
}

const loadStatistics = async () => {
  if (!userStore.isStudent) return

  try {
    const data = parseRes(await getStudentCourses())
    if (data) statistics.value[0].value = Array.isArray(data) ? data.length : (data.total || 0)
  } catch (e) { /* ignore */ }

  try {
    const data = parseRes(await getStudentExamRecords({ current: 1, size: 1 }))
    if (data) statistics.value[1].value = data.total || 0
  } catch (e) { /* ignore */ }

  try {
    const data = parseRes(await getNoteList({ current: 1, size: 1 }))
    if (data) statistics.value[2].value = data.total || 0
  } catch (e) { /* ignore */ }
}

onMounted(() => {
  loadAvatar()
  loadStatistics()
})
</script>

<style scoped>
.profile-center-container { padding: 20px; }
.profile-center-container h2 { margin-bottom: 20px; }
.profile-row { display: flex; align-items: stretch; }
.profile-col { display: flex; flex-direction: column; }
.avatar-card { text-align: center; }
.avatar-section { padding: 20px 0; }
.user-name { font-size: 20px; font-weight: 600; margin: 12px 0 8px; color: #303133; }
.info-list { padding: 10px 0; }
.info-item { display: flex; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid #f0f0f0; }
.info-item:last-child { border-bottom: none; }
.info-label { color: #909399; font-size: 14px; }
.info-value { color: #606266; font-size: 14px; font-weight: 500; }
.stat-card { text-align: center; padding: 10px 0; }
.stat-icon { margin-bottom: 8px; }
.stat-num { font-size: 28px; font-weight: 700; margin-bottom: 4px; }
.stat-title { font-size: 13px; color: #909399; }
.detail-card { margin-top: 20px; }
.detail-card.no-margin { margin-top: 0; }
.detail-card :deep(.el-card__header) { display: flex; justify-content: space-between; align-items: center; }
</style>