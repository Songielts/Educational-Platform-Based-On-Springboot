import request from '@/utils/request'

/**
 * 发送邮箱验证码
 * @param data { email: string, scene: string }
 */
export function sendEmailCode(data: { email: string; scene?: string }) {
  return request.post('/email/send-code', data)
}

/**
 * 验证邮箱验证码（可选，用于前端二次校验）
 * @param data { email: string, code: string, scene: string }
 */
export function verifyEmailCode(data: { email: string; code: string; scene?: string }) {
  return request.post('/email/verify-code', data)
}