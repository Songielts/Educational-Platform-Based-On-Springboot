<template>
  <div class="teacher-exams-container">
    <div class="page-header">
      <h2>试卷管理</h2>
      <el-button type="primary" :icon="Plus" @click="openAddDialog">创建试卷</el-button>
    </div>

    <el-empty description="暂无试卷" v-if="!loading && (!examList || examList.length === 0)" />

    <div class="exam-list" v-loading="loading" v-else>
      <el-card v-for="exam in examList" :key="exam.id" class="exam-card" shadow="hover">
        <div class="exam-header">
          <h3>{{ exam.examName }}</h3>
          <el-tag :type="getStatusType(exam.status)">{{ exam.statusName || getStatusName(exam.status) }}</el-tag>
        </div>
        <div class="exam-info">
          <span><el-icon><Timer /></el-icon> {{ exam.timeLimit }}分钟</span>
          <span><el-icon><Document /></el-icon> {{ exam.questionCount || 0 }}题</span>
          <span><el-icon><Trophy /></el-icon> {{ exam.totalScore }}分</span>
        </div>
        <div class="exam-actions">
          <el-button type="primary" link :icon="Edit" @click="openEditDialog(exam)">编辑</el-button>
          <el-button type="warning" link :icon="Collection" @click="openQuestionManager(exam)">管理题目</el-button>
          <el-button type="success" link :icon="Top" @click="publishExamHandler(exam.id)" v-if="exam.status === 0">发布</el-button>
          <el-button type="info" link :icon="Bottom" @click="unpublishExamHandler(exam.id)" v-if="exam.status === 1">取消发布</el-button>
          <el-button type="danger" link :icon="Delete" @click="deleteExamHandler(exam.id)">删除</el-button>
        </div>
      </el-card>
    </div>

    <!-- 创建/编辑试卷对话框 -->
    <el-dialog v-model="showDialog" :title="isEdit ? '编辑试卷' : '创建试卷'" width="700px" destroy-on-close>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="试卷名称" prop="examName">
          <el-input v-model="form.examName" placeholder="请输入试卷名称" />
        </el-form-item>
        <el-form-item label="关联课程">
          <el-select v-model="form.courseId" placeholder="请选择关联课程（可选）" clearable style="width: 100%;">
            <el-option
              v-for="course in courseList"
              :key="course.id"
              :label="course.courseName"
              :value="course.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="考试类型">
          <el-select v-model="form.examType" placeholder="请选择考试类型">
            <el-option label="练习" :value="1" />
            <el-option label="测验" :value="2" />
            <el-option label="考试" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="考试时长">
          <el-input-number v-model="form.timeLimit" :min="10" :max="180" />
          <span style="margin-left: 10px;">分钟</span>
        </el-form-item>
        <el-form-item label="总分">
          <el-input-number v-model="form.totalScore" :min="10" :max="200" />
        </el-form-item>
        <el-form-item label="及格分">
          <el-input-number v-model="form.passScore" :min="0" :max="form.totalScore" />
        </el-form-item>
        <el-form-item label="重试次数">
          <el-input-number v-model="form.maxRetryCount" :min="1" :max="10" />
        </el-form-item>
        <el-form-item label="防作弊">
          <el-checkbox v-model="form.shuffleQuestions">题目乱序</el-checkbox>
          <el-checkbox v-model="form.shuffleOptions">选项乱序</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">{{ isEdit ? '保存' : '创建' }}</el-button>
      </template>
    </el-dialog>

    <!-- 题目管理对话框 -->
    <el-dialog v-model="showQuestionDialog" :title="`管理题目 - ${currentExam?.examName}`" width="900px" destroy-on-close>
      <div class="question-manager">
        <!-- 添加题目区域 -->
        <el-card class="add-question-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>添加题目</span>
              <el-radio-group v-model="addQuestionMode" size="small">
                <el-radio-button value="bank">从题库选择</el-radio-button>
                <el-radio-button value="manual">手动录入</el-radio-button>
              </el-radio-group>
            </div>
          </template>

          <!-- 从题库选择 -->
          <div v-if="addQuestionMode === 'bank'">
            <el-form :inline="true" :model="bankQueryForm">
              <el-form-item label="题型">
                <el-select v-model="bankQueryForm.questionType" placeholder="全部" clearable style="width: 120px;">
                  <el-option label="单选题" :value="1" />
                  <el-option label="多选题" :value="2" />
                  <el-option label="判断题" :value="3" />
                  <el-option label="填空题" :value="4" />
                </el-select>
              </el-form-item>
              <el-form-item label="难度">
                <el-select v-model="bankQueryForm.difficultyLevel" placeholder="全部" clearable style="width: 120px;">
                  <el-option label="简单" :value="1" />
                  <el-option label="中等" :value="2" />
                  <el-option label="困难" :value="3" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="loadBankQuestions" :icon="Search">查询</el-button>
              </el-form-item>
            </el-form>

            <el-table :data="bankQuestions" size="small" max-height="300" v-loading="bankLoading">
              <el-table-column prop="questionContent" label="题目内容" show-overflow-tooltip min-width="200" />
              <el-table-column prop="questionTypeName" label="题型" width="80" />
              <el-table-column prop="difficultyLevelName" label="难度" width="70" />
              <el-table-column label="操作" width="120" fixed="right">
                <template #default="{ row }">
                  <el-input-number v-model="row._score" :min="1" :max="50" :step="5" size="small" style="width: 80px;" placeholder="分值" />
                  <el-button type="primary" link size="small" @click="addFromBank(row)">添加</el-button>
                </template>
              </el-table-column>
            </el-table>

            <el-pagination
              v-model:current-page="bankQueryForm.current"
              v-model:page-size="bankQueryForm.size"
              :total="bankTotal"
              :page-sizes="[5, 10, 20]"
              layout="total, sizes, prev, pager, next"
              @change="loadBankQuestions"
              size="small"
              style="margin-top: 10px;"
            />
          </div>

          <!-- 手动录入 -->
          <div v-else>
            <el-form :model="manualQuestionForm" :rules="manualQuestionRules" ref="manualFormRef" label-width="80px">
              <el-form-item label="题型" prop="questionType">
                <el-radio-group v-model="manualQuestionForm.questionType">
                  <el-radio :value="1">单选题</el-radio>
                  <el-radio :value="2">多选题</el-radio>
                  <el-radio :value="3">判断题</el-radio>
                  <el-radio :value="4">填空题</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="题目内容" prop="questionContent">
                <el-input v-model="manualQuestionForm.questionContent" type="textarea" :rows="2" placeholder="请输入题目内容" />
              </el-form-item>
              <el-form-item label="选项" v-if="manualQuestionForm.questionType === 1 || manualQuestionForm.questionType === 2">
                <div v-for="(opt, idx) in manualQuestionForm.options" :key="idx" class="option-row">
                  <span class="option-label">{{ String.fromCharCode(65 + idx) }}.</span>
                  <el-input v-model="opt.value" placeholder="选项内容" style="width: 300px;" />
                  <el-checkbox v-model="opt.isCorrect" style="margin-left: 10px;">正确答案</el-checkbox>
                  <el-button type="danger" link :icon="Delete" @click="removeOption(idx)" v-if="manualQuestionForm.options.length > 2" />
                </div>
                <el-button type="primary" link :icon="Plus" @click="addOption" v-if="manualQuestionForm.options.length < 8">添加选项</el-button>
              </el-form-item>
              <el-form-item label="正确答案" v-if="manualQuestionForm.questionType === 3">
                <el-radio-group v-model="manualQuestionForm.correctAnswer">
                  <el-radio value="正确">正确</el-radio>
                  <el-radio value="错误">错误</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="正确答案" prop="correctAnswer" v-if="manualQuestionForm.questionType === 4">
                <el-input v-model="manualQuestionForm.correctAnswer" placeholder="请输入正确答案" />
              </el-form-item>
              <el-form-item label="正确答案" prop="correctAnswer" v-if="manualQuestionForm.questionType === 1">
                <el-select v-model="manualQuestionForm.correctAnswer" placeholder="选择正确答案">
                  <el-option
                    v-for="(opt, idx) in manualQuestionForm.options"
                    :key="idx"
                    :label="String.fromCharCode(65 + idx) + '. ' + (opt.value || '空选项')"
                    :value="String.fromCharCode(65 + idx)"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="正确答案" prop="correctAnswer" v-if="manualQuestionForm.questionType === 2">
                <el-select v-model="manualQuestionForm.correctAnswer" multiple placeholder="选择正确答案">
                  <el-option
                    v-for="(opt, idx) in manualQuestionForm.options"
                    :key="idx"
                    :label="String.fromCharCode(65 + idx) + '. ' + (opt.value || '空选项')"
                    :value="String.fromCharCode(65 + idx)"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="答案解析">
                <el-input v-model="manualQuestionForm.answerAnalysis" type="textarea" :rows="2" placeholder="请输入答案解析（可选）" />
              </el-form-item>
              <el-form-item label="难度">
                <el-rate v-model="manualQuestionForm.difficultyLevel" :max="3" :colors="['#67C23A', '#E6A23C', '#F56C6C']" />
              </el-form-item>
              <el-form-item label="分值" prop="score">
                <el-input-number v-model="manualQuestionForm.score" :min="1" :max="50" :step="5" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="submitManualQuestion" :loading="manualSubmitting">添加题目</el-button>
                <el-button @click="resetManualForm">重置</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-card>

        <!-- 当前试卷题目列表 -->
        <el-divider content-position="left">
          <el-icon><Document /></el-icon>
          当前试卷题目（共 {{ examQuestions.length }} 题，总分 {{ examTotalScore }} 分）
        </el-divider>

        <el-empty description="暂无题目，请从上方添加" v-if="examQuestions.length === 0" />

        <el-table :data="examQuestions" size="small" v-else>
          <el-table-column type="index" label="序号" width="60" />
          <el-table-column prop="questionContent" label="题目内容" show-overflow-tooltip min-width="200" />
          <el-table-column prop="questionTypeName" label="题型" width="80" />
          <el-table-column prop="score" label="分值" width="80">
            <template #default="{ row }">
              <el-input-number v-model="row.score" :min="1" :max="50" size="small" style="width: 70px;" @change="updateQuestionScore(row)" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row, $index }">
              <el-button type="primary" link size="small" @click="moveQuestion($index, -1)" :disabled="$index === 0">上移</el-button>
              <el-button type="primary" link size="small" @click="moveQuestion($index, 1)" :disabled="$index === examQuestions.length - 1">下移</el-button>
              <el-button type="danger" link size="small" @click="removeQuestion(row)">移除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <template #footer>
        <el-button @click="showQuestionDialog = false">关闭</el-button>
        <el-button type="primary" @click="saveQuestionOrder" :loading="saveOrderLoading" v-if="examQuestions.length > 0">保存排序与分值</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import {
  Plus, Timer, Document, Trophy, Edit, Collection,
  Delete, Top, Bottom, Search
} from '@element-plus/icons-vue'
import {
  getExamList, createExam, updateExam, deleteExam,
  publishExam, unpublishExam, getExamQuestions,
  addQuestionToExam, removeQuestionFromExam,
  updateExamQuestionScore, reorderExamQuestions,
  getQuestionList, addQuestion
} from '@/api/exam'
import { getTeacherCourses } from '@/api/course'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

