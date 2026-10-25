<template>
  <div class="ai-chat">
    <el-card class="chat-card">
      <template #header>
        <div class="chat-header">
          <el-icon><ChatDotRound /></el-icon>
          <span>AI学习助手</span>
        </div>
      </template>
      
      <div class="chat-messages" ref="messageContainer">
        <div 
          v-for="(msg, index) in messages" 
          :key="index"
          :class="['message', msg.role]"
        >
          <div class="avatar">
            <el-avatar v-if="msg.role === 'ai'" :icon="UserFilled" />
            <el-avatar v-else>{{ getUserInitial() }}</el-avatar>
          </div>
          <div class="content">
            <div class="text" v-html="formatMessage(msg.content)"></div>
          </div>
        </div>
        <el-skeleton v-if="loading" :rows="3" animated />
      </div>
      
      <div class="chat-input">
        <el-input
          v-model="inputMessage"
          type="textarea"
          :rows="2"
          placeholder="输入你的问题，AI会为你解答..."
          @keyup.enter.ctrl="sendMessage"
        />
        <el-button 
          type="primary" 
          @click="sendMessage"
          :loading="loading"
        >
          发送
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { ChatDotRound, UserFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { askAI } from '@/api/ai'

const props = defineProps<{
  courseId: number
}>()

const userStore = useUserStore()
const messages = ref([
  { role: 'ai', content: '你好！我是你的AI学习助手，有什么可以帮助你的吗？' }
])
const inputMessage = ref('')
const loading = ref(false)
const messageContainer = ref<HTMLElement>()

// 获取用户名字首字母
const getUserInitial = () => {
  const username = (userStore.userInfo as any)?.username || 'U'
  return username.charAt(0).toUpperCase()
}

const sendMessage = async () => {
  if (!inputMessage.value.trim()) return
  
  const userMsg = inputMessage.value
  messages.value.push({ role: 'user', content: userMsg })
  inputMessage.value = ''
  loading.value = true
  
  try {
    const res = await askAI({
      question: userMsg,
      courseId: props.courseId
    })
    
    // ========== 关键修复：调试日志 + 正确的响应解析 ==========
    console.log('=== AI响应调试 ===')
    console.log('完整响应:', res)
    console.log('res.data:', res.data)
    console.log('res.data.data:', res.data?.data)
    
    // 后端返回: {code: 200, message: "success", data: {answer: "..."}}
    // 因此 answer 在 res.data.data.answer 中
    let answer = ''
    if (res.data?.data?.answer) {
      answer = res.data.data.answer
    } else if (res.data?.answer) {
      answer = res.data.answer
    } else {
      answer = '抱歉，未能获取到有效回答'
      console.error('无法解析响应结构:', res)
    }
    
    messages.value.push({ role: 'ai', content: answer })
    // ========== 修复结束 ==========
  } catch (error: any) {
    console.error('AI请求失败:', error)
    messages.value.push({ role: 'ai', content: '抱歉，AI服务暂时不可用' })
  } finally {
    loading.value = false
    nextTick(() => {
      messageContainer.value!.scrollTop = messageContainer.value!.scrollHeight
    })
  }
}

const formatMessage = (content: string | undefined) => {
  // 如果 content 为 undefined 或 null，返回空字符串
  if (!content) return ''
  
  // 简单的 Markdown 格式处理
  return content
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.+?)\*/g, '<em>$1</em>')
    .replace(/`(.+?)`/g, '<code>$1</code>')
    .replace(/\n/g, '<br>')
}
</script>

<style scoped>
.ai-chat {
  height: 500px;
}

.chat-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: bold;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
  background: #f5f7fa;
  border-radius: 4px;
  margin-bottom: 10px;
}

.message {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
}

.message.user {
  flex-direction: row-reverse;
}

.message .content {
  max-width: 70%;
  padding: 10px 15px;
  border-radius: 8px;
  background: #fff;
}

.message.user .content {
  background: #409eff;
  color: #fff;
}

.chat-input {
  display: flex;
  gap: 10px;
}

.chat-input .el-input {
  flex: 1;
}
</style>