<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import SpecularButton from '@/components/SpecularButton.vue'
import SideRays from '@/components/SideRays.vue'
import BubbleMenu from '@/components/BubbleMenu.vue'
import AnimatedList from '@/components/AnimatedList.vue'
import PostCard from '@/components/PostCard.vue'
import {
  deletePost,
  getConversations,
  getCurrentUser,
  getLikedPosts,
  getUserPosts,
  updateCurrentUser,
} from '@/utils/communityApi'
import { hasUnreadConversation } from '@/utils/conversationReadState'
import { vAuthedSrc } from '@/utils/authImage'

const router = useRouter()

// 右侧面板当前状态：菜单 / 我的发布 / 我的点赞 / 账号设置
const view = ref('menu')
const currentUser = reactive({
  username: '',
  name: '',
  handle: '',
  avatar: null,
  email: '',
  stats: { posts: 0, likes: 0, followers: 0 },
})
const worksPosts = ref([])
const likedPosts = ref([])
const unreadPeopleCount = ref(0)
const isLoading = ref(true)
const isSavingSettings = ref(false)
const pageError = ref('')
const deletingId = ref('')
const deleteError = ref('')

const menuItems = computed(() => [
  {
    key: 'works',
    label: '我的发布',
    href: '#works',
    ariaLabel: '我的发布',
    rotation: -8,
    hoverStyles: { bgColor: '#2563eb', textColor: '#ffffff' },
  },
  {
    key: 'likes',
    label: '我的点赞',
    href: '#likes',
    ariaLabel: '我的点赞',
    rotation: 8,
    hoverStyles: { bgColor: '#0ea5e9', textColor: '#ffffff' },
  },
  {
    key: 'forum',
    label: '论坛中心',
    href: '#forum',
    ariaLabel: '论坛中心',
    rotation: 8,
    hoverStyles: { bgColor: '#10b981', textColor: '#ffffff' },
  },
  {
    key: 'settings',
    label: '账号设置',
    href: '#settings',
    ariaLabel: '账号设置',
    rotation: -8,
    hoverStyles: { bgColor: '#8b5cf6', textColor: '#ffffff' },
  },
  {
    key: 'talks',
    label: '我的私聊',
    href: '#talks',
    ariaLabel: '我的私聊',
    rotation: -8,
    badge: unreadPeopleCount.value,
    hoverStyles: { bgColor: '#8b5cf6', textColor: '#ffffff' },
  },
  {
    key: 'follows',
    label: '关注列表',
    href: '#follows',
    ariaLabel: '关注列表',
    rotation: 8,
    hoverStyles: { bgColor: '#ef4444', textColor: '#ffffff' },
  },
])

const stats = computed(() => {
  return { works: currentUser.stats.posts, likes: currentUser.stats.likes }
})

const currentPosts = computed(() => (view.value === 'likes' ? likedPosts.value : worksPosts.value))
const viewTitle = computed(() => (view.value === 'likes' ? '我的点赞' : '我的发布'))

const settings = reactive({
  email: '',
  username: '',
  password: '',
})
const settingsMessage = ref('')

async function refreshUnreadCount() {
  try {
    const result = await getConversations({ page: 1, pageSize: 500 })
    const ownerId = currentUser.id || currentUser.username
    unreadPeopleCount.value = result.items.filter((room) =>
      hasUnreadConversation(room, ownerId),
    ).length
  } catch (error) {
    // 拉取新消息失败时保留旧值，避免影响菜单展示
  }
}

onMounted(async () => {
  isLoading.value = true
  pageError.value = ''
  try {
    const user = await getCurrentUser()
    Object.assign(currentUser, user)
    settings.email = user.email || ''
    settings.username = user.username || ''
    const [postsResult] = await Promise.all([
      getUserPosts(user.id, { page: 1, pageSize: 100 }),
      refreshUnreadCount(),
    ])
    worksPosts.value = postsResult.items
  } catch (error) {
    pageError.value =
      error.response?.data?.message || error.message || '加载个人主页失败，请稍后重试'
  } finally {
    isLoading.value = false
  }
})

// 每次从发布 / 点赞 / 设置面板返回菜单时，重新拉取私聊未读人数
watch(view, (value) => {
  deleteError.value = ''
  if (value === 'menu') {
    refreshUnreadCount()
  }
})

async function loadLikedPosts() {
  try {
    likedPosts.value = (await getLikedPosts({ page: 1, pageSize: 50 })).items
  } catch (error) {
    pageError.value =
      error.response?.data?.message || error.message || '加载点赞列表失败，请稍后重试'
  }
}

