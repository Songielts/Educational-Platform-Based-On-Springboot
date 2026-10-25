<template>
  <div class="video-upload">
    <el-upload
      drag
      action="#"
      :auto-upload="false"
      :on-change="handleFileChange"
      :show-file-list="false"
      accept=".mp4,.avi,.mov,.wmv"
    >
      <el-icon class="el-icon--upload"><upload-filled /></el-icon>
      <div class="el-upload__text">
        拖拽视频到此处或 <em>点击上传</em>
      </div>
      <template #tip>
        <div class="el-upload__tip">
          支持MP4/AVI/MOV/WMV格式，单个文件不超过500MB
        </div>
      </template>
    </el-upload>

    <el-form v-if="selectedFile" :model="form" label-width="80px">
      <el-form-item label="视频名称">
        <el-input v-model="form.videoName" placeholder="请输入视频名称" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="submitUpload" :loading="uploading">
          确认上传
        </el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { uploadVideo } from '@/api/video'

const props = defineProps<{
  chapterId: number
  courseId: number
}>()

const selectedFile = ref<File | null>(null)
const uploading = ref(false)
const form = ref({
  videoName: ''
})

const handleFileChange = (file: any) => {
  selectedFile.value = file.raw
  // 自动填充视频名称
  if (!form.value.videoName && file.name) {
    form.value.videoName = file.name.replace(/\.[^/.]+$/, '')
  }
}

const submitUpload = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请选择视频文件')
    return
  }
  if (!form.value.videoName) {
    ElMessage.warning('请输入视频名称')
    return
  }

  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    formData.append('chapterId', props.chapterId.toString())
    formData.append('courseId', props.courseId.toString())
    formData.append('videoName', form.value.videoName)

    await uploadVideo(formData)
    ElMessage.success('上传成功')
    
    // 清空表单
    selectedFile.value = null
    form.value.videoName = ''
  } catch (error) {
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
  }
}
</script>