// ==================== 辅助函数：统一解析后端响应 ====================
// 你的 request.ts 返回完整 AxiosResponse，结构为 { data: { code, message, data } }
const parseRes = (res: any) => {
  if (!res) return null
  if (res.data && res.data.code === 200) {
    return res.data.data
  }
  return null
}

const parseResMsg = (res: any): string => {
  if (!res || !res.data) return '请求失败'
  return res.data.message || '请求失败'
}

const loading = ref(false)
const examList = ref<any[]>([])
const courseList = ref<any[]>([])

const showDialog = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  id: undefined as number | undefined,
  examName: '',
  courseId: undefined as number | undefined,
  examType: 1,
  timeLimit: 60,
  totalScore: 100,
  passScore: 60,
  maxRetryCount: 3,
  shuffleQuestions: false,
  shuffleOptions: false
})
const rules: FormRules = {
  examName: [{ required: true, message: '请输入试卷名称', trigger: 'blur' }]
}

const showQuestionDialog = ref(false)
const currentExam = ref<any>(null)
const examQuestions = ref<any[]>([])
const addQuestionMode = ref<'bank' | 'manual'>('bank')
const saveOrderLoading = ref(false)

const bankLoading = ref(false)
const bankQuestions = ref<any[]>([])
const bankTotal = ref(0)
const bankQueryForm = reactive({
  questionType: undefined as number | undefined,
  difficultyLevel: undefined as number | undefined,
  current: 1,
  size: 10
})

