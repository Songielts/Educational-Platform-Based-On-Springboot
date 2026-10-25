import request from '@/utils/request'

// ==================== 考试管理（教师/管理员）====================

export function getExamList(params: { keyword?: string; current?: number; size?: number }) {
  return request.get('/exam/teacher/list', { params })
}

export function getExamDetail(id: number) {
  return request.get(`/exam/${id}`)
}

export function createExam(data: any) {
  return request.post('/exam', data)
}

export function updateExam(id: number, data: any) {
  return request.put(`/exam/${id}`, data)
}

export function deleteExam(id: number) {
  return request.delete(`/exam/${id}`)
}

export function publishExam(id: number) {
  return request.post(`/exam/${id}/publish`)
}

export function unpublishExam(id: number) {
  return request.post(`/exam/${id}/unpublish`)
}

export function endExam(id: number) {
  return request.post(`/exam/${id}/end`)
}

export function getPendingExams(params: { keyword?: string; current?: number; size?: number }) {
  return request.get('/exam/admin/pending', { params })
}

export function auditExam(examId: number, data: { approved: boolean; reason?: string }) {
  return request.post(`/exam/admin/audit/${examId}`, data)
}

export function adminDeleteExam(examId: number) {
  return request.delete(`/exam/admin/${examId}`)
}

// ==================== 考试题目管理 ====================

export function getExamQuestions(examId: number) {
  return request.get(`/exam/${examId}/questions`)
}

export function addQuestionToExam(data: { examId: number; questionId: number; score?: number; questionOrder?: number }) {
  return request.post('/exam/question/add-to-exam', data)
}

export function removeQuestionFromExam(examId: number, questionId: number) {
  return request.delete(`/exam/${examId}/question/${questionId}`)
}

export function updateExamQuestionScore(data: { examId: number; questionId: number; score: number }) {
  return request.put('/exam/question/score', data)
}

export function reorderExamQuestions(examId: number, questionOrders: any[]) {
  return request.put(`/exam/${examId}/question-order`, questionOrders)
}

// ==================== 题库管理 ====================

export function getQuestionList(params: { bankId?: number; questionType?: number; difficultyLevel?: number; keyword?: string; current?: number; size?: number }) {
  return request.get('/exam/question/list', { params })
}

export function getQuestionDetail(id: number) {
  return request.get(`/exam/question/${id}`)
}

export function addQuestion(data: any) {
  return request.post('/exam/question', data)
}

export function updateQuestion(id: number, data: any) {
  return request.put(`/exam/question/${id}`, data)
}

export function deleteQuestion(id: number) {
  return request.delete(`/exam/question/${id}`)
}

// ==================== 考试参与（学生端）====================

export function getStudentExamList(params: { keyword?: string; current?: number; size?: number }) {
  return request.get('/exam/student/list', { params })
}

export function startExam(id: number) {
  return request.post(`/exam/${id}/start`)
}

export function submitExam(id: number, answers: Record<number, string>) {
  return request.post(`/exam/${id}/submit`, answers)
}

export function getExamRecord(recordId: number) {
  return request.get(`/exam/record/${recordId}`)
}

export function getStudentExamRecords(params: { current?: number; size?: number }) {
  return request.get('/exam/record/list', { params })
}

// ==================== ⚠️ 以下接口已迁移，请从其他文件导入 ====================

// 错题本相关 —— 请从 '@/api/study' 导入：
//   import { getWrongQuestions, removeWrongQuestion } from '@/api/study'

// AI 智能出题 —— 请从 '@/api/ai' 导入：
//   import { generateAIQuestions, saveAIQuestions } from '@/api/ai'