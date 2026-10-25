<template>
  <div class="video-player-wrapper">
    <video
      ref="videoRef"
      class="video-player"
      :src="videoUrl"
      controls
      preload="auto"
      @timeupdate="onTimeUpdate"
      @loadedmetadata="onLoadedMetadata"
      @ended="onVideoEnded"
      @play="onPlay"
      @pause="onPause"
    ></video>
    
    <!-- 学习进度提示 -->
    <div v-if="savedProgress > 0 && !hasResumed" class="progress-tip">
      <el-alert
        title="检测到上次学习进度"
        :description="`上次观看到 ${formatTime(savedProgress)}，点击继续播放`"
        type="info"
        show-icon
        :closable="false"
      >
        <template #default>
          <el-button type="primary" size="small" @click="resumeProgress">
            继续学习
          </el-button>
          <el-button size="small" @click="hasResumed = true">
            从头开始
          </el-button>
        </template>
      </el-alert>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { saveVideoProgress, getVideoProgress } from '@/api/video'

const props = defineProps<{
  videoId: number
  courseId: number
  videoUrl: string
}>()

const videoRef = ref<HTMLVideoElement>()
const currentTime = ref(0)
const duration = ref(0)
const savedProgress = ref(0)
const hasResumed = ref(false)
let progressTimer: any = null
let isPlaying = ref(false)

// ==================== 核心修复：增量计算变量 ====================
/** 上次保存进度时的播放位置（秒），用于计算增量 */
let lastSavedTime = 0

// 页面加载时获取上次进度
onMounted(async () => {
  try {
    const res = await getVideoProgress(props.videoId)
    const data = parseData(res)
    if (data && data.currentTime > 0) {
      savedProgress.value = data.currentTime
      // 恢复时重置 lastSavedTime 为上次保存的位置
      lastSavedTime = data.currentTime
    }
  } catch (error) {
    console.log('获取进度失败', error)
  }
})

// 定时保存进度（每5秒）
const startSaveTimer = () => {
  if (!progressTimer) {
    progressTimer = setInterval(() => {
      if (currentTime.value > 0 && isPlaying.value) {
        saveProgress()
      }
    }, 5000)
  }
}

const stopSaveTimer = () => {
  if (progressTimer) {
    clearInterval(progressTimer)
    progressTimer = null
  }
}

onBeforeUnmount(() => {
  stopSaveTimer()
  saveProgress() // 页面离开前保存一次
})

const onPlay = () => {
  isPlaying.value = true
  startSaveTimer()
}

const onPause = () => {
  isPlaying.value = false
  stopSaveTimer()
  saveProgress()
}

const onTimeUpdate = () => {
  if (videoRef.value) {
    currentTime.value = Math.floor(videoRef.value.currentTime)
  }
}

const onLoadedMetadata = () => {
  if (videoRef.value) {
    duration.value = Math.floor(videoRef.value.duration)
  }
}

const onVideoEnded = () => {
  isPlaying.value = false
  stopSaveTimer()
  saveProgress(1) // 标记为已完成
  ElMessage.success('恭喜完成本节学习！')
}

// 恢复上次进度
const resumeProgress = () => {
  if (videoRef.value && savedProgress.value > 0) {
    videoRef.value.currentTime = savedProgress.value
    hasResumed.value = true
    // 关键：重置 lastSavedTime 为续播位置，避免重复计算
    lastSavedTime = savedProgress.value
  }
}

/**
 * 保存进度（增量模式）
 * 
 * 核心修复：
 * 1. 计算增量 studySeconds = currentTime - lastSavedTime
 * 2. 只将增量传给后端，避免重复累加
 * 3. 保存成功后更新 lastSavedTime = currentTime
 * 4. studySeconds <= 0 时不上报时长（只更新播放位置）
 */
const saveProgress = async (isCompleted = 0) => {
  try {
    // 计算本次实际观看的增量秒数
    const delta = Math.max(0, currentTime.value - lastSavedTime)
    
    console.log(`[VideoPlayer] saveProgress: current=${currentTime.value}s, lastSaved=${lastSavedTime}s, delta=${delta}s`)

    await saveVideoProgress({
      videoId: props.videoId,
      courseId: props.courseId,
      currentTime: currentTime.value,      // 当前播放位置（用于续播）
      totalTime: duration.value,           // 视频总时长
      studySeconds: delta,                 // 增量秒数（核心修复）
      isCompleted
    })

    // 保存成功后才更新 lastSavedTime
    if (delta > 0) {
      lastSavedTime = currentTime.value
    }
  } catch (error) {
    console.log('保存进度失败', error)
  }
}

// 格式化时间
const formatTime = (seconds: number) => {
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

/**
 * 解析后端响应（兼容多种格式）
 */
const parseData = (res: any): any => {
  if (!res) return null
  if (res.data && res.data.data !== undefined && res.data.code === 200) return res.data.data
  if (res.data !== undefined && res.code === 200) return res.data
  if (res.data !== undefined) return res.data
  return res
}
</script>

<style scoped>
.video-player-wrapper {
  width: 100%;
  background: #000;
  position: relative;
}

.video-player {
  width: 100%;
  max-height: 500px;
  display: block;
}

.progress-tip {
  position: absolute;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  width: 400px;
  z-index: 10;
}
</style>