async function saveSettings() {
  if (isSavingSettings.value) return
  isSavingSettings.value = true
  settingsMessage.value = ''
  pageError.value = ''
  const payload = {
    email: settings.email.trim(),
    username: settings.username.trim(),
  }
  if (settings.password) payload.password = settings.password
  try {
    const user = await updateCurrentUser(payload)
    Object.assign(currentUser, user)
    settings.email = user.email || ''
    settings.username = user.username || ''
    settings.password = ''
    settingsMessage.value = '设置已保存'
  } catch (error) {
    pageError.value =
      error.response?.data?.message || error.message || '保存设置失败，请稍后重试'
  } finally {
    isSavingSettings.value = false
  }
}

function onMenuSelect(item) {
  switch (item.key) {
    case 'works':
      view.value = 'works'
      break
    case 'likes':
      view.value = 'likes'
      loadLikedPosts()
      break
    case 'forum':
      router.push('/forum')
      break
    case 'settings':
      view.value = 'settings'
      break
    case 'follows':
      router.push('/follows')
      break
    case 'talks':
      router.push('/talks')
      break
  }
}

function goHome() {
  router.push('/')
}

function openPost(item) {
  router.push(`/post/${item.id}`)
}

function openUser(item) {
  const id = item.authorId || item.authorUsername
  if (!id) return
  const isSelf =
    (currentUser.id && item.authorId && String(currentUser.id) === String(item.authorId)) ||
    (currentUser.username &&
      item.authorUsername &&
      currentUser.username === item.authorUsername)
  if (isSelf) {
    router.push('/profile')
    return
  }
  router.push(`/user/${encodeURIComponent(id)}`)
}

async function confirmDeletePost(item) {
  if (deletingId.value) return
  const title = item.title || '这篇帖子'
  const confirmed = window.confirm(`确定删除「${title}」吗？删除后无法恢复。`)
  if (!confirmed) return

  deletingId.value = item.id
  deleteError.value = ''
  try {
    await deletePost(item.id)
    worksPosts.value = worksPosts.value.filter((post) => String(post.id) !== String(item.id))
    currentUser.stats.posts = Math.max(0, Number(currentUser.stats.posts || 0) - 1)
  } catch (error) {
    deleteError.value =
      error.response?.data?.message || error.message || '删除帖子失败，请稍后重试'
  } finally {
    deletingId.value = ''
  }
}
</script>

