import request from '@/utils/request'

// ==================== 类型定义（学习统计看板）====================

export interface SummaryData {
  totalHours: number
  studyingCount: number
  completedCount: number
  avgCompletionRate: number
}

export interface TrendPoint {
  date: string
  minutes: number
}

export interface CourseProgressItem {
  courseName: string
  completionRate: number
  completedLessons: number
  totalLessons: number
}

export interface RadarItem {
  dimension: string
  score: number
}

export interface StudyDashboard {
  summary: SummaryData
  trend: TrendPoint[]
  courseProgress: CourseProgressItem[]
  abilityRadar: RadarItem[]
}

// ==================== 学习进度（对齐后端 StudyController）====================

/**
 * 获取课程下所有课时进度
 * GET /api/study/progress/course/{courseId}
 */
export function getCourseStudyProgress(courseId: number | string) {
  return request.get(`/study/progress/course/${courseId}`)
}

/**
 * 获取课时学习进度
 * GET /api/study/progress/{lessonId}
 */
export function getLessonProgress(lessonId: number | string) {
  return request.get(`/study/progress/${lessonId}`)
}

/**
 * 更新课时学习进度（30秒心跳上报）
 * POST /api/study/progress/{lessonId}
 *
 * @param data.onlineTime 本次实际观看秒数（增量 = currentTime - lastSavedTime）
 *                        后端据此累加 total_study_time，studySeconds=0 时不累加
 */
export function updateLessonProgress(lessonId: number | string, data: {
  completed?: boolean
  progress?: number
  lastPosition?: number
  duration?: number
  courseId?: number | string
  onlineTime?: number  // 本次实际观看秒数（增量），用于后端累加学习时长
}) {
  return request.post(`/study/progress/${lessonId}`, data)
}

// ==================== 笔记（对齐后端 StudyController）====================

/**
 * 获取课时笔记列表（按 lessonId 查）
 * GET /api/study/note/{lessonId}
 */
export function getLessonNotes(lessonId: number | string) {
  return request.get(`/study/note/${lessonId}`)
}

/**
 * 分页查询笔记列表
 * GET /api/study/note/list
 */
export function getNoteList(params: {
  keyword?: string
  current?: number
  size?: number
  courseId?: number | string
  lessonId?: number | string
}) {
  return request.get('/study/note/list', { params })
}

/**
 * 添加笔记
 * POST /api/study/note
 */
export function addNote(data: {
  lessonId: number
  courseId?: number | string
  noteTitle?: string
  noteContent?: string
  content?: string
  timestamp?: number
}) {
  return request.post('/study/note', data)
}

/**
 * 更新笔记
 * PUT /api/study/note/{noteId}
 */
export function updateNote(noteId: number | string, data: {
  noteTitle?: string
  noteContent?: string
  content?: string
  timestamp?: number
}) {
  return request.put(`/study/note/${noteId}`, data)
}

/**
 * 删除笔记
 * DELETE /api/study/note/{noteId}
 */
export function deleteNote(noteId: number | string) {
  return request.delete(`/study/note/${noteId}`)
}

// ==================== 课程练习（对齐后端 StudyPracticeController）====================

/**
 * 获取课时的练习题目
 * GET /api/study/practice/lesson/{lessonId}
 */
export function getPracticeQuestions(lessonId: number | string) {
  return request.get(`/study/practice/lesson/${lessonId}`)
}

/**
 * 提交练习答案
 * POST /api/study/practice/lesson/{lessonId}/submit
 */
export function submitPracticeAnswers(
  lessonId: number | string,
  answers: Record<number, string>
) {
  return request.post(`/study/practice/lesson/${lessonId}/submit`, answers)
}

/**
 * 获取课时的题目列表（教师配置用）
 * GET /api/study/practice/lesson/{lessonId}/questions
 */
export function getLessonQuestions(lessonId: number | string) {
  return request.get(`/study/practice/lesson/${lessonId}/questions`)
}

// ==================== 教师题目管理（对齐后端 StudyPracticeController）====================

/**
 * 教师：给课时添加题目
 * POST /api/study/practice/lesson/{lessonId}/question/{questionId}
 */
export function addQuestionToLesson(
  lessonId: number | string,
  questionId: number | string,
  questionOrder?: number,
  score?: number
) {
  return request.post(
    `/study/practice/lesson/${lessonId}/question/${questionId}`,
    null,
    { params: { questionOrder, score } }
  )
}

