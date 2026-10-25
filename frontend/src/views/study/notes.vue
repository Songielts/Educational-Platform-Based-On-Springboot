<template>
  <div class="notes-container">
    <h2>学习笔记</h2>
    
    <div class="notes-actions">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索笔记"
        style="width: 300px"
        clearable
        @keyup.enter="fetchNotes"
      >
        <template #append>
          <el-button :icon="Search" @click="fetchNotes" />
        </template>
      </el-input>
    </div>
    
    <!-- 修复：使用可选链防止 undefined 错误 -->
    <el-empty description="暂无笔记" v-if="!loading && (!notes || notes.length === 0)" />
    
    <div class="notes-list" v-loading="loading" v-else>
      <el-card v-for="note in notes" :key="note.id" class="note-card" shadow="hover">
        <div class="note-header">
          <h4>{{ note.noteTitle || '未命名笔记' }}</h4>
          <div class="note-actions">
            <el-button type="primary" link :icon="Edit" @click="editNote(note)">编辑</el-button>
            <el-button type="danger" link :icon="Delete" @click="deleteNote(note.id)">删除</el-button>
          </div>
        </div>
        <p class="note-content">{{ note.noteContent }}</p>
        <div class="note-meta">
          <span class="course-name">{{ note.courseName }}</span>
          <span class="lesson-name" v-if="note.lessonName">{{ note.lessonName }}</span>
          <span class="note-time">{{ note.createTime }}</span>
        </div>
      </el-card>
    </div>
    
    <!-- 编辑笔记对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑笔记" width="600px">
      <el-form :model="editForm">
        <el-form-item label="标题">
          <el-input v-model="editForm.noteTitle" placeholder="笔记标题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="editForm.noteContent" type="textarea" :rows="6" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Search, Edit, Delete } from '@element-plus/icons-vue'
import { getNoteList, updateNote, deleteNote as apiDeleteNote } from '@/api/study'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const notes = ref<any[]>([])  // 确保初始化为空数组
const searchKeyword = ref('')
const editDialogVisible = ref(false)
const editForm = ref({
  id: 0,
  noteTitle: '',
  noteContent: ''
})

const fetchNotes = async () => {
  loading.value = true
  try {
    const res = await getNoteList({
      keyword: searchKeyword.value || undefined,
      current: 1,
      size: 20
    })
    
    console.log('API返回:', res) // 调试用
    
    if (res.data.code === 200) {
      // 修复：正确处理返回数据结构，使用 || [] 防止 undefined
      const responseData = res.data.data || res.data
      notes.value = responseData.records || []
    }
  } catch (error) {
    console.error('获取笔记失败:', error)
    notes.value = [] // 出错时重置为空数组
  } finally {
    loading.value = false
  }
}

const editNote = (note: any) => {
  editForm.value = {
    id: note.id,
    noteTitle: note.noteTitle || '',
    noteContent: note.noteContent || ''
  }
  editDialogVisible.value = true
}

const saveEdit = async () => {
  try {
    await updateNote(editForm.value.id, {
      noteTitle: editForm.value.noteTitle,
      noteContent: editForm.value.noteContent
    })
    ElMessage.success('更新成功')
    editDialogVisible.value = false
    fetchNotes()
  } catch (error) {
    console.error('更新笔记失败:', error)
  }
}

const deleteNote = (id: number) => {
  ElMessageBox.confirm('确定要删除这条笔记吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await apiDeleteNote(id)
      ElMessage.success('删除成功')
      fetchNotes()
    } catch (error) {
      console.error('删除笔记失败:', error)
    }
  })
}

onMounted(() => {
  fetchNotes()
})
</script>

<style scoped>
.notes-container {
  padding: 20px;
}

.notes-container h2 {
  margin-bottom: 20px;
}

.notes-actions {
  margin-bottom: 20px;
}

.notes-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.note-card {
  cursor: pointer;
}

.note-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.note-header h4 {
  margin: 0;
}

.note-content {
  color: #606266;
  line-height: 1.6;
  margin-bottom: 10px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.note-meta {
  display: flex;
  gap: 15px;
  font-size: 13px;
  color: #909399;
}

.course-name {
  color: #409EFF;
}
</style>