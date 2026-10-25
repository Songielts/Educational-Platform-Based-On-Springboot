<template>
  <div class="email-verify-wrapper">
    <el-input
      v-model="localEmail"
      placeholder="请输入邮箱"
      clearable
      :disabled="disabled"
      @input="$emit('update:email', localEmail)"
    >
      <template #append>
        <el-button
          :type="counting ? 'info' : 'primary'"
          :disabled="counting || disabled || !isValidEmail"
          @click="sendCode"
          style="min-width: 110px;"
        >
          {{ counting ? `${countdown}s 后重试` : '获取验证码' }}
        </el-button>
      </template>
    </el-input>
    <el-input
      v-model="localCode"
      placeholder="请输入验证码"
      maxlength="6"
      style="margin-top: 10px;"
      :disabled="disabled"
      @input="$emit('update:code', localCode)"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { sendEmailCode } from '@/api/email'

const props = defineProps<{
  email?: string
  code?: string
  disabled?: boolean
  scene?: string
}>()

const emit = defineEmits<{
  (e: 'update:email', val: string): void
  (e: 'update:code', val: string): void
  (e: 'send-success'): void
}>()

// 内部状态
const localEmail = ref(props.email || '')
const localCode = ref(props.code || '')
const counting = ref(false)
const countdown = ref(60)
let timer: ReturnType<typeof setInterval> | null = null

// 外部传入值变化时同步
watch(() => props.email, (v) => { if (v !== undefined) localEmail.value = v })
watch(() => props.code, (v) => { if (v !== undefined) localCode.value = v })

// 内部变化时通知父组件
watch(localEmail, (v) => emit('update:email', v))
watch(localCode, (v) => emit('update:code', v))

const isValidEmail = computed(() => {
  return /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(localEmail.value)
})

const sendCode = async () => {
  if (!isValidEmail.value) { ElMessage.warning('请输入正确的邮箱格式'); return }
  try {
    const res = await sendEmailCode({ email: localEmail.value, scene: props.scene || 'register' })
    if (res.data?.code === 200) {
      ElMessage.success('验证码已发送，请查收邮件')
      startCountdown()
      emit('send-success')
    } else {
      ElMessage.warning(res.data?.message || '发送失败')
    }
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '发送失败')
  }
}

const startCountdown = () => {
  counting.value = true
  countdown.value = 60
  timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) { counting.value = false; if (timer) clearInterval(timer) }
  }, 1000)
}

onUnmounted(() => { if (timer) clearInterval(timer) })
</script>

<style scoped>
.email-verify-wrapper { width: 100%; }
</style>