const manualFormRef = ref<FormInstance>()
const manualSubmitting = ref(false)
const manualQuestionForm = reactive({
  questionType: 1,
  questionContent: '',
  options: [
    { value: '', isCorrect: false },
    { value: '', isCorrect: false },
    { value: '', isCorrect: false },
    { value: '', isCorrect: false }
  ] as { value: string; isCorrect: boolean }[],
  correctAnswer: '' as string | string[],
  answerAnalysis: '',
  difficultyLevel: 2,
  score: 10
})
const manualQuestionRules: FormRules = {
  questionType: [{ required: true, message: '请选择题型', trigger: 'change' }],
  questionContent: [{ required: true, message: '请输入题目内容', trigger: 'blur' }],
  correctAnswer: [{ required: true, message: '请选择正确答案', trigger: 'change' }],
  score: [{ required: true, message: '请输入分值', trigger: 'blur' }]
}

const examTotalScore = computed(() => {
  return examQuestions.value.reduce((sum, q) => sum + (Number(q.score) || 0), 0)
})

const getStatusType = (status: number) => {
  const types: Record<number, string> = { 0: 'info', 1: 'success', 2: 'danger' }
  return types[status] || 'info'
}
const getStatusName = (status: number) => {
  const names: Record<number, string> = { 0: '草稿', 1: '已发布', 2: '已结束' }
  return names[status] || '未知'
}

const fetchExams = async () => {
  loading.value = true
  try {
    const res = await getExamList({ current: 1, size: 20 }) as any
    console.log('试卷列表原始响应:', res)
    const data = parseRes(res)
    examList.value = data?.records || []
    console.log('解析到的试卷列表:', examList.value)
  } catch (error) {
    console.error('获取试卷列表失败:', error)
    examList.value = []
  } finally {
    loading.value = false
  }
}

const fetchCourses = async () => {
  try {
    const res = await getTeacherCourses({ current: 1, size: 100 }) as any
    const data = parseRes(res)
    courseList.value = data?.records || []
  } catch (error) {
    courseList.value = []
  }
}

const openAddDialog = () => {
  isEdit.value = false
  form.id = undefined
  form.examName = ''
  form.courseId = undefined
  form.examType = 1
  form.timeLimit = 60
  form.totalScore = 100
  form.passScore = 60
  form.maxRetryCount = 3
  form.shuffleQuestions = false
  form.shuffleOptions = false
  showDialog.value = true
}

const openEditDialog = (exam: any) => {
  isEdit.value = true
  Object.assign(form, {
    id: exam.id,
    examName: exam.examName,
    courseId: exam.courseId,
    examType: exam.examType || 1,
    timeLimit: exam.timeLimit,
    totalScore: exam.totalScore,
    passScore: exam.passScore,
    maxRetryCount: exam.maxRetryCount || 3,
    shuffleQuestions: exam.shuffleQuestions === 1,
    shuffleOptions: exam.shuffleOptions === 1
  })
  showDialog.value = true
}

const submitForm = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const data = {
      examName: form.examName,
      courseId: form.courseId,
      examType: form.examType,
      timeLimit: form.timeLimit,
      totalScore: form.totalScore,
      passScore: form.passScore,
      maxRetryCount: form.maxRetryCount,
      shuffleQuestions: form.shuffleQuestions ? 1 : 0,
      shuffleOptions: form.shuffleOptions ? 1 : 0
    }
    if (isEdit.value && form.id) {
      await updateExam(form.id, data)
      ElMessage.success('更新成功')
    } else {
      await createExam(data)
      ElMessage.success('创建成功')
    }
    showDialog.value = false
    fetchExams()
  } catch (error) {
    console.error(isEdit.value ? '更新失败:' : '创建失败:', error)
  } finally {
    submitting.value = false
  }
}

const publishExamHandler = async (id: number) => {
  try {
    await ElMessageBox.confirm('发布后学生将可以在考试中心看到此试卷，是否继续？', '确认发布')
    await publishExam(id)
    ElMessage.success('发布成功')
    fetchExams()
  } catch (error: any) {
    if (error !== 'cancel') console.error('发布失败:', error)
  }
}

const unpublishExamHandler = async (id: number) => {
  try {
    await ElMessageBox.confirm('取消发布后学生将无法看到此试卷，是否继续？', '确认取消')
    await unpublishExam(id)
    ElMessage.success('已取消发布')
    fetchExams()
  } catch (error: any) {
    if (error !== 'cancel') console.error('取消发布失败:', error)
  }
}

const deleteExamHandler = (id: number) => {
  ElMessageBox.confirm('确定要删除这个试卷吗？删除后不可恢复！', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteExam(id)
      ElMessage.success('删除成功')
      fetchExams()
    } catch (error) {
      console.error('删除失败:', error)
    }
  }).catch(() => {})
}

// ===== 题目管理 =====

const openQuestionManager = async (exam: any) => {
  currentExam.value = exam
  showQuestionDialog.value = true
  await loadExamQuestions(exam.id)
  await loadBankQuestions()
}

const loadExamQuestions = async (examId: number) => {
  try {
    const res = await getExamQuestions(examId) as any
    examQuestions.value = parseRes(res) || []
  } catch (error) {
    examQuestions.value = []
  }
}

const loadBankQuestions = async () => {
  bankLoading.value = true
  try {
    const params: any = {
      current: bankQueryForm.current,
      size: bankQueryForm.size
    }
    if (bankQueryForm.questionType) params.questionType = bankQueryForm.questionType
    if (bankQueryForm.difficultyLevel) params.difficultyLevel = bankQueryForm.difficultyLevel

    const res = await getQuestionList(params) as any
    const data = parseRes(res)
    if (data) {
      bankQuestions.value = (data.records || []).map((q: any) => ({ ...q, _score: 10 }))
      bankTotal.value = data.total || 0
    } else {
      bankQuestions.value = []
      bankTotal.value = 0
    }
  } catch (error) {
    bankQuestions.value = []
  } finally {
    bankLoading.value = false
  }
}

const addFromBank = async (question: any) => {
  if (!currentExam.value) return
  try {
    const res = await addQuestionToExam({
      examId: currentExam.value.id,
      questionId: question.id,
      score: question._score || 10
    }) as any
    if (parseRes(res) !== null) {
      ElMessage.success('添加成功')
      await loadExamQuestions(currentExam.value.id)
    } else {
      ElMessage.error(parseResMsg(res))
    }
  } catch (error) {
    ElMessage.error('添加题目失败')
  }
}

const addOption = () => {
  if (manualQuestionForm.options.length < 8) {
    manualQuestionForm.options.push({ value: '', isCorrect: false })
  }
}
const removeOption = (idx: number) => {
  if (manualQuestionForm.options.length > 2) {
    manualQuestionForm.options.splice(idx, 1)
  }
}

const submitManualQuestion = async () => {
  if (!currentExam.value) return
  const valid = await manualFormRef.value?.validate().catch(() => false)
  if (!valid) return

  let questionOptions: any[] | undefined = undefined
  let correctAnswer = ''

  if (manualQuestionForm.questionType === 1 || manualQuestionForm.questionType === 2) {
    questionOptions = manualQuestionForm.options.map((opt, idx) => ({
      key: String.fromCharCode(65 + idx),
      value: opt.value
    }))
    if (manualQuestionForm.questionType === 2) {
      correctAnswer = Array.isArray(manualQuestionForm.correctAnswer)
        ? (manualQuestionForm.correctAnswer as string[]).join(',')
        : (manualQuestionForm.correctAnswer as string)
    } else {
      correctAnswer = manualQuestionForm.correctAnswer as string
    }
  } else if (manualQuestionForm.questionType === 3) {
    correctAnswer = manualQuestionForm.correctAnswer as string
  } else {
    correctAnswer = manualQuestionForm.correctAnswer as string
  }

  manualSubmitting.value = true
  try {
    const addRes = await addQuestion({
      bankId: 1,
      questionType: manualQuestionForm.questionType,
      questionContent: manualQuestionForm.questionContent,
      questionOptions,
      correctAnswer,
      answerAnalysis: manualQuestionForm.answerAnalysis,
      difficultyLevel: manualQuestionForm.difficultyLevel,
      score: manualQuestionForm.score
    }) as any

    console.log('创建题目响应:', addRes)

    const newQuestionId = parseRes(addRes)
    console.log('解析到的题目ID:', newQuestionId)

    if (!newQuestionId) {
      ElMessage.error('添加题目失败：未获取到题目ID')
      return
    }

    const linkRes = await addQuestionToExam({
      examId: currentExam.value.id,
      questionId: newQuestionId,
      score: manualQuestionForm.score
    }) as any

    if (parseRes(linkRes) !== null) {
      ElMessage.success('题目添加成功')
      resetManualForm()
      await loadExamQuestions(currentExam.value.id)
      await loadBankQuestions()
    } else {
      ElMessage.error(parseResMsg(linkRes))
    }
  } catch (error: any) {
    console.error('添加题目失败:', error)
    ElMessage.error(error.response?.data?.message || '添加题目失败')
  } finally {
    manualSubmitting.value = false
  }
}

const resetManualForm = () => {
  manualQuestionForm.questionType = 1
  manualQuestionForm.questionContent = ''
  manualQuestionForm.options = [
    { value: '', isCorrect: false },
    { value: '', isCorrect: false },
    { value: '', isCorrect: false },
    { value: '', isCorrect: false }
  ]
  manualQuestionForm.correctAnswer = ''
  manualQuestionForm.answerAnalysis = ''
  manualQuestionForm.difficultyLevel = 2
  manualQuestionForm.score = 10
  manualFormRef.value?.resetFields()
}

const removeQuestion = async (question: any) => {
  if (!currentExam.value) return
  try {
    await ElMessageBox.confirm('确定从试卷中移除此题目？', '确认')
    await removeQuestionFromExam(currentExam.value.id, question.questionId)
    ElMessage.success('移除成功')
    await loadExamQuestions(currentExam.value.id)
  } catch (error: any) {
    if (error !== 'cancel') console.error('移除失败:', error)
  }
}

const updateQuestionScore = async (row: any) => {
  if (!currentExam.value) return
  try {
    await updateExamQuestionScore({
      examId: currentExam.value.id,
      questionId: row.questionId,
      score: row.score
    })
    ElMessage.success('分值更新成功')
  } catch (error) {
    console.error('更新分值失败:', error)
  }
}

const moveQuestion = async (index: number, direction: number) => {
  const newIndex = index + direction
  if (newIndex < 0 || newIndex >= examQuestions.value.length) return

  const temp = examQuestions.value[index]
  examQuestions.value[index] = examQuestions.value[newIndex]
  examQuestions.value[newIndex] = temp

  try {
    const questionOrders = examQuestions.value.map((q, idx) => ({
      questionId: q.questionId,
      questionOrder: idx + 1
    }))
    await reorderExamQuestions(currentExam.value!.id, questionOrders)
  } catch (error) {
    console.error('保存排序失败:', error)
  }
}

const saveQuestionOrder = async () => {
  if (!currentExam.value) return
  saveOrderLoading.value = true
  try {
    const questionOrders = examQuestions.value.map((q, idx) => ({
      questionId: q.questionId,
      questionOrder: idx + 1,
      score: q.score
    }))
    await reorderExamQuestions(currentExam.value.id, questionOrders)
    ElMessage.success('保存成功')
    await loadExamQuestions(currentExam.value.id)
    fetchExams()
  } catch (error) {
    console.error('保存失败:', error)
  } finally {
    saveOrderLoading.value = false
  }
}

onMounted(() => {
  fetchExams()
  fetchCourses()
})
</script>

<style scoped>
.teacher-exams-container { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; }
.exam-list { display: flex; flex-direction: column; gap: 15px; }
.exam-card { cursor: pointer; transition: all 0.3s; }
.exam-card:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.exam-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; }
.exam-header h3 { margin: 0; font-size: 18px; }
.exam-info { display: flex; gap: 20px; margin-bottom: 15px; color: #606266; font-size: 14px; }
.exam-info span { display: flex; align-items: center; gap: 5px; }
.exam-actions { display: flex; gap: 5px; flex-wrap: wrap; }
.question-manager { max-height: 600px; overflow-y: auto; }
.add-question-card { margin-bottom: 20px; background: #fafafa; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.option-row { display: flex; align-items: center; margin-bottom: 10px; gap: 5px; }
.option-label { font-weight: bold; color: #409eff; min-width: 24px; }
</style>