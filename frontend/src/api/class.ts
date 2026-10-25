import request from '@/utils/request'

// ==================== 公共接口 ====================

// 获取班级详情
export const getClassDetail = (id: number) => {
  return request.get(`/class/${id}`)
}

// 学生加入班级
export const joinClass = (joinCode: string) => {
  return request.post('/class/join', null, { params: { joinCode } })
}

// 学生退出班级
export const leaveClass = (classId: number) => {
  return request.post(`/class/${classId}/leave`)
}

// 获取学生加入的班级列表
export const getStudentClasses = () => {
  return request.get('/class/student/list')
}
// 检查学生是否加入了任何班级
export const checkStudentInAnyClass = () => {
  return request.get('/class/student/check-in-class')
}
// ==================== 教师端接口 ====================

// 获取教师的班级列表
export const getTeacherClasses = (params: {
  keyword?: string
  current?: number
  size?: number
}) => {
  return request.get('/class/teacher/list', { params })
}

// 创建班级
export const createClass = (data: {
  classCode: string
  className: string
  description?: string
  coverImage?: string
  maxStudents: number
}) => {
  return request.post('/class', data)
}

// 更新班级
export const updateClass = (id: number, data: any) => {
  return request.put(`/class/${id}`, data)
}

// 删除班级
export const deleteClass = (id: number) => {
  return request.delete(`/class/${id}`)
}

// 生成班级加入码
export const generateJoinCode = (classId: number, expireHours: number = 24) => {
  return request.post(`/class/${classId}/join-code`, null, { params: { expireHours } })
}

// 批量导入学生
export const batchImportStudents = (classId: number, studentIds: number[]) => {
  return request.post(`/class/${classId}/students/batch`, studentIds)
}

// 移除班级学生
export const removeStudent = (classId: number, studentId: number) => {
  return request.delete(`/class/${classId}/students/${studentId}`)
}

// ==================== 班级学生管理接口 ====================

// 获取班级学生列表
export const getClassStudents = (classId: number) => {
  return request.get(`/class/${classId}/students`)
}

// 添加学生到班级
export const addStudentToClass = (classId: number, studentId: number) => {
  return request.post(`/class/${classId}/students`, null, { params: { studentId } })
}

// 从班级移除学生
export const removeStudentFromClass = (classId: number, studentId: number) => {
  return request.delete(`/class/${classId}/students/${studentId}`)
}

// ==================== 用户搜索接口 ====================

// 搜索学生（用于添加学生时搜索）
export const searchStudents = (keyword: string) => {
  return request.get('/user/search', { params: { keyword, userType: 1 } })
}