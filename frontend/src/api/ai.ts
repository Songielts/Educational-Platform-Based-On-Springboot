import request from '@/utils/request'

// ========== 学生端 AI 问答 ==========

// AI智能问答
export const askAI = (data: { question: string; courseId: number }) => {
  return request.post('/ai/chat/ask', data)
}

// AI学习推荐
export const getAIRecommend = (data: { profile: string; weakPoints: string }) => {
  return request.post('/ai/chat/recommend', data)
}

// ========== 教师端 AI 出题 ==========

// AI生成题目
export const generateAIQuestions = (data: {
  courseId: number
  questionType: string
  count: number
  difficulty: string
  prompt: string
}) => {
  return request.post('/ai/generate-questions', data)
}

// 保存AI生成的题目
export const saveAIQuestions = (data: {
  courseId: number | undefined
  questions: any[]
}) => {
  return request.post('/ai/question/save', data)
}