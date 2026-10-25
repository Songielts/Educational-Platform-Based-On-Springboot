import request from '@/utils/request'
import { updateLessonProgress, getLessonProgress, getCourseStudyProgress } from './study'

// ==================== 视频上传（FileController）====================

/**
 * 上传视频文件
 * POST /api/upload/video
 */
export const uploadVideo = (data: FormData) => {
  return request.post('/upload/video', data, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// ==================== 学习进度（适配层，保留兼容）====================

/**
 * 保存学习进度
 * 底层调用 updateLessonProgress，增加 studySeconds -> onlineTime 映射
 *
 * @param data.videoId       课时ID
 * @param data.courseId      课程ID
 * @param data.currentTime   当前播放位置（秒）
 * @param data.totalTime     视频总时长（秒）
 * @param data.studySeconds  本次实际观看增量秒数（核心修复字段）
 * @param data.isCompleted   是否已完成 0/1
 */
export const saveVideoProgress = (data: {
  videoId: number
  courseId: number
  currentTime: number
  totalTime: number
  studySeconds?: number  // 本次增量秒数（currentTime - lastSavedTime）
  isCompleted: number
}) => {
  return updateLessonProgress(data.videoId, {
    courseId: data.courseId,
    lastPosition: Math.floor(data.currentTime),
    duration: data.totalTime,
    completed: data.isCompleted === 1,
    progress: data.totalTime > 0 ? Math.floor((data.currentTime / data.totalTime) * 100) : 0,
    onlineTime: data.studySeconds  // 增量秒数 -> 后端 onlineTime 参数
  })
}

/**
 * 获取视频学习进度
 * 底层调用 getLessonProgress
 */
export const getVideoProgress = (videoId: number) => {
  return getLessonProgress(videoId)
}

/**
 * 获取课程学习进度统计
 * 底层调用 getCourseStudyProgress
 */
export const getCourseProgress = (courseId: number) => {
  return getCourseStudyProgress(courseId)
}

/**
 * 获取章节视频列表
 */
export const getVideoList = (chapterId: number) => {
  return request.get(`/course/video/list/${chapterId}`)
}

/**
 * 删除视频
 */
export const deleteVideo = (id: number) => {
  return request.delete(`/course/video/${id}`)
}