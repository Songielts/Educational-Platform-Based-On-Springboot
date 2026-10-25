import request from '@/utils/request'

// ==================== 课程管理（对齐后端 CourseController）====================

/**
 * 创建课程（教师/管理员）
 * POST /api/course
 */
export function createCourse(data: {
  courseName: string
  coverImage?: string
  subjectId?: number
  educationStage?: number
  difficultyLevel?: number
  description?: string
  [key: string]: any
}) {
  return request.post('/course', data)
}

/**
 * 更新课程（教师/管理员）
 * PUT /api/course/{courseId}
 */
export function updateCourse(courseId: number | string, data: any) {
  return request.put(`/course/${courseId}`, data)
}

/**
 * 删除课程（教师/管理员）
 * DELETE /api/course/{courseId}
 */
export function deleteCourse(courseId: number | string) {
  return request.delete(`/course/${courseId}`)
}

/**
 * 获取课程详情
 * GET /api/course/{courseId}
 */
export function getCourseDetail(courseId: number | string) {
  return request.get(`/course/${courseId}`)
}

/**
 * 分页查询公开课程
 * GET /api/course/public/list
 */
export function getPublicCourseList(params?: {
  keyword?: string
  subjectId?: number
  educationStage?: number
  current?: number
  size?: number
}) {
  return request.get('/course/public/list', { params })
}

/**
 * 分页查询教师的课程列表
 * GET /api/course/teacher/list
 */
export function getTeacherCourses(params?: {
  keyword?: string
  current?: number
  size?: number
}) {
  return request.get('/course/teacher/list', { params })
}

/**
 * 发布课程（教师提交审核）
 * POST /api/course/{courseId}/publish
 */
export function publishCourse(courseId: number | string) {
  return request.post(`/course/${courseId}/publish`)
}

/**
 * 下架课程
 * POST /api/course/{courseId}/offline
 */
export function offlineCourse(courseId: number | string) {
  return request.post(`/course/${courseId}/offline`)
}

/**
 * 查询所有已发布课程
 * GET /api/course/all/published
 */
export function getAllPublishedCourses() {
  return request.get('/course/all/published')
}

// ==================== 章节管理（对齐后端 CourseController）====================

/**
 * 添加章节
 * POST /api/course/chapter
 */
export function addChapter(data: any) {
  return request.post('/course/chapter', data)
}

/**
 * 更新章节
 * PUT /api/course/chapter/{chapterId}
 */
export function updateChapter(chapterId: number | string, data: any) {
  return request.put(`/course/chapter/${chapterId}`, data)
}

/**
 * 删除章节
 * DELETE /api/course/chapter/{chapterId}
 */
export function deleteChapter(chapterId: number | string) {
  return request.delete(`/course/chapter/${chapterId}`)
}

/**
 * 获取课程的章节树
 * GET /api/course/{courseId}/chapters
 */
export function getCourseChapters(courseId: number | string) {
  return request.get(`/course/${courseId}/chapters`)
}

// ==================== 课时管理（对齐后端 CourseController）====================

/**
 * 添加课时
 * POST /api/course/lesson
 */
export function addLesson(data: any) {
  return request.post('/course/lesson', data)
}

/**
 * 更新课时
 * PUT /api/course/lesson/{lessonId}
 */
export function updateLesson(lessonId: number | string, data: any) {
  return request.put(`/course/lesson/${lessonId}`, data)
}

/**
 * 删除课时
 * DELETE /api/course/lesson/{lessonId}
 */
export function deleteLesson(lessonId: number | string) {
  return request.delete(`/course/lesson/${lessonId}`)
}

/**
 * 获取课时详情
 * GET /api/course/lesson/{lessonId}
 */
export function getLessonDetail(lessonId: number | string) {
  return request.get(`/course/lesson/${lessonId}`)
}

// ==================== 学生课程（对齐后端 CourseController）====================

/**
 * 查询学生学习的课程列表（我的课程）
 * GET /api/course/student/list
 *
 * 注意：你之前描述的 /course/my-courses 后端不存在，实际接口是 /course/student/list
 */
export function getStudentCourses() {
  return request.get('/course/student/list')
}

/**
 * 学生选课
 * POST /api/course/student/select/{courseId}
 */
export function selectCourse(courseId: number | string) {
  return request.post(`/course/student/select/${courseId}`)
}

// ==================== 管理员审核（对齐后端 CourseController）====================

/**
 * 查询待审核课程列表（管理员）
 * GET /api/course/admin/pending
 */
export function getPendingCourses(params?: {
  keyword?: string
  current?: number
  size?: number
}) {
  return request.get('/course/admin/pending', { params })
}

/**
 * 审核课程（管理员通过/拒绝）
 * POST /api/course/admin/audit/{courseId}
 */
export function auditCourse(
  courseId: number | string,
  data: { approved: boolean; reason?: string }
) {
  return request.post(`/course/admin/audit/${courseId}`, data)
}

// ==================== 兼容别名（旧代码引用）====================

/** @deprecated 请使用 getPublicCourseList */
export const getPublicCourses = getPublicCourseList

/** @deprecated 请使用 getTeacherCourses */
export const getTeacherCourseList = getTeacherCourses

/** @deprecated 请使用 getCourseDetail */
export const getCourseById = getCourseDetail

/** @deprecated 请使用 getCourseChapters */
export const getChapterList = getCourseChapters

/** @deprecated 请使用 getLessonDetail */
export const getLessonList = getLessonDetail

/** @deprecated 请使用 addChapter */
export const createChapter = addChapter

/** @deprecated 请使用 updateChapter */
export const modifyChapter = updateChapter

/** @deprecated 请使用 addLesson */
export const createLesson = addLesson

/** @deprecated 请使用 updateLesson */
export const modifyLesson = updateLesson