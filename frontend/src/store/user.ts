// src/store/user.ts
import { defineStore } from 'pinia'
import { ref, computed, type Ref, type ComputedRef } from 'vue'
import { login, getUserInfo as apiGetUserInfo } from '@/api/auth'

interface UserInfo {
  id?: number
  username?: string
  realName?: string
  email?: string
  avatar?: string
  phone?: string
  gender?: number
  userType?: number
  userTypeName?: string
  status?: number
  roles?: string[]
  permissions?: string[]
  createTime?: string
}

export const useUserStore = defineStore('user', () => {
  // State
  const token: Ref<string> = ref<string>(localStorage.getItem('token') || '')
  const savedUserInfo = localStorage.getItem('userInfo')
  const userInfo: Ref<UserInfo | null> = ref<UserInfo | null>(
    savedUserInfo ? JSON.parse(savedUserInfo) : null
  )

  // Getters
  const isLoggedIn: ComputedRef<boolean> = computed(() => !!token.value && !!userInfo.value)
  const isStudent: ComputedRef<boolean> = computed(() => userInfo.value?.userType === 1)
  const isTeacher: ComputedRef<boolean> = computed(() => userInfo.value?.userType === 2)
  const isAdmin: ComputedRef<boolean> = computed(() => userInfo.value?.userType === 3)

  // Actions
  const setToken = (newToken: string) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

    const setUserInfo = (info: UserInfo | any) => {
    userInfo.value = info
    localStorage.setItem('userInfo', JSON.stringify(info))
    // 根据 userType 同步 userRole
    const roleMap: Record<number, string> = { 1: 'STUDENT', 2: 'TEACHER', 3: 'ADMIN' }
    const role = roleMap[info?.userType]
     if (role) {
      localStorage.setItem('userRole', role)
     }
  }


  // 新增：从服务器刷新用户信息
  const fetchUserInfo = async () => {
    try {
      const res = await apiGetUserInfo()
      const responseData = res.data
      if (responseData.code === 200 && responseData.data) {
        setUserInfo(responseData.data)
        return responseData.data
      }
      return null
    } catch (error) {
      console.error('获取用户信息失败:', error)
      return null
    }
  }

  const clearToken = () => {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  const loginAction = async (loginData: { username: string; password: string; captcha?: string; captchaKey?: string }) => {
    try {
      const res = await login(loginData)
      const responseData = res.data
      if (responseData.code === 200) {
        const data = responseData.data
        const tokenValue = data.accessToken
        if (tokenValue) {
          setToken(tokenValue)
          if (data.userInfo) {
            setUserInfo(data.userInfo)
          }
          return true
        }
      }
      return false
    } catch (error) {
      console.error('登录失败:', error)
      return false
    }
  }

  const checkLogin = async () => {
    if (userInfo.value) return true
    if (token.value) {
      try {
        const res = await apiGetUserInfo()
        const responseData = res.data
        if (responseData.code === 200) {
          setUserInfo(responseData.data)
          return true
        } else {
          clearToken()
          return false
        }
      } catch {
        clearToken()
        return false
      }
    }
    return false
  }

  const logout = () => {
    clearToken()
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    isStudent,
    isTeacher,
    isAdmin,
    setToken,
    setUserInfo,
    fetchUserInfo,
    clearToken,
    loginAction,
    checkLogin,
    logout
  }
})