import request from '@/utils/request'

// 登录
export const login = (data: {
  username: string
  password: string
  captcha?: string
  captchaKey?: string
}) => {
  return request.post('/auth/login', data)
}

// 注册（直接传 verifyCode，后端 DTO 要求这个字段名）
export const register = (data: {
  username: string
  password: string
  confirmPassword: string
  email: string
  realName: string
  userType: string
  verifyCode: string
}) => {
  return request.post('/auth/register', data)
}

// 发送邮箱验证码
export const sendEmailCode = (email: string, type: string = 'register') => {
  return request.post('/auth/send-email-code', null, {
    params: { email, type }
  })
}

// 重置密码
export const resetPassword = (data: {
  email: string
  emailCode: string
  newPassword: string
  confirmPassword: string
}) => {
  return request.post('/auth/reset-password', data)
}

// 获取用户信息
export const getUserInfo = () => {
  return request.get('/user/info')
}

// 更新用户信息
export const updateUserInfo = (data: {
  realName?: string
  email?: string
  phone?: string
  avatar?: string
  gender?: number
}) => {
  return request.put('/user/info', data)
}

// 修改密码
export const changePassword = (data: {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}) => {
  return request.put('/user/password', data)
}

// 获取图形验证码
export const getCaptcha = () => {
  return request.get('/captcha/image')
}