<template>
  <div class="ai-question-page">
    <el-page-header @back="goBack" title="AI智能出题" />

    <el-card class="generate-card">
      <el-form :model="form" label-width="100px">
        <el-form-item label="所属课程" required>
          <el-select
            v-model="form.courseId"
            placeholder="选择课程"
            style="width: 100%;"
            :loading="courseLoading"
          >
            <el-option
              v-for="course in courseList"
              :key="course.id"
              :label="course.courseName"
              :value="course.id"
            />
          </el-select>
          <div v-if="courseList.length === 0 && !courseLoading" class="empty-hint">
            <el-text type="warning">暂无课程，请先创建课程</el-text>
          </div>
        </el-form-item>

        <el-form-item label="题型">
          <el-radio-group v-model="form.questionType">
            <el-radio value="单选题">单选题</el-radio>
            <el-radio value="多选题">多选题</el-radio>
            <el-radio value="判断题">判断题</el-radio>
            <el-radio value="填空题">填空题</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="题目数量">
          <el-slider v-model="form.count" :min="1" :max="10" show-stops show-tooltip />
          <span class="slider-value">{{ form.count }} 题</span>
        </el-form-item>

        <el-form-item label="难度">
          <el-radio-group v-model="form.difficulty">
            <el-radio value="简单">简单</el-radio>
            <el-radio value="中等">中等</el-radio>
            <el-radio value="困难">困难</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="提示词" required>
          <el-input
            v-model="form.prompt"
            type="textarea"
            :rows="4"
            placeholder="请输入提示词，例如：生成关于Java面向对象编程的基础题目，包括类、对象、继承、多态等知识点"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            @click="generateQuestions"
            :loading="generating"
            :disabled="!form.courseId || !form.prompt"
          >
            <el-icon><Pointer /></el-icon>
            AI生成题目
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="generatedQuestions.length > 0" class="preview-card">
      <template #header>
        <div class="preview-header">
          <span>生成结果预览（共 {{ generatedQuestions.length }} 题）</span>
          <div>
            <el-button type="info" @click="regenerateQuestions" :loading="generating" style="margin-right: 10px;">
              <el-icon><Refresh /></el-icon>
              重新生成
            </el-button>
            <el-button type="success" @click="saveQuestions" :loading="saving">
              <el-icon><Check /></el-icon>
              保存到题库
            </el-button>
          </div>
        </div>
      </template>

      <div v-for="(q, index) in generatedQuestions" :key="index" class="question-item">
        <div class="question-header">
          <span class="q-number">题目 {{ index + 1 }}</span>
          <div>
            <el-tag size="small" :type="getDifficultyType(q.difficulty || q.difficultyLevel)">难度{{ q.difficulty || q.difficultyLevel }}</el-tag>
            <el-tag size="small" style="margin-left: 5px;">{{ q.questionType || form.questionType }}</el-tag>
          </div>
        </div>
        <div class="question-content">{{ q.question || q.questionContent }}</div>
        <div v-if="q.options || q.questionOptions" class="options">
          <div v-for="(opt, i) in parseOptions(q.options || q.questionOptions)" :key="i" class="option">
            {{ String.fromCharCode(65 + i) }}. {{ opt }}
          </div>
        </div>
        <div class="answer">答案：{{ q.answer || q.correctAnswer }}</div>
        <div class="analysis" v-if="q.analysis || q.answerAnalysis">解析：{{ q.analysis || q.answerAnalysis }}</div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Pointer, Check, Refresh } from '@element-plus/icons-vue'
import { getTeacherCourses } from '@/api/course'
import { generateAIQuestions, saveAIQuestions } from '@/api/ai'

const router = useRouter()

const form = ref({
  courseId: undefined as number | undefined,
  questionType: '单选题',
  count: 5,
  difficulty: '中等',
  prompt: ''
})

const courseList = ref<any[]>([])
const courseLoading = ref(false)
const generating = ref(false)
const saving = ref(false)
const generatedQuestions = ref<any[]>([])

onMounted(async () => {
  await loadCourses()
})

// ==================== 辅助函数：统一解析后端响应（兼容两种模式）====================
const parseRes = (res: any) => {
  console.log('[parseRes] 原始res:', res)
  if (!res) return null
  // 模式1：res 是 AxiosResponse（res.data 是 Result）
  if (res.data && typeof res.data.code === 'number' && res.data.code === 200) {
    console.log('[parseRes] 模式1 AxiosResponse -> data:', res.data.data)
    return res.data.data
  }
  // 模式2：res 直接是 Result（response interceptor 已解包）
  if (typeof res.code === 'number' && res.code === 200) {
    console.log('[parseRes] 模式2 Result -> data:', res.data)
    return res.data
  }
  // 模式3：res 直接是数组或对象（无包装）
  if (Array.isArray(res) || (typeof res === 'object' && res !== null && !res.code)) {
    console.log('[parseRes] 模式3 直接数据:', res)
    return res
  }
  console.log('[parseRes] 无法解析，返回null')
  return null
}

const parseResMsg = (res: any): string => {
  if (!res) return '请求失败'
  if (res.data && res.data.message) return res.data.message
  if (res.message) return res.message
  return '请求失败'
}

