import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
      meta: { title: '登录' },
    },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      redirect: '/dashboard',
      children: [
        { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/Dashboard.vue'), meta: { title: '仪表盘', keepAlive: true } },
        { path: 'chat', name: 'Chat', component: () => import('@/views/Chat.vue'), meta: { title: '聊天记录' } },
        { path: 'chat/:sessionId', name: 'ChatSession', component: () => import('@/views/ChatSession.vue'), meta: { title: '会话回放' } },
        { path: 'knowledge', name: 'Knowledge', component: () => import('@/views/Knowledge.vue'), meta: { title: '知识库' } },
        { path: 'knowledge/:kbId/items', name: 'KnowledgeItems', component: () => import('@/views/KnowledgeItems.vue'), meta: { title: '知识条目' } },
        { path: 'avatar', name: 'Avatar', component: () => import('@/views/Avatar.vue'), meta: { title: '形象管理' } },
        { path: 'avatar/edit/:id?', name: 'AvatarEdit', component: () => import('@/views/AvatarEdit.vue'), meta: { title: '形象编辑' } },
        { path: 'llm', name: 'Llm', component: () => import('@/views/Llm.vue'), meta: { title: 'LLM配置' } },
        { path: 'llm/edit/:id?', name: 'LlmEdit', component: () => import('@/views/LlmEdit.vue'), meta: { title: '配置编辑' } },
        { path: 'rag', name: 'Rag', component: () => import('@/views/Rag.vue'), meta: { title: 'RAG文档' } },
        { path: 'rag/upload', name: 'RagUpload', component: () => import('@/views/RagUpload.vue'), meta: { title: '文档上传' } },
        { path: 'rag/:docId/chunks', name: 'RagChunks', component: () => import('@/views/RagChunks.vue'), meta: { title: '文档块' } },
        { path: 'rag/search', name: 'RagSearch', component: () => import('@/views/RagSearch.vue'), meta: { title: '检索测试' } },
        { path: 'rag/chunk-preview', name: 'RagChunkPreview', component: () => import('@/views/RagChunkPreview.vue'), meta: { title: '切割预览' } },
        { path: 'rag/eval', name: 'RagEval', component: () => import('@/views/RagEval.vue'), meta: { title: '评测管理' } },
        { path: 'report', name: 'Report', component: () => import('@/views/Report.vue'), meta: { title: '用户分析' } },
        { path: 'tourist-analysis', name: 'TouristAnalysis', component: () => import('@/views/TouristAnalysis.vue'), meta: { title: '游客消费分析' } },
        { path: 'tourist-import', name: 'TouristImport', component: () => import('@/views/TouristImport.vue'), meta: { title: '消费数据导入' } },
        { path: 'role', name: 'Role', component: () => import('@/views/Role.vue'), meta: { title: '角色管理' } },
        { path: 'route', name: 'Route', component: () => import('@/views/Route.vue'), meta: { title: '路线管理' } },
        { path: 'user', name: 'User', component: () => import('@/views/User.vue'), meta: { title: '用户管理' } },
      ],
    },
  ],
})

// 路由守卫
router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - VirtualWife Admin` : 'VirtualWife Admin'
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
