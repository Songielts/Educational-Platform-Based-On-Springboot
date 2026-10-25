<template>
  <div class="profile-container">
    <h2>账号设置</h2>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>基本信息</span>
          </template>
          <el-form :model="userForm" :rules="userRules" ref="userFormRef" label-width="100px">
            <el-form-item label="头像">
              <el-upload
                class="avatar-uploader"
                action="/api/upload/image"
                :headers="uploadHeaders"
                :show-file-list="false"
                :on-success="handleAvatarSuccess"
                :on-error="() => ElMessage.error('头像上传失败')"
              >
                <img v-if="avatarDisplayUrl" :src="avatarDisplayUrl" class="avatar" @error="avatarDisplayUrl = ''" />
                <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
              </el-upload>
              <div class="upload-tip">点击上传新头像</div>
            </el-form-item>
            <el-form-item label="用户名">
              <el-input v-model="userForm.username" disabled />
            </el-form-item>
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="userForm.realName" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="userForm.email" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="userForm.phone" />
            </el-form-item>
            <el-form-item label="性别">
              <el-radio-group v-model="userForm.gender">
                <el-radio :value="0">保密</el-radio>
                <el-radio :value="1">男</el-radio>
                <el-radio :value="2">女</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveProfile">保存修改</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>
            <span>修改密码</span>
          </template>
          <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="100px">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="passwordForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="changeUserPassword">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { updateUserInfo, changePassword } from '@/api/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const userFormRef = ref()
const passwordFormRef = ref()
const avatarDisplayUrl = ref('')

const userForm = reactive({
  username: '', realName: '', email: '', phone: '', avatar: '', gender: 0
})

const passwordForm = reactive({
  oldPassword: '', newPassword: '', confirmPassword: ''
})

const userRules = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
}

const validateConfirmPassword = (rule: any, value: string, callback: any) => {
  if (value !== passwordForm.newPassword) callback(new Error('两次输入的密码不一致'))
  else callback()
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在6-20个字符之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const uploadHeaders = computed(() => {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
})

// 头像处理
const extractAvatarPath = (avatar: any): string => {
  if (!avatar) return ''
  if (typeof avatar === 'object') return avatar.value || avatar.url || avatar.path || ''
  return String(avatar)
}

const getAvatarDisplayUrl = (avatar: any): string => {
  const path = extractAvatarPath(avatar)
  if (!path) return ''
  if (path.startsWith('http')) return path
  const cleanPath = path.startsWith('/') ? path : `/${path}`
  return `/api${cleanPath}`
}

const loadAvatarDisplay = () => {
  avatarDisplayUrl.value = getAvatarDisplayUrl(userForm.avatar)
}

const extractAvatarFromResponse = (res: any): string => {
  if (!res) return ''
  const data = res.data
  if (typeof data === 'string') return data
  if (typeof data === 'object') return data.value || data.url || data.path || ''
  return String(data)
}

const handleAvatarSuccess = (res: any) => {
  if (res.code === 200) {
    userForm.avatar = extractAvatarFromResponse(res)
    loadAvatarDisplay()
    ElMessage.success('头像上传成功')
  } else {
    ElMessage.error(res.message || '头像上传失败')
  }
}

const saveProfile = async () => {
  const valid = await userFormRef.value?.validate().catch(() => false)
  if (!valid) return

  try {
    await updateUserInfo({
      realName: userForm.realName,
      email: userForm.email,
      phone: userForm.phone,
      avatar: userForm.avatar,
      gender: userForm.gender
    })

    const info = userStore.userInfo as any
    if (info) {
      userStore.setUserInfo({
        ...info,
        realName: userForm.realName,
        email: userForm.email,
        phone: userForm.phone,
        avatar: userForm.avatar,
        gender: userForm.gender
      })
    }

    ElMessage.success('保存成功')
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || '保存失败')
  }
}

const changeUserPassword = async () => {
  const valid = await passwordFormRef.value?.validate().catch(() => false)
  if (!valid) return

  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
      confirmPassword: passwordForm.confirmPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    setTimeout(() => {
      userStore.logout()
      router.push('/login')
    }, 1500)
  } catch (error: any) {
    ElMessage.error(error?.message || '修改密码失败')
  }
}

onMounted(() => {
  const info = userStore.userInfo as any
  if (info) {
    userForm.username = info.username || ''
    userForm.realName = info.realName || ''
    userForm.email = info.email || ''
    userForm.phone = info.phone || ''
    userForm.avatar = extractAvatarPath(info.avatar)
    userForm.gender = info.gender || 0
    loadAvatarDisplay()
  }
})
</script>

<style scoped>
.profile-container { padding: 20px; }
.profile-container h2 { margin-bottom: 20px; }
.avatar-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  overflow: hidden;
  width: 100px;
  height: 100px;
}
.avatar-uploader:hover { border-color: #409EFF; }
.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 100px;
  height: 100px;
  text-align: center;
  line-height: 100px;
}
.avatar {
  width: 100px;
  height: 100px;
  object-fit: cover;
  display: block;
}
.upload-tip { font-size: 12px; color: #909399; margin-top: 8px; }
</style>