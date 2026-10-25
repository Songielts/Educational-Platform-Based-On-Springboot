<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside width="220px" class="sidebar">
      <div class="logo">
        <img src="/logo.png" alt="logo" v-if="false">
        <span>在线教育平台</span>
      </div>
      
      <el-menu
        :default-active="activeMenu"
        router
        class="sidebar-menu"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
        </el-menu-item>
        
        <el-menu-item index="/courses">
          <el-icon><Reading /></el-icon>
          <span>课程中心</span>
        </el-menu-item>
        
        <el-menu-item index="/my-courses" v-if="userStore.isStudent">
          <el-icon><Collection /></el-icon>
          <span>我的课程</span>
        </el-menu-item>
        <el-menu-item index="/my-classes" v-if="userStore.isStudent">
        <el-icon><UserFilled /></el-icon>
        <span>我的班级</span>
        </el-menu-item>
        <el-menu-item index="/notes" v-if="userStore.isStudent">
          <el-icon><Document /></el-icon>
          <span>学习笔记</span>
        </el-menu-item>
        
        <el-menu-item index="/exams">
          <el-icon><EditPen /></el-icon>
          <span>考试中心</span>
        </el-menu-item>
        
        <el-menu-item index="/wrong-questions" v-if="userStore.isStudent">
          <el-icon><CircleClose /></el-icon>
          <span>错题本</span>
        </el-menu-item>
        
        <el-menu-item index="/statistics" v-if="userStore.isStudent">
          <el-icon><TrendCharts /></el-icon>
          <span>学习统计</span>
        </el-menu-item>
        
        <!-- 教师菜单 -->
        <el-sub-menu index="/teacher" v-if="userStore.isTeacher">
          <template #title>
            <el-icon><UserFilled /></el-icon>
            <span>教师中心</span>
          </template>
          <el-menu-item index="/teacher/classes">班级管理</el-menu-item>
          <el-menu-item index="/teacher/courses">课程管理</el-menu-item>
          <el-menu-item index="/teacher/questions">题库管理</el-menu-item>
          <el-menu-item index="/teacher/exams">试卷管理</el-menu-item>
          <el-menu-item index="/teacher/ai-question"><el-icon><MagicStick /></el-icon><span>AI智能出题</span></el-menu-item>
        </el-sub-menu>
        
        <!-- 管理员菜单 -->
        <el-sub-menu index="/admin" v-if="userStore.isAdmin">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/admin/users">用户管理</el-menu-item>
          <el-menu-item index="/admin/courses">课程审核</el-menu-item>
          <el-menu-item index="/admin/exams">考试审核</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>
    
    <el-container>
      <!-- 顶部导航 -->
      <el-header class="header">
        <div class="header-left">
          <breadcrumb />
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <!-- 关键修复：使用 computed，确保响应式更新 -->
              <el-avatar :size="32" :src="avatarUrl">
                {{ displayName.charAt(0) || 'U' }}
              </el-avatar>
              <span class="username">{{ displayName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="settings">账号设置</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      
      <!-- 主内容区 -->
      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  HomeFilled,
  Reading,
  Collection,
  Document,
  EditPen,
  CircleClose,
  TrendCharts,
  UserFilled,
  Setting,
  ArrowDown,
  MagicStick
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import Breadcrumb from '@/components/Breadcrumb.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

// ==========================================
// 关键修复：用 computed 包裹，确保头像和名字响应式更新
// ==========================================
const avatarUrl = computed(() => {
  const avatar = (userStore.userInfo as any)?.avatar
  if (!avatar) return ''
  if (avatar.startsWith('http')) return avatar
  return `/api${avatar}`  // 添加 /api 前缀
})

const displayName = computed(() => {
  return (userStore.userInfo as any)?.value?.realName || ''
})

const handleCommand = (command: string) => {
  switch (command) {
    case 'profile':
      router.push('/profile-center')
      break
    case 'settings':
      router.push('/profile')
      break
    case 'logout':
      ElMessageBox.confirm('确定要退出登录吗?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        userStore.logout()
        ElMessage.success('已退出登录')
        router.push('/login')
      })
      break
  }
}
</script>

<style scoped>
.layout-container {
  min-height: 100vh;
}

.sidebar {
  background-color: #304156;
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  z-index: 100;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  border-bottom: 1px solid #1f2d3d;
}

.sidebar-menu {
  border-right: none;
}

.header {
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: fixed;
  top: 0;
  right: 0;
  left: 220px;
  z-index: 99;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 0 10px;
}

.username {
  margin: 0 8px;
  font-size: 14px;
  color: #606266;
}

.main-content {
  margin-left: 220px;
  margin-top: 60px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>