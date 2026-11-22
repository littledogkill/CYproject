import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
import AuthView from '@/views/AuthView.vue'
import ProfileView from '@/views/ProfileView.vue'
import ForumView from '@/views/ForumView.vue'
import ChatView from '@/views/ChatView.vue'
import PostDetailView from '@/views/PostDetailView.vue'
import CreatePostView from '@/views/CreatePostView.vue'
import UserProfileView from '@/views/UserProfileView.vue'
import FollowListView from '@/views/FollowListView.vue'
import TalksView from '@/views/TalksView.vue'
import NotFoundView from '@/views/NotFoundView.vue'
import { hasAuthToken } from '@/utils/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
      meta: { requiresAuth: false },
    },
    {
      path: '/login',
      name: 'login',
      component: AuthView,
      meta: { requiresAuth: false },
    },
    {
      path: '/register',
      name: 'register',
      component: AuthView,
      meta: { requiresAuth: false },
    },
    {
      path: '/profile',
      name: 'profile',
      component: ProfileView,
    },
    {
      path: '/user/:userid',
      name: 'user-profile',
      component: UserProfileView,
    },
    {
      path: '/follows',
      name: 'follow-list',
      component: FollowListView,
    },
    {
      path: '/talks',
      name: 'talks',
      component: TalksView,
    },
    {
      path: '/forum',
      name: 'forum',
      component: ForumView,
    },
    {
      path: '/chat/:userid?',
      name: 'chat',
      component: ChatView,
    },
    {
      path: '/post/new',
      name: 'post-create',
      component: CreatePostView,
    },
    {
      path: '/post/:id',
      name: 'post-detail',
      component: PostDetailView,
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: NotFoundView,
      meta: { requiresAuth: false },   // 404 页无需登录即可访问
    },
  ],
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth === false || hasAuthToken()) {
    return true
  }

  return {
    name: 'login',
    query: { redirect: to.fullPath },
  }
})

export default router
