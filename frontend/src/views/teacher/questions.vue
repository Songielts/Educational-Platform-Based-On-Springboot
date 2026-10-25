<template>
  <div class="teacher-questions-container">
    <div class="page-header">
      <h2>题库管理</h2>
      <el-button type="primary" :icon="Plus" @click="openAddDialog">添加题目</el-button>
    </div>
    
    <!-- 筛选栏 -->
    <div class="filter-section">
      <el-select v-model="filterForm.bankId" placeholder="选择题库" clearable @change="fetchQuestions">
        <el-option v-for="bank in bankList" :key="bank.id" :label="bank.bankName" :value="bank.id" />
      </el-select>
      <el-select v-model="filterForm.questionType" placeholder="题型" clearable @change="fetchQuestions">
        <el-option label="单选题" :value="1" />
        <el-option label="多选题" :value="2" />
        <el-option label="判断题" :value="3" />
        <el-option label="填空题" :value="4" />
      </el-select>
      <el-select v-model="filterForm.difficultyLevel" placeholder="难度" clearable @change="fetchQuestions">
        <el-option label="简单" :value="1" />
        <el-option label="中等" :value="2" />
        <el-option label="困难" :value="3" />
      </el-select>
    </div>
    
    <el-empty description="暂无题目" v-if="!loading && (!questionList || questionList.length === 0)" />
    
    <div class="questions-list" v-loading="loading" v-else>
      <el-card v-for="q in questionList" :key="q.id" class="question-card" shadow="hover">
        <div class="question-header">
          <el-tag>{{ q.questionTypeName || getQuestionTypeName(q.questionType) }}</el-tag>
          <el-tag type="success" v-if="q.difficultyLevelName || q.difficultyLevel">{{ q.difficultyLevelName || getDifficultyName(q.difficultyLevel) }}</el-tag>
          <!-- 修复：分值加兜底显示 -->
          <span class="question-score">{{ q.score || 1 }}分</span>
        </div>
        <div class="question-content">{{ q.questionContent }}</div>
        <div class="options-list" v-if="q.questionOptions && q.questionOptions.length > 0">
          <div v-for="opt in q.questionOptions" :key="opt.key" class="option-item">
            {{ opt.key }}. {{ opt.value }}
          </div>
        </div>
        <div class="question-answer">
          <span>正确答案: {{ q.correctAnswer }}</span>
        </div>
        <div class="question-actions">
          <el-button type="primary" link @click="editQuestion(q)">编辑</el-button>
          <el-button type="danger" link @click="deleteQuestion(q.id)">删除</el-button>
        </div>
      </el-card>
    </div>
    
    <!-- ==================== 添加题目对话框 ==================== -->
    <el-dialog v-model="showAddDialog" title="添加题目" width="700px">
      <el-form :model="addForm" :rules="addRules" ref="addFormRef" label-width="100px">
        <el-form-item label="所属题库" prop="bankId">
          <el-select v-model="addForm.bankId" placeholder="选择题库">
            <el-option v-for="bank in bankList" :key="bank.id" :label="bank.bankName" :value="bank.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="题型" prop="questionType">
          <el-select v-model="addForm.questionType" placeholder="请选择题型">
            <el-option label="单选题" :value="1" />
            <el-option label="多选题" :value="2" />
            <el-option label="判断题" :value="3" />
            <el-option label="填空题" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="题目内容" prop="questionContent">
          <el-input v-model="addForm.questionContent" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="选项" v-if="addForm.questionType === 1 || addForm.questionType === 2">
          <div v-for="(opt, index) in addForm.questionOptions" :key="index" class="option-input-row">
            <el-input v-model="opt.key" style="width: 80px" placeholder="选项" />
            <el-input v-model="opt.value" placeholder="选项内容" />
            <el-button type="danger" link @click="removeOption(index)">删除</el-button>
          </div>
          <el-button type="primary" link @click="addOption">添加选项</el-button>
        </el-form-item>
        <el-form-item label="正确答案" prop="correctAnswer">
          <el-input v-model="addForm.correctAnswer" placeholder="请输入正确答案" />
        </el-form-item>
        <el-form-item label="答案解析">
          <el-input v-model="addForm.answerAnalysis" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="addForm.difficultyLevel">
            <el-option label="简单" :value="1" />
            <el-option label="中等" :value="2" />
            <el-option label="困难" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="分值">
          <el-input-number v-model="addForm.score" :min="0.5" :max="100" :step="0.5" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="createQuestion">保存</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 编辑题目对话框（新增）==================== -->
    <el-dialog v-model="showEditDialog" title="编辑题目" width="700px">
      <el-form :model="editForm" :rules="editRules" ref="editFormRef" label-width="100px">
        <el-form-item label="题型" prop="questionType">
          <el-select v-model="editForm.questionType" placeholder="请选择题型">
            <el-option label="单选题" :value="1" />
            <el-option label="多选题" :value="2" />
            <el-option label="判断题" :value="3" />
            <el-option label="填空题" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="题目内容" prop="questionContent">
          <el-input v-model="editForm.questionContent" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="选项" v-if="editForm.questionType === 1 || editForm.questionType === 2">
          <div v-for="(opt, index) in editForm.questionOptions" :key="index" class="option-input-row">
            <el-input v-model="opt.key" style="width: 80px" placeholder="选项" />
            <el-input v-model="opt.value" placeholder="选项内容" />
            <el-button type="danger" link @click="removeEditOption(index)">删除</el-button>
          </div>
          <el-button type="primary" link @click="addEditOption">添加选项</el-button>
        </el-form-item>
        <el-form-item label="正确答案" prop="correctAnswer">
          <el-input v-model="editForm.correctAnswer" placeholder="请输入正确答案" />
        </el-form-item>
        <el-form-item label="答案解析">
          <el-input v-model="editForm.answerAnalysis" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="editForm.difficultyLevel">
            <el-option label="简单" :value="1" />
            <el-option label="中等" :value="2" />
            <el-option label="困难" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="分值">
          <el-input-number v-model="editForm.score" :min="0.5" :max="100" :step="0.5" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { getQuestionList, addQuestion, updateQuestion, deleteQuestion as apiDeleteQuestion } from '@/api/exam'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const questionList = ref<any[]>([])