const loadCourses = async () => {
  courseLoading.value = true
  try {
    const res = await getTeacherCourses({ current: 1, size: 100 }) as any
    console.log('[loadCourses] API原始返回:', res)

    const data = parseRes(res)
    console.log('[loadCourses] parseRes结果:', data)

    let courses: any[] = []
    if (Array.isArray(data)) {
      courses = data
      console.log('[loadCourses] 模式A：data是数组，长度:', courses.length)
    } else if (data?.records && Array.isArray(data.records)) {
      courses = data.records
      console.log('[loadCourses] 模式B：data.records，长度:', courses.length)
    } else if (data?.list && Array.isArray(data.list)) {
      courses = data.list
      console.log('[loadCourses] 模式C：data.list，长度:', courses.length)
    } else {
      console.log('[loadCourses] 无法提取课程数组，data:', data)
    }

    courseList.value = courses.map((c: any) => ({
      ...c,
      id: c.id ?? c.courseId ?? 0,
      courseName: c.courseName ?? c.name ?? '未命名课程'
    })).filter((c: any) => c.id !== 0)

    console.log('[loadCourses] 最终courseList:', courseList.value)

    if (courseList.value.length === 0) {
      ElMessage.warning('暂无课程数据，请先创建课程')
    }
  } catch (error) {
    console.error('[loadCourses] 异常:', error)
    courseList.value = []
    ElMessage.error('获取课程列表失败')
  } finally {
    courseLoading.value = false
  }
}

const parseOptions = (options: any): string[] => {
  try {
    if (typeof options === 'string') return JSON.parse(options)
    if (Array.isArray(options)) {
      if (options.length > 0 && typeof options[0] === 'object' && options[0] !== null) {
        if ('value' in options[0]) return options.map((o: any) => o.value)
        if ('label' in options[0]) return options.map((o: any) => o.label)
      }
      return options
    }
  } catch { return [] }
  return []
}

const generateQuestions = async () => {
  if (!form.value.courseId) { ElMessage.warning('请选择所属课程'); return }
  if (!form.value.prompt) { ElMessage.warning('请输入提示词'); return }

  generating.value = true
  try {
    const res = await generateAIQuestions({
      courseId: form.value.courseId,
      questionType: form.value.questionType,
      count: form.value.count,
      difficulty: form.value.difficulty,
      prompt: form.value.prompt
    }) as any

    const data = parseRes(res)
    generatedQuestions.value = data?.questions || data || []
    if (generatedQuestions.value.length === 0) {
      ElMessage.warning('未生成题目，请修改提示词后重试')
    } else {
      ElMessage.success(`成功生成 ${generatedQuestions.value.length} 道题目`)
    }
  } catch (error) {
    ElMessage.error('AI生成题目失败，请稍后重试')
  } finally {
    generating.value = false
  }
}

const regenerateQuestions = () => {
  generatedQuestions.value = []
  generateQuestions()
}

const saveQuestions = async () => {
  if (generatedQuestions.value.length === 0) return
  saving.value = true
  try {
    const normalizedQuestions = generatedQuestions.value.map((q: any) => ({
      bankId: 1,
      courseId: form.value.courseId,
      questionType: mapQuestionType(form.value.questionType),
      questionContent: q.question || q.questionContent,
      questionOptions: normalizeOptions(q.options || q.questionOptions),
      correctAnswer: q.answer || q.correctAnswer,
      answerAnalysis: q.analysis || q.answerAnalysis || '',
      difficultyLevel: mapDifficulty(form.value.difficulty),
      score: 10
    }))

    const res = await saveAIQuestions({
      courseId: form.value.courseId,
      questions: normalizedQuestions
    }) as any

    if (parseRes(res) !== null) {
      ElMessage.success('保存成功')
      generatedQuestions.value = []
    } else {
      ElMessage.error(parseResMsg(res))
    }
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const mapQuestionType = (typeName: string): number => {
  const map: Record<string, number> = { '单选题': 1, '多选题': 2, '判断题': 3, '填空题': 4 }
  return map[typeName] || 1
}

const mapDifficulty = (diff: string): number => {
  const map: Record<string, number> = { '简单': 1, '中等': 2, '困难': 3 }
  return map[diff] || 2
}

const normalizeOptions = (options: any): any[] | undefined => {
  if (!options) return undefined
  try {
    if (typeof options === 'string') {
      const parsed = JSON.parse(options)
      if (Array.isArray(parsed)) {
        return parsed.map((v: any, i: number) => ({
          key: String.fromCharCode(65 + i),
          value: typeof v === 'string' ? v : (v.value || v.label || '')
        }))
      }
    }
    if (Array.isArray(options)) {
      return options.map((v: any, i: number) => {
        if (typeof v === 'string') return { key: String.fromCharCode(65 + i), value: v }
        return { key: v.key || String.fromCharCode(65 + i), value: v.value || v.label || '' }
      })
    }
  } catch { return undefined }
  return undefined
}

const getDifficultyType = (level: number | string) => {
  const num = typeof level === 'string' ? parseInt(level) : level
  if (num <= 1) return 'success'
  if (num <= 2) return 'warning'
  return 'danger'
}

const goBack = () => { router.back() }
</script>

<style scoped>
.ai-question-page { padding: 20px; }
.generate-card { margin-top: 20px; margin-bottom: 20px; }
.slider-value { margin-left: 15px; color: #409eff; font-weight: bold; }
.empty-hint { margin-top: 5px; }
.preview-card { margin-bottom: 20px; }
.preview-header { display: flex; justify-content: space-between; align-items: center; }
.question-item { padding: 15px; margin-bottom: 15px; background: #f5f7fa; border-radius: 8px; border-left: 4px solid #409eff; }
.question-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.q-number { font-weight: bold; color: #409eff; font-size: 15px; }
.question-content { font-size: 15px; margin-bottom: 10px; line-height: 1.6; }
.options { margin-bottom: 10px; padding: 8px 12px; background: #fff; border-radius: 4px; }
.option { padding: 5px 0; color: #606266; }
.answer { color: #67c23a; font-weight: bold; margin-bottom: 5px; }
.analysis { color: #909399; font-size: 13px; line-height: 1.5; }
</style>