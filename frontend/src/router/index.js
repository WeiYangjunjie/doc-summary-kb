import { createRouter, createWebHistory } from 'vue-router'

// 路由表（按 CONTRACT.md 章节 8）
const routes = [
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('@/views/HomeView.vue'),
        meta: { title: '系统概览' }
      },
      {
        path: 'documents',
        name: 'documents',
        component: () => import('@/views/DocumentListView.vue'),
        meta: { title: '文档管理' }
      },
      {
        path: 'documents/:id',
        name: 'document-detail',
        component: () => import('@/views/DocumentDetailView.vue'),
        meta: { title: '文档详情' },
        props: true
      },
      {
        path: 'qa',
        name: 'qa',
        component: () => import('@/views/QaView.vue'),
        meta: { title: '智能问答' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFoundView.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.afterEach((to) => {
  if (to.meta?.title) {
    document.title = `${to.meta.title} - 文档摘要与知识库`
  } else {
    document.title = '文档摘要与知识库'
  }
})

export default router