const bankList = ref<any[]>([
  { id: 1, bankName: '默认题库' },
  { id: 2, bankName: '数学题库' },
  { id: 3, bankName: '英语题库' }
])

// ==================== 添加题目 ====================
const showAddDialog = ref(false)
const addFormRef = ref()

const filterForm = reactive({
  bankId: undefined as number | undefined,
  questionType: undefined as number | undefined,
  difficultyLevel: undefined as number | undefined
})

const addForm = reactive({
  bankId: 1,
  questionType: 1,
  questionContent: '',
  questionOptions: [{ key: 'A', value: '' }, { key: 'B', value: '' }] as { key: string; value: string }[],
  correctAnswer: '',
  answerAnalysis: '',
  difficultyLevel: 1,
  score: 1
})

const addRules = {
  bankId: [{ required: true, message: '请选择题库', trigger: 'change' }],
  questionType: [{ required: true, message: '请选择题型', trigger: 'change' }],
  questionContent: [{ required: true, message: '请输入题目内容', trigger: 'blur' }],
  correctAnswer: [{ required: true, message: '请输入正确答案', trigger: 'blur' }]
}

// ==================== 编辑题目（新增）====================
const showEditDialog = ref(false)
const editFormRef = ref()
const editingQuestionId = ref<number | null>(null)

const editForm = reactive({
  questionType: 1,
  questionContent: '',
  questionOptions: [{ key: 'A', value: '' }, { key: 'B', value: '' }] as { key: string; value: string }[],
  correctAnswer: '',
  answerAnalysis: '',
  difficultyLevel: 1,
  score: 1
})

const editRules = {
  questionType: [{ required: true, message: '请选择题型', trigger: 'change' }],
  questionContent: [{ required: true, message: '请输入题目内容', trigger: 'blur' }],
  correctAnswer: [{ required: true, message: '请输入正确答案', trigger: 'blur' }]
}

const getQuestionTypeName = (type: number) => {
  const names: Record<number, string> = {
    1: '单选题',
    2: '多选题',
    3: '判断题',
    4: '填空题'
  }
  return names[type] || '未知'
}

const getDifficultyName = (level: number) => {
  const names: Record<number, string> = {
    1: '简单',
    2: '中等',
    3: '困难'
  }
  return names[level] || '未知'
}

const fetchQuestions = async () => {
  loading.value = true
  try {
    console.log('查询参数:', filterForm)
    const params: any = {
      current: 1,
      size: 20
    }
    if (filterForm.bankId) {
      params.bankId = filterForm.bankId
    }
    if (filterForm.questionType) {
      params.questionType = filterForm.questionType
    }
    if (filterForm.difficultyLevel) {
      params.difficultyLevel = filterForm.difficultyLevel
    }
    
    const res = await getQuestionList(params)
    console.log('题目列表响应:', res)
    
    if (res.data.code === 200) {
      questionList.value = res.data.data?.records || []
      console.log('题目列表:', questionList.value)
    } else {
      questionList.value = []
    }
  } catch (error) {
    console.error('获取题目失败:', error)
    questionList.value = []
  } finally {
    loading.value = false
  }
}

