<template>
  <div class="register-container">
    <div class="register-box">
      <div class="register-header">
        <h1>注册账号</h1>
        <p>加入在线教育平台，开启学习之旅</p>
      </div>
      
      <el-form
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        class="register-form"
      >
        <el-form-item prop="username">
          <el-input
            v-model="registerForm.username"
            placeholder="用户名"
            size="large"
            :prefix-icon="User"
          />
        </el-form-item>
        
        <el-form-item prop="realName">
          <el-input
            v-model="registerForm.realName"
            placeholder="真实姓名"
            size="large"
            :prefix-icon="UserFilled"
          />
        </el-form-item>
        
        <el-form-item prop="email">
          <el-input
            v-model="registerForm.email"
            placeholder="邮箱"
            size="large"
            :prefix-icon="Message"
          />
        </el-form-item>
        
        <el-form-item prop="emailCode">
          <div class="code-row">
            <el-input
              v-model="registerForm.emailCode"
              placeholder="邮箱验证码"
              size="large"
              :prefix-icon="Key"
            />
            <el-button
              size="large"
              :disabled="codeSending || countdown > 0"
              @click="sendCode"
            >
              {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="密码"
            size="large"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="确认密码"
            size="large"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        
        <el-form-item prop="userType">
          <el-radio-group v-model="registerForm.userType">
            <el-radio value="1">学生</el-radio>
            <el-radio value="2">教师</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="register-button"
            :loading="loading"
            @click="handleRegister"
          >
            注册
          </el-button>
        </el-form-item>
        
        <div class="register-options">
          <span>已有账号?</span>
          <el-link type="primary" @click="$router.push('/login')">立即登录</el-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { User, UserFilled, Message, Lock, Key } from '@element-plus/icons-vue'
import { sendEmailCode } from '@/api/auth'
import request from '@/utils/request'

const router = useRouter()

const registerFormRef = ref<FormInstance>()
const loading = ref(false)
const codeSending = ref(false)
const countdown = ref(0)

const registerForm = reactive({
  username: '',
  realName: '',
  email: '',
  emailCode: '',
  password: '',
  confirmPassword: '',
  userType: '1'
})

const validateConfirmPassword = (rule: any, value: string, callback: any) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 20, message: '用户名长度在4-20个字符之间', trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  emailCode: [
    { required: true, message: '请输入邮箱验证码', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在6-20个字符之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  userType: [
    { required: true, message: '请选择用户类型', trigger: 'change' }
  ]
}

const sendCode = async () => {
  if (!registerForm.email) {
    ElMessage.warning('请先输入邮箱')
    return
  }
  
  codeSending.value = true
  try {
    const res = await sendEmailCode(registerForm.email, 'register')
    if (res.data.code === 200) {
      ElMessage.success('验证码已发送')
      countdown.value = 60
      const timer = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) {
          clearInterval(timer)
        }
      }, 1000)
    }
  } catch (error) {
    console.error('发送验证码失败:', error)
  } finally {
    codeSending.value = false
  }
}

const handleRegister = async () => {
  // ===== 强制调试：校验 emailCode 是否有值 =====
  console.log('[注册调试] emailCode 原始值:', JSON.stringify(registerForm.emailCode))
  console.log('[注册调试] emailCode trim 后:', JSON.stringify(registerForm.emailCode?.trim()))

  if (!registerForm.emailCode || registerForm.emailCode.trim() === '') {
    ElMessage.error('验证码不能为空，请重新获取并输入')
    return
  }

  const valid = await registerFormRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    // 后端 DTO 同时有 verifyCode 和 emailCode 字段，同时传两个兼容
    const payload = {
      username: registerForm.username,
      realName: registerForm.realName,
      email: registerForm.email,
      verifyCode: registerForm.emailCode.trim(),
      emailCode: registerForm.emailCode.trim(),    // ← 同时传，防止后端读的是这个字段
      password: registerForm.password,
      confirmPassword: registerForm.confirmPassword,
      userType: registerForm.userType
    }

    console.log('[注册调试] 最终提交 payload:', JSON.stringify(payload, null, 2))

    // 直接调用 request.post，绕过 auth.ts 的 register 函数，确保字段名正确
    const res = await request.post('/auth/register', payload)
    console.log('[注册调试] 后端响应:', res)
    if (res.data.code === 200) {
      ElMessage.success('注册成功')
      router.push('/login')
    } else {
      ElMessage.warning(res.data?.message || '注册失败')
    }
  } catch (error: any) {
    console.error('[注册] 失败:', error)
    ElMessage.error(error?.response?.data?.message || error?.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px 0;
}

.register-box {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.register-header {
  text-align: center;
  margin-bottom: 30px;
}

.register-header h1 {
  font-size: 28px;
  color: #333;
  margin-bottom: 10px;
}

.register-header p {
  color: #999;
  font-size: 14px;
}

.register-form {
  margin-top: 20px;
}

.register-button {
  width: 100%;
}

.register-options {
  text-align: center;
  margin-top: 15px;
}

.register-options span {
  color: #999;
  margin-right: 5px;
}

.code-row {
  display: flex;
  gap: 10px;
}

.code-row .el-input {
  flex: 1;
}
</style>