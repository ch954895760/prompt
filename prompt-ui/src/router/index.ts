import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/Login.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      redirect: '/dashboard',
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: () => import('@/views/Dashboard.vue'),
    },
    {
      path: '/prompts',
      name: 'prompts',
      component: () => import('@/views/PromptList.vue'),
    },
    {
      path: '/editor',
      name: 'editor',
      component: () => import('@/views/PromptEditor.vue'),
    },
    {
      path: '/editor/:id',
      name: 'editor-edit',
      component: () => import('@/views/PromptEditor.vue'),
    },
    {
      path: '/categories',
      name: 'categories',
      component: () => import('@/views/CategoryManage.vue'),
    },
    {
      path: '/tags',
      name: 'tags',
      component: () => import('@/views/TagManage.vue'),
    },
    {
      path: '/settings',
      name: 'settings',
      component: () => import('@/views/Settings.vue'),
    },
  ],
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  if (!to.meta.public && !userStore.isLoggedIn) {
    next('/login')
  } else {
    next()
  }
})

export default router
