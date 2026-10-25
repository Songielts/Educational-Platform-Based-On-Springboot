import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { public: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/register/index.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/views/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页', requiresAuth: true }
      },
      {
        path: 'courses',
        name: 'Courses',
        component: () => import('@/views/course/list.vue'),
        meta: { title: '课程中心', requiresAuth: true }
      },
      {
        path: 'course/:id',
        name: 'CourseDetail',
        component: () => import('@/views/teacher/course-detail.vue'),
        meta: { title: '课程详情', requiresAuth: true }
      },
      {
        path: 'my-classes',
        name: 'MyClasses',
        component: () => import('@/views/class/my-classes.vue'),
        meta: { title: '我的班级', requiresAuth: true }
      },
      {
        path: 'study/:lessonId',
        name: 'Study',
        component: () => import('@/views/study/index.vue'),
        meta: { title: '学习页面', requiresAuth: true }
      },
      {
        path: 'my-courses',
        name: 'MyCourses',
        component: () => import('@/views/course/my-courses.vue'),
        meta: { title: '我的课程', requiresAuth: true }
      },
      {
        path: 'notes',
        name: 'Notes',
        component: () => import('@/views/study/notes.vue'),
        meta: { title: '学习笔记', requiresAuth: true }
      },
      {
        path: 'exams',
        name: 'Exams',
        component: () => import('@/views/exam/list.vue'),
        meta: { title: '考试中心', requiresAuth: true }
      },
      {
        path: 'exam/:id',
        name: 'ExamDetail',
        component: () => import('@/views/exam/detail.vue'),
        meta: { title: '考试详情', requiresAuth: true }
      },
      {
        path: 'take-exam/:id',
        name: 'TakeExam',
        component: () => import('@/views/exam/take.vue'),
        meta: { title: '参加考试', requiresAuth: true }
      },
      {
        path: 'wrong-questions',
        name: 'WrongQuestions',
        component: () => import('@/views/exam/wrong-questions.vue'),
        meta: { title: '错题本', requiresAuth: true }
      },
      {
        path: 'statistics',
        name: 'Statistics',
        component: () => import('@/views/statistics/index.vue'),
        meta: { title: '学习统计', requiresAuth: true }
      },
      // 教师端路由
      {
        path: 'teacher/classes',
        name: 'TeacherClasses',
        component: () => import('@/views/teacher/classes.vue'),
        meta: { title: '班级管理', requiresAuth: true, role: 'TEACHER' }
      },
      {
        path: 'teacher/class/:classId/students',
        name: 'ClassStudents',
        component: () => import('@/views/teacher/Students.vue'),
        meta: { title: '班级学生管理', requiresAuth: true, role: 'TEACHER' }
      },
      {
        path: 'teacher/courses',
        name: 'TeacherCourses',
        component: () => import('@/views/teacher/courses.vue'),
        meta: { title: '课程管理', requiresAuth: true, role: 'TEACHER' }
      },
      // ==================== 课程详情管理（含删除章节/课时/视频/题目）====================
      {
        path: 'teacher/course/:courseId',
        name: 'TeacherCourseDetail',
        component: () => import('@/views/teacher/course-detail.vue'),
        meta: { title: '课程详情管理', requiresAuth: true, role: 'TEACHER' }
      },
      {
        path: 'teacher/questions',
        name: 'TeacherQuestions',
        component: () => import('@/views/teacher/questions.vue'),
        meta: { title: '题库管理', requiresAuth: true, role: 'TEACHER' }
      },
      {
        path: 'teacher/exams',
        name: 'TeacherExams',
        component: () => import('@/views/teacher/exams.vue'),
        meta: { title: '试卷管理', requiresAuth: true, role: 'TEACHER' }
      },
      {
        path: 'teacher/ai-question',
        name: 'AIQuestion',
        component: () => import('@/views/teacher/AIQuestion.vue'),
        meta: { title: 'AI智能出题', requiresAuth: true, role: 'TEACHER' }
      },
      // ==================== 关键修复：课时题目管理路由（query参数形式，兼容course-detail.vue跳转）====================
      {
        path: 'teacher/lesson-questions',
        name: 'LessonQuestions',
        component: () => import('@/views/teacher/lesson-questions.vue'),
        meta: { title: '课时题目管理', requiresAuth: true, role: 'TEACHER' }
      },
      // 管理员端路由
      {
        path: 'admin/users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/users.vue'),
        meta: { title: '用户管理', requiresAuth: true, role: 'ADMIN' }
      },
      {
        path: 'admin/courses',
        name: 'AdminCourses',
        component: () => import('@/views/admin/courses.vue'),
        meta: { title: '课程审核', requiresAuth: true, role: 'ADMIN' }
      },
      {
        path: 'admin/exams',
        name: 'AdminExams',
        component: () => import('@/views/admin/audit-exam.vue'),
        meta: { title: '考试审核', requiresAuth: true, role: 'ADMIN' }
      },
      // ==================== 关键修复：个人中心/账号设置对所有登录角色开放 ====================
      {
        path: 'profile-center',
        name: 'ProfileCenter',
        component: () => import('@/views/user/profile-center.vue'),
        meta: { title: '个人中心', requiresAuth: true, roles: ['STUDENT', 'TEACHER', 'ADMIN'] }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/user/profile.vue'),
        meta: { title: '账号设置', requiresAuth: true, roles: ['STUDENT', 'TEACHER', 'ADMIN'] }
      },
      {
        path: 'study/course/:courseId',
        name: 'CourseLearn',
        component: () => import('@/views/study/courselearn.vue'),
        meta: { title: '课程学习', requiresAuth: true, role: 'STUDENT' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// ==================== 关键修复：完善的路由守卫 ====================
router.beforeEach((to, from, next) => {
  console.log('[路由守卫]', to.path, 'meta:', to.meta)

  // 1. 公开页面直接放行
  if (to.meta?.public) {
    next()
    return
  }

  // 2. 从 localStorage 获取 token 和 userRole
  const token = localStorage.getItem('token')
  const userRole = localStorage.getItem('userRole')

  // 3. 需要登录但没有 token
  if (to.meta?.requiresAuth && !token) {
    console.log('[路由守卫] 需要登录，无 token，跳转登录')
    next('/login')
    return
  }

  // 4. 有 role 限制的路由，检查用户角色
  const requiredRole = to.meta?.role as string | undefined
  const requiredRoles = to.meta?.roles as string[] | undefined

  if (requiredRole || (requiredRoles && requiredRoles.length > 0)) {
    if (!userRole) {
      console.log('[路由守卫] 无角色信息，跳转登录')
      next('/login')
      return
    }

    // 单角色限制
    if (requiredRole && requiredRole !== userRole) {
      console.log(`[路由守卫] 角色不匹配: 需要 ${requiredRole}, 当前 ${userRole}`)
      // 不直接跳首页，而是根据当前角色跳对应首页
      if (userRole === 'STUDENT') next('/dashboard')
      else if (userRole === 'TEACHER') next('/teacher/courses')
      else if (userRole === 'ADMIN') next('/admin/courses')
      else next('/login')
      return
    }

    // 多角色限制
    if (requiredRoles && !requiredRoles.includes(userRole)) {
      console.log(`[路由守卫] 角色不匹配: 允许 ${requiredRoles.join('/')}, 当前 ${userRole}`)
      if (userRole === 'STUDENT') next('/dashboard')
      else if (userRole === 'TEACHER') next('/teacher/courses')
      else if (userRole === 'ADMIN') next('/admin/courses')
      else next('/login')
      return
    }
  }

  // 5. 已登录且权限通过，放行
  console.log('[路由守卫] 已登录，放行')
  next()
})

export default router