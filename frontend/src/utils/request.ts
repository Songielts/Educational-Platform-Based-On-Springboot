import axios, { type AxiosError, type InternalAxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 不需要跳转的API路径列表
const SILENT_APIS = [
  '/study/progress',
  '/study/video/progress',
  '/study/note'
]

const isSilentApi = (url: string | undefined): boolean => {
  if (!url) return false
  return SILENT_APIS.some(path => url.includes(path))
}

// 统一清除登录状态
const clearLoginState = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  localStorage.removeItem('userRole')
}

// 请求拦截器
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error: AxiosError) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    // blob 响应直接放行，不做 JSON 解析
    if (response.config.responseType === 'blob') {
      return response
    }

    const res = response.data

    if (res.code !== 200) {
      if (!isSilentApi(response.config.url)) {
        ElMessage.error(res.message || '请求失败')
      }

      // ==================== 关键修复：只有 401 才清除 token 跳转 ====================
      // 403（权限不足）、400（参数校验失败）等其他错误只弹窗，不强制跳转
      if (res.code === 401 && !isSilentApi(response.config.url)) {
        clearLoginState()
        window.location.href = '/login?t=' + Date.now()
      }

      return Promise.reject(new Error(res.message || '请求失败'))
    }

    return response
  },
  (error: AxiosError<ApiResponse>) => {
    const url = error.config?.url

    if (error.response) {
      const status = error.response.status

      if (status === 401 || status === 403) {
        // ==================== 关键修复：blob 请求 401 静默处理 ====================
        if (error.config?.responseType === 'blob') {
          console.warn('[request] Blob 资源认证失败，静默处理:', url)
          return Promise.reject(error)
        }

        if (isSilentApi(url)) {
          console.warn('[request] 静默API认证失败:', url)
          return Promise.reject(error)
        }

        // ==================== 关键修复：403 只弹窗不跳转 ====================
        if (status === 403) {
          ElMessage.error('权限不足，无法执行此操作')
          return Promise.reject(error)
        }

        // 只有 HTTP 401 才清除 token 并跳转
        ElMessage.error('登录已过期，请重新登录')
        clearLoginState()
        window.location.href = '/login?t=' + Date.now()
      } else {
        if (!isSilentApi(url)) {
          ElMessage.error(error.response.data?.message || '请求失败')
        }
      }
    } else {
      if (!isSilentApi(url)) {
        ElMessage.error('网络错误')
      }
    }

    return Promise.reject(error)
  }
)

export const http = {
  get<T = any>(url: string, config?: any): Promise<T> {
    return request.get<ApiResponse<T>>(url, config).then(res => res.data.data)
  },
  post<T = any>(url: string, data?: any, config?: any): Promise<T> {
    return request.post<ApiResponse<T>>(url, data, config).then(res => res.data.data)
  },
  put<T = any>(url: string, data?: any, config?: any): Promise<T> {
    return request.put<ApiResponse<T>>(url, data, config).then(res => res.data.data)
  },
  delete<T = any>(url: string, config?: any): Promise<T> {
    return request.delete<ApiResponse<T>>(url, config).then(res => res.data.data)
  }
}

export default request