<template>
  <div class="profile">
    <!-- SideRays 背景 -->
    <div class="profile__bg" aria-hidden="true">
      <SideRays
        :speed="2.5"
        ray-color1="#2563eb"
        ray-color2="#60a5fa"
        :intensity="3"
        :spread="2"
        origin="top-right"
        :tilt="0"
        :saturation="1.2"
        :blend="0.6"
        :falloff="0.8"
        :opacity="1"
      />
    </div>

    <!-- 左上角返回首页 -->
    <div class="profile__back">
      <SpecularButton
        size="md"
        :radius="999"
        text-color="#eaf2ff"
        line-color="#60a5fa"
        base-color="#1e3a8a"
        :intensity="1.1"
        :auto-animate="true"
        :speed="0.5"
        @click="goHome"
      >
        返回首页
      </SpecularButton>
    </div>

    <!-- 左侧 1/5：个人信息 -->
    <aside class="profile__info">
      <img
        v-if="currentUser.avatar"
        v-authed-src="currentUser.avatar"
        class="profile__avatar"
        alt="头像"
      />
      <div v-else class="profile__avatar profile__avatar--letter">
        {{ currentUser.name.slice(0, 1) }}
      </div>
      <h1 class="profile__name">{{ currentUser.name }}</h1>
      <p class="profile__email">{{ currentUser.email }}</p>

      <ul class="profile__stats">
        <li>
          <strong>{{ stats.works }}</strong>
          <span>作品数</span>
        </li>
        <li>
          <strong>{{ stats.likes }}</strong>
          <span>点赞数</span>
        </li>
        <li>
          <strong>—</strong>
          <span>其他</span>
        </li>
      </ul>
    </aside>

    <!-- 右侧 4/5：菜单 / 内容面板 -->
    <div class="profile__menu">
      <p v-if="isLoading" class="profile__message" role="status">正在加载个人资料…</p>
      <p v-if="pageError" class="profile__message profile__message--error" role="alert">
        {{ pageError }}
      </p>
      <!-- 默认菜单 -->
      <BubbleMenu
        v-if="view === 'menu'"
        :logo="null"
        :items="menuItems"
        menu-aria-label="打开操作菜单"
        menu-bg="rgba(10, 18, 40, 0.72)"
        menu-content-color="#dbeafe"
        :use-fixed-position="false"
        :default-open="true"
        animation-ease="back.out(1.5)"
        :animation-duration="0.5"
        :stagger-delay="0.12"
        @select="onMenuSelect"
      />

      <!-- 我的发布 / 我的点赞 -->
      <div v-else-if="view === 'works' || view === 'likes'" class="profile__panel">
        <div class="profile__panel-head">
          <SpecularButton
            size="sm"
            :radius="999"
            text-color="#eaf2ff"
            line-color="#60a5fa"
            base-color="#1e3a8a"
            :intensity="1.1"
            :auto-animate="true"
            :speed="0.5"
            @click="view = 'menu'"
          >
            返回菜单
          </SpecularButton>
          <h2 class="profile__panel-title">{{ viewTitle }}</h2>
        </div>

        <p v-if="deleteError" class="profile__delete-error" role="alert">
          {{ deleteError }}
        </p>

        <main class="profile__panel-list">
          <AnimatedList
            :items="currentPosts"
            :max-height="'100%'"
            :show-gradients="true"
            :enable-arrow-navigation="true"
            :display-scrollbar="true"
            @item-selected="openPost"
          >
            <template #item="{ item }">
              <div class="profile__post">
                <PostCard :post="item" @open-user="openUser" />
                <div v-if="view === 'works'" class="profile__post-actions">
                  <button
                    type="button"
                    class="profile__post-delete"
                    :disabled="deletingId === item.id"
                    @click.stop="confirmDeletePost(item)"
                  >
                    {{ deletingId === item.id ? '删除中…' : '删除' }}
                  </button>
                </div>
              </div>
            </template>
          </AnimatedList>

          <p v-if="!currentPosts.length" class="profile__panel-empty">暂无帖子</p>
        </main>
      </div>

      <!-- 账号设置 -->
      <div v-else-if="view === 'settings'" class="profile__panel profile__panel--settings">
        <div class="profile__panel-head">
          <SpecularButton
            size="sm"
            :radius="999"
            text-color="#eaf2ff"
            line-color="#60a5fa"
            base-color="#1e3a8a"
            :intensity="1.1"
            :auto-animate="true"
            :speed="0.5"
            @click="view = 'menu'"
          >
            返回菜单
          </SpecularButton>
          <h2 class="profile__panel-title">账号设置</h2>
        </div>

        <form class="settings" @submit.prevent="saveSettings">
          <label class="settings__label" for="settings-email">邮箱</label>
          <input
            id="settings-email"
            v-model="settings.email"
            class="settings__input"
            type="email"
            placeholder="name@example.com"
            autocomplete="email"
            required
            readonly
          />

          <label class="settings__label" for="settings-username">用户名</label>
          <input
            id="settings-username"
            v-model="settings.username"
            class="settings__input"
            type="text"
            placeholder="你的用户名"
            autocomplete="username"
            required
          />

          <label class="settings__label" for="settings-password">密码</label>
          <input
            id="settings-password"
            v-model="settings.password"
            class="settings__input"
            type="password"
            placeholder="输入新密码（留空则不变）"
            autocomplete="new-password"
          />

          <button
            class="settings__submit"
            type="submit"
            :disabled="isSavingSettings"
          >
            {{ isSavingSettings ? '保存中…' : '保存设置' }}
          </button>
          <p v-if="settingsMessage" class="settings__message" role="status">
            {{ settingsMessage }}
          </p>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile {
  position: relative;
  width: 100%;
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
  background: #05080f;
}

.profile__bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.profile__back {
  position: absolute;
  top: 22px;
  left: 22px;
  z-index: 3;
}

.profile__info {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 20%;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 24px 20px;
  border-right: 1px solid rgba(96, 165, 250, 0.14);
  background: linear-gradient(90deg, rgba(4, 9, 20, 0.72), rgba(4, 9, 20, 0.2));
  -webkit-backdrop-filter: blur(10px);
  backdrop-filter: blur(10px);
}

.settings__input:read-only {
  color: #999;
  cursor: not-allowed;
}

.profile__avatar {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid rgba(96, 165, 250, 0.5);
  box-shadow: 0 0 26px rgba(37, 99, 235, 0.45);
}

.profile__avatar--letter {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 34px;
  font-weight: 700;
  color: #eaf2ff;
  background: linear-gradient(135deg, #2563eb, #1e40af);
}

.profile__name {
  margin: 6px 0 0;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 0.01em;
  color: #eaf2ff;
}

.profile__email {
  margin: 0;
  font-size: 13px;
  color: rgba(147, 197, 253, 0.6);
  word-break: break-all;
}

.profile__stats {
  list-style: none;
  display: flex;
  gap: 18px;
  margin: 18px 0 0;
  padding: 0;
}

.profile__stats li {
  text-align: center;
}

.profile__stats strong {
  display: block;
  font-size: 20px;
  font-weight: 700;
  color: #eaf2ff;
}

.profile__stats span {
  display: block;
  margin-top: 2px;
  font-size: 12px;
  color: rgba(147, 197, 253, 0.55);
}

.profile__menu {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 80%;
  z-index: 2;
}

.profile__message {
  position: absolute;
  top: 72px;
  left: 48px;
  z-index: 4;
  color: rgba(191, 219, 254, 0.8);
}

.profile__message--error {
  color: #fca5a5;
}

.profile__panel {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  padding: 96px 48px 40px;
}

.profile__panel-head {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-bottom: 14px;
}

.profile__panel-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 0.01em;
  color: #eaf2ff;
}

.profile__panel-list {
  position: relative;
  flex: 1;
  min-height: 0;
}

.profile__panel-empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0;
  font-size: 14px;
  color: rgba(147, 197, 253, 0.55);
  pointer-events: none;
}

.profile__delete-error {
  margin: 0 0 10px;
  color: #fca5a5;
  font-size: 13px;
}

.profile__post {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.profile__post-actions {
  display: flex;
  justify-content: flex-end;
}

.profile__post-delete {
  padding: 6px 14px;
  border-radius: 999px;
  border: 1px solid rgba(248, 113, 113, 0.45);
  background: rgba(127, 29, 29, 0.32);
  color: #fca5a5;
  font-size: 12px;
  letter-spacing: 0.04em;
  cursor: pointer;
  transition:
    background 0.2s ease,
    color 0.2s ease,
    border-color 0.2s ease,
    transform 0.15s ease;
}

.profile__post-delete:hover:not(:disabled) {
  background: rgba(153, 27, 27, 0.5);
  color: #fecaca;
  border-color: rgba(248, 113, 113, 0.75);
}

.profile__post-delete:active:not(:disabled) {
  transform: scale(0.97);
}

.profile__post-delete:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.profile__panel--settings {
  align-items: center;
}

.profile__panel--settings .profile__panel-head {
  align-self: flex-start;
}

.settings {
  width: min(420px, 100%);
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 8vh;
  padding: 28px 26px;
  border-radius: 20px;
  border: 1px solid rgba(96, 165, 250, 0.2);
  background: rgba(8, 15, 32, 0.6);
  -webkit-backdrop-filter: blur(14px);
  backdrop-filter: blur(14px);
}

.settings__label {
  font-size: 13px;
  color: rgba(191, 219, 254, 0.8);
  letter-spacing: 0.04em;
}

.settings__input {
  width: 100%;
  height: 44px;
  padding: 0 14px;
  border-radius: 12px;
  border: 1px solid rgba(96, 165, 250, 0.2);
  background: rgba(10, 18, 40, 0.5);
  color: #eaf2ff;
  font-size: 14px;
  outline: none;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.settings__input::placeholder {
  color: rgba(147, 197, 253, 0.35);
}

.settings__input:focus {
  border-color: rgba(96, 165, 250, 0.7);
  box-shadow: 0 0 0 3px rgba(96, 165, 250, 0.16);
}

.settings__submit {
  margin-top: 6px;
  height: 44px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #2563eb, #1e40af);
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.06em;
  cursor: pointer;
  transition:
    filter 0.2s ease,
    transform 0.15s ease;
}

.settings__submit:hover {
  filter: brightness(1.08);
}

.settings__submit:active {
  transform: scale(0.98);
}

.settings__message {
  margin: 0;
  color: #6ee7b7;
  font-size: 13px;
  text-align: center;
}

@media (max-width: 640px) {
  .profile__info {
    width: 34%;
    padding: 16px 12px;
  }

  .profile__menu {
    width: 66%;
  }

  .profile__panel {
    padding: 84px 16px 20px;
  }

  .profile__avatar {
    width: 64px;
    height: 64px;
  }

  .profile__name {
    font-size: 18px;
  }

  .profile__stats {
    gap: 10px;
  }

  .profile__stats strong {
    font-size: 16px;
  }
}
</style>