/**
 * 教师：从课时移除题目
 * DELETE /api/study/practice/lesson/{lessonId}/question/{questionId}
 */
export function removeQuestionFromLesson(
  lessonId: number | string,
  questionId: number | string
) {
  return request.delete(`/study/practice/lesson/${lessonId}/question/${questionId}`)
}

/**
 * 更新课时题目的排序和分值
 * PUT /api/study/practice/lesson/{lessonId}/question/{questionId}
 */
export function updateLessonQuestion(
  lessonId: number | string,
  questionId: number | string,
  data: { questionOrder?: number; score?: number }
) {
  return request.put(`/study/practice/lesson/${lessonId}/question/${questionId}`, data)
}

// ==================== 错题本 ====================

/**
 * 查询所有错题（考试 + 练习）
 * GET /api/study/practice/wrong-questions
 */
export function getWrongQuestions() {
  return request.get('/study/practice/wrong-questions')
}

/**
 * 移除错题
 * DELETE /api/study/practice/wrong-question/{questionId}
 */
export function removeWrongQuestion(questionId: number | string) {
  return request.delete(`/study/practice/wrong-question/${questionId}`)
}

// ==================== 学习统计（旧 StudyController 接口，保留兼容）====================

/**
 * 获取学生学习统计
 * GET /api/study/stat
 */
export function getStudyStatistics() {
  return request.get('/study/stat')
}

/**
 * 获取课程学习统计
 * GET /api/study/stat/course/{courseId}
 */
export function getCourseStudyStatistics(courseId: number | string) {
  return request.get(`/study/stat/course/${courseId}`)
}

/**
 * 获取学习趋势数据（旧接口，传对象格式）
 * GET /api/study/stat/trend
 * 用法：getStudyTrend({ days: 7 })
 */
export function getStudyTrend(params?: { days?: number }) {
  return request.get('/study/stat/trend', { params })
}

/**
 * 获取知识点掌握情况
 * GET /api/study/stat/knowledge
 */
export function getKnowledgeMastery() {
  return request.get('/study/stat/knowledge')
}

// ==================== 新学习统计看板（StatisticsController）====================

/**
 * 学习统计看板（综合数据，一次请求）
 * GET /api/study/statistics/dashboard
 */
export function getStudyDashboard() {
  return request.get<StudyDashboard>('/study/statistics/dashboard')
}

/**
 * 学习统计概览（顶部4张卡片）
 * GET /api/study/statistics/overview
 */
export function getStudyOverview() {
  return request.get<SummaryData>('/study/statistics/overview')
}

/**
 * 学习趋势（新看板接口，直接传数字天数）
 * GET /api/study/statistics/trend?days=7
 * 用法：getStatisticsTrend(7)
 */
export function getStatisticsTrend(days: number = 7) {
  return request.get<TrendPoint[]>('/study/statistics/trend', { params: { days } })
}

/**
 * 课程完成进度排行
 * GET /api/study/statistics/courses
 */
export function getCourseRanking() {
  return request.get<CourseProgressItem[]>('/study/statistics/courses')
}

/**
 * 能力分析雷达图数据
 * GET /api/study/statistics/radar
 */
export function getAbilityRadar() {
  return request.get<RadarItem[]>('/study/statistics/radar')
}

// ==================== 兼容别名（旧代码引用）====================

export const updateVideoProgress = (data: {
  lessonId: number | string
  courseId?: number | string
  currentPosition?: number
  totalDuration?: number
  progressPercent?: number
  studyTime?: number
  completed?: boolean
  progress?: number
  lastPosition?: number
}) => updateLessonProgress(data.lessonId, {
  completed: data.completed,
  progress: data.progress,
  lastPosition: data.currentPosition ?? data.lastPosition,
  courseId: data.courseId
})

export const getVideoProgress = getLessonProgress
export const getStudentStudyStat = getStudyStatistics
export const getLessonPracticeQuestions = getPracticeQuestions
export const submitLessonPractice = submitPracticeAnswers
export const getKnowledgeMasteryStat = getKnowledgeMastery
export const getStudyTrendData = getStudyTrend

export const getStatisticsDashboard = getStudyDashboard
export const getDashboardTrend = getStatisticsTrend