import request from '@/utils/request'

/**
 * 获取用户列表
 */
export const getUserList = (params: {
  keyword?: string
  userType?: number
  current?: number
  size?: number
}) => {
  return request.get('/admin/users', { params })
}

/**
 * 切换用户状态
 */
export const toggleUserStatus = (userId: number) => {
  return request.post(`/admin/users/${userId}/toggle-status`)
}

/**
 * 重置用户密码
 */
export const resetUserPassword = (userId: number) => {
  return request.post(`/admin/users/${userId}/reset-password`)
}

/**
 * 删除用户
 */
export const deleteUser = (userId: number) => {
  return request.delete(`/admin/users/${userId}`)
}
// 搜索用户（用于教师搜索学生）
export const searchUsers = (params: {
  keyword?: string
  userType?: number
}) => {
  return request.get('/user/search', { params })
}