// ==================== 添加选项（添加模式）====================
const addOption = () => {
  const keys = ['A', 'B', 'C', 'D', 'E', 'F']
  const nextKey = keys[addForm.questionOptions.length] || 'X'
  addForm.questionOptions.push({ key: nextKey, value: '' })
}

const removeOption = (index: number) => {
  if (addForm.questionOptions.length > 2) {
    addForm.questionOptions.splice(index, 1)
  }
}

// ==================== 添加选项（编辑模式）====================
const addEditOption = () => {
  const keys = ['A', 'B', 'C', 'D', 'E', 'F']
  const nextKey = keys[editForm.questionOptions.length] || 'X'
  editForm.questionOptions.push({ key: nextKey, value: '' })
}

const removeEditOption = (index: number) => {
  if (editForm.questionOptions.length > 2) {
    editForm.questionOptions.splice(index, 1)
  }
}

const openAddDialog = () => {
  // 重置表单
  addForm.bankId = 1
  addForm.questionType = 1
  addForm.questionContent = ''
  addForm.correctAnswer = ''
  addForm.answerAnalysis = ''
  addForm.difficultyLevel = 1
  addForm.score = 1
  addForm.questionOptions = [{ key: 'A', value: '' }, { key: 'B', value: '' }]
  showAddDialog.value = true
}

const createQuestion = async () => {
  const valid = await addFormRef.value?.validate().catch(() => false)
  if (!valid) return
  
  try {
    console.log('添加题目数据:', addForm)
    const res = await addQuestion(addForm)
    console.log('添加题目响应:', res)
    ElMessage.success('添加成功')
    showAddDialog.value = false
    fetchQuestions()
  } catch (error) {
    console.error('添加失败:', error)
  }
}

// ==================== 编辑功能（修复）====================
const editQuestion = (q: any) => {
  editingQuestionId.value = q.id
  editForm.questionType = q.questionType || 1
  editForm.questionContent = q.questionContent || ''
  editForm.correctAnswer = q.correctAnswer || ''
  editForm.answerAnalysis = q.answerAnalysis || ''
  editForm.difficultyLevel = q.difficultyLevel || 1
  editForm.score = q.score || 1
  
  // 处理选项 - 兼容多种格式
  if (q.questionOptions && Array.isArray(q.questionOptions) && q.questionOptions.length > 0) {
    editForm.questionOptions = q.questionOptions.map((opt: any) => {
      if (typeof opt === 'string') {
        // 尝试解析 "A. xxx" 格式
        const match = opt.match(/^([A-Z])\.\s*(.*)$/)
        if (match) return { key: match[1], value: match[2] }
        return { key: 'A', value: opt }
      }
      return {
        key: opt.key || opt.optionKey || 'A',
        value: opt.value || opt.optionContent || opt.content || ''
      }
    })
  } else {
    // 默认两个空选项
    editForm.questionOptions = [{ key: 'A', value: '' }, { key: 'B', value: '' }]
  }
  
  showEditDialog.value = true
}

const saveEdit = async () => {
  const valid = await editFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!editingQuestionId.value) return
  
  try {
    const payload = {
      questionType: editForm.questionType,
      questionContent: editForm.questionContent,
      questionOptions: editForm.questionOptions,
      correctAnswer: editForm.correctAnswer,
      answerAnalysis: editForm.answerAnalysis,
      difficultyLevel: editForm.difficultyLevel,
      score: editForm.score
    }
    console.log('编辑题目数据:', payload)
    const res = await updateQuestion(editingQuestionId.value, payload)
    console.log('编辑题目响应:', res)
    ElMessage.success('修改成功')
    showEditDialog.value = false
    editingQuestionId.value = null
    fetchQuestions()
  } catch (error) {
    console.error('修改失败:', error)
    ElMessage.error('修改失败')
  }
}

const deleteQuestion = (id: number) => {
  ElMessageBox.confirm('确定要删除这道题吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await apiDeleteQuestion(id)
      ElMessage.success('删除成功')
      fetchQuestions()
    } catch (error) {
      console.error('删除失败:', error)
    }
  })
}

onMounted(() => {
  fetchQuestions()
})
</script>

<style scoped>
.teacher-questions-container {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
}

.filter-section {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
}

.questions-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.question-card {
  cursor: pointer;
}

.question-header {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 15px;
}

.question-score {
  margin-left: auto;
  color: #f56c6c;
  font-weight: bold;
}

.question-content {
  font-size: 16px;
  line-height: 1.6;
  margin-bottom: 15px;
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 15px;
}

.option-item {
  padding: 8px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.question-answer {
  background-color: #f0f9ff;
  padding: 10px;
  border-radius: 4px;
  margin-bottom: 10px;
}

.option-input-row {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
}
</style>