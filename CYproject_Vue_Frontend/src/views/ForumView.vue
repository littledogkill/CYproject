<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import RetatingText from '@/components/RetatingText.vue'
import SlingButton from '@/components/SlingButton.vue'
import AnimatedList from '@/components/AnimatedList.vue'
import SpecularButton from '@/components/SpecularButton.vue'
import DotField from '@/components/DotField.vue'
import PostCard from '@/components/PostCard.vue'
import { SearchIcon } from '@hugeicons/core-free-icons'
import { HugeiconsIcon } from '@hugeicons/vue'
import { getCurrentUser, getPosts } from '@/utils/communityApi'

const router = useRouter()

// ============ 标题（占位，之后自行修改） ============
const titleTexts = ['作品展示', '技术问答', '灵感碰撞', 'AI 社区']

// ============ 主标签（帖子分类） ============
const mainTags = [
  { label: '全部', value: 'all' },
  { label: '作品帖', value: 'work' },
  { label: '询问帖', value: 'ask' },
]

// ============ 副标签（排序方式） ============
const sortOptions = [
  { label: '按浏览数量', value: 'views' },
  { label: '按点赞数量', value: 'likes' },
  { label: '按发布时间', value: 'time' },
]

// ============ 搜索状态 ============
const keyword = ref('')
const appliedKeyword = ref('')
const mainTag = ref('all')
const sortBy = ref('time')
const posts = ref([])
const isLoading = ref(false)
const loadError = ref('')
const currentUserId = ref('')
const currentUsername = ref('')
let requestId = 0

function applySearch() {
  appliedKeyword.value = keyword.value
}

async function loadPosts() {
  const currentRequestId = ++requestId
  isLoading.value = true
  loadError.value = ''
  try {
    const result = await getPosts({ page: 1, pageSize: 1000 })
    if (currentRequestId === requestId) posts.value = result.items
  } catch (error) {
    if (currentRequestId === requestId) {
      posts.value = []
      loadError.value =
        error.response?.data?.message || error.message || '加载帖子失败，请稍后重试'
    }
  } finally {
    if (currentRequestId === requestId) isLoading.value = false
  }
}

onMounted(() => {
  loadPosts()
  getCurrentUser()
    .then((user) => {
      currentUserId.value = user.id || ''
      currentUsername.value = user.username || ''
    })
    .catch(() => {})
})

const filteredPosts = computed(() => {
  const keyword = appliedKeyword.value.trim().toLowerCase()
  let list = posts.value

  if (mainTag.value !== 'all') {
    list = list.filter((post) => post.category === mainTag.value)
  }

  if (keyword) {
    list = list.filter((post) => {
      const haystack = [
        post.title,
        post.excerpt,
        post.author,
        post.authorUsername,
        ...(post.tags || []),
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase()
      return haystack.includes(keyword)
    })
  }

  return [...list].sort((a, b) => {
    if (sortBy.value === 'views') return (b.views || 0) - (a.views || 0)
    if (sortBy.value === 'likes') return (b.likes || 0) - (a.likes || 0)
    return new Date(b.publishedAt || 0).getTime() - new Date(a.publishedAt || 0).getTime()
  })
})

const forumStats = computed(() => {
  const list = posts.value
  return {
    total: list.length,
    works: list.filter((post) => post.category === 'work').length,
    asks: list.filter((post) => post.category === 'ask').length,
    views: list.reduce((sum, post) => sum + (Number(post.views) || 0), 0),
    likes: list.reduce((sum, post) => sum + (Number(post.likes) || 0), 0),
  }
})

function goHome() {
  router.push('/')
}

function goMain() {
  // TODO: 与「返回首页」区分，后续按需求调整目标路由
  router.push('/profile')
}

function goCreate() {
  router.push('/post/new')
}

function openPost(item) {
  router.push(`/post/${item.id}`)
}

async function openUser(item) {
  const id = item.authorId || item.authorUsername
  if (!id) return

  if (!currentUserId.value && !currentUsername.value) {
    try {
      const user = await getCurrentUser()
      currentUserId.value = user.id || ''
      currentUsername.value = user.username || ''
    } catch (error) {
      // 获取当前用户失败时按普通用户主页跳转，避免阻塞点击
    }
  }

  const isSelf =
    (currentUserId.value && item.authorId && String(currentUserId.value) === String(item.authorId)) ||
    (currentUsername.value &&
      item.authorUsername &&
      currentUsername.value === item.authorUsername)
  if (isSelf) {
    router.push('/profile')
    return
  }
  router.push(`/user/${encodeURIComponent(id)}`)
}
</script>

<template>
  <div class="forum">
    <!-- 背景 -->
    <div class="forum__bg" aria-hidden="true">
      <DotField
        :dot-radius="3.5"
        :dot-spacing="16"
        :bulge-strength="70"
        :glow-radius="180"
        :sparkle="false"
        :wave-amplitude="0"
        :cursor-radius="420"
        :cursor-force="0.12"
        bulge-only
        gradient-from="rgba(96, 165, 250, 0.4)"
        gradient-to="rgba(37, 99, 235, 0.22)"
        glow-color="#04060d"
      />
    </div>

    <!-- 右侧社区概览 -->
    <aside class="forum__aside" aria-label="社区概览">
      <h2 class="forum__aside-title">社区概览</h2>
      <ul class="forum__aside-stats">
        <li>
          <span>帖子总数</span>
          <strong>{{ forumStats.total }}</strong>
        </li>
        <li>
          <span>作品帖</span>
          <strong>{{ forumStats.works }}</strong>
        </li>
        <li>
          <span>询问帖</span>
          <strong>{{ forumStats.asks }}</strong>
        </li>
        <li>
          <span>总浏览</span>
          <strong>{{ forumStats.views }}</strong>
        </li>
        <li>
          <span>总点赞</span>
          <strong>{{ forumStats.likes }}</strong>
        </li>
      </ul>
    </aside>

    <!-- 左上角：两个 SpecularButton -->
    <div class="forum__topbar">
      <SpecularButton
        size="sm"
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
      <SpecularButton
        size="sm"
        :radius="999"
        text-color="#eaf2ff"
        line-color="#60a5fa"
        base-color="#1e3a8a"
        :intensity="1.1"
        :auto-animate="true"
        :speed="0.5"
        @click="goMain"
      >
        返回主页
      </SpecularButton>
      <SpecularButton
        size="sm"
        :radius="999"
        text-color="#eaf2ff"
        line-color="#93c5fd"
        base-color="#1e3a8a"
        :intensity="1.2"
        :auto-animate="true"
        :speed="0.5"
        @click="goCreate"
      >
        发帖
      </SpecularButton>
    </div>

    <div class="forum__body">
      <!-- 顶部标题 -->
      <header class="forum__title">
      <RetatingText
        :texts="titleTexts"
        :rotation-interval="2200"
        :stagger-duration="0.03"
        stagger-from="center"
        :transition="{ type: 'spring', damping: 26, stiffness: 320 }"
        main-class-name="justify-center"
        split-level-class-name="overflow-hidden pb-1"
        element-level-class-name="text-[clamp(1.3rem,3vw,2.2rem)] font-bold tracking-tight text-[#eaf2ff]"
      />
    </header>

    <!-- 搜索 + 标签 -->
    <section class="forum__controls">
      <div class="forum__search">
        <div class="forum__search-input">
          <HugeiconsIcon
            :icon="SearchIcon"
            :size="18"
            :stroke-width="2"
            class="forum__search-icon"
          />
          <input
            v-model="keyword"
            type="text"
            placeholder="搜索你感兴趣的东西…"
            autocomplete="off"
            @keyup.enter="applySearch"
          />
        </div>
        <SlingButton
          :size="52"
          pad-color="#0f1c38"
          icon-color="#bfdbfe"
          accent-color="#60a5fa"
          well-color="#1e3a8a"
          band-color="#3b82f6"
          aria-label="搜索"
          @send="applySearch"
        >
          <HugeiconsIcon :icon="SearchIcon" :size="20" :stroke-width="2.2" />
        </SlingButton>
      </div>

      <div class="forum__filters">
        <div class="forum__filter-row">
          <span class="forum__filter-label">主标签</span>
          <div class="forum__chips">
            <button
              v-for="t in mainTags"
              :key="t.value"
              type="button"
              class="forum__chip"
              :class="{ 'forum__chip--active': mainTag === t.value }"
              @click="mainTag = t.value"
            >
              {{ t.label }}
            </button>
          </div>

          <span class="forum__filter-label forum__filter-label--sub">副标签</span>
          <div class="forum__chips">
            <button
              v-for="s in sortOptions"
              :key="s.value"
              type="button"
              class="forum__chip"
              :class="{ 'forum__chip--active': sortBy === s.value }"
              @click="sortBy = s.value"
            >
              {{ s.label }}
            </button>
          </div>
        </div>
      </div>
    </section>

    <!-- 帖子列表 -->
    <main class="forum__list">
      <p v-if="isLoading" class="forum__message" role="status">正在加载帖子…</p>
      <p v-else-if="loadError" class="forum__message forum__message--error" role="alert">
        {{ loadError }}
      </p>
      <p v-else-if="!filteredPosts.length" class="forum__message">没有找到帖子</p>
      <AnimatedList
        v-else
        :items="filteredPosts"
        :max-height="'100%'"
        :show-gradients="true"
        :enable-arrow-navigation="true"
        :display-scrollbar="true"
        @item-selected="openPost"
      >
        <template #item="{ item }">
          <PostCard :post="item" @open-user="openUser" />
        </template>
      </AnimatedList>
    </main>
    </div>
  </div>
</template>

<style scoped>
.forum {
  position: relative;
  width: 100%;
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 14px 20px 16px;
  color: #dbeafe;
  background:
    radial-gradient(900px 600px at 50% 0%, rgba(3, 64, 234, 0.1), transparent 60%), #03060d;
}

.forum__bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.forum__aside {
  position: absolute;
  top: 50%;
  right: 24px;
  transform: translateY(-50%);
  z-index: 3;
  width: 210px;
  padding: 18px 16px;
  border-radius: 18px;
  border: 1px solid rgba(96, 165, 250, 0.2);
  background: rgba(8, 15, 32, 0.58);
  -webkit-backdrop-filter: blur(14px);
  backdrop-filter: blur(14px);
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.35);
  pointer-events: none;
}

.forum__aside-title {
  margin: 0 0 14px;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: #eaf2ff;
  text-align: center;
}

.forum__aside-stats {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin: 0;
  padding: 0;
}

.forum__aside-stats li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(96, 165, 250, 0.12);
}

.forum__aside-stats li:last-child {
  padding-bottom: 0;
  border-bottom: none;
}

.forum__aside-stats span {
  font-size: 12px;
  color: rgba(147, 197, 253, 0.62);
}

.forum__aside-stats strong {
  font-size: 16px;
  font-weight: 700;
  color: #93c5fd;
}

.forum__topbar {
  position: absolute;
  top: 14px;
  left: 14px;
  z-index: 5;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.forum__body {
  position: relative;
  z-index: 2;
  flex: 1;
  min-height: 0;
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  transform: translateX(calc(-100vw / 15));
}

.forum__title {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 0;
  text-align: center;
}

.forum__controls {
  position: relative;
  z-index: 2;
  width: min(760px, 100%);
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.forum__search {
  display: flex;
  align-items: center;
  gap: 14px;
}

.forum__search-input {
  position: relative;
  flex: 1;
  display: flex;
  align-items: center;
}

.forum__search-icon {
  position: absolute;
  left: 14px;
  color: rgba(147, 197, 253, 0.5);
  pointer-events: none;
}

.forum__search-input input {
  width: 100%;
  height: 44px;
  padding: 0 16px 0 44px;
  border-radius: 14px;
  border: 1px solid rgba(96, 165, 250, 0.22);
  background: rgba(10, 18, 40, 0.6);
  color: #eaf2ff;
  font-size: 14px;
  outline: none;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.forum__search-input input::placeholder {
  color: rgba(147, 197, 253, 0.35);
}

.forum__search-input input:focus {
  border-color: rgba(96, 165, 250, 0.7);
  box-shadow: 0 0 0 3px rgba(96, 165, 250, 0.16);
}

.forum__filters {
  display: block;
}

.forum__filter-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.forum__filter-label {
  flex: none;
  font-size: 13px;
  letter-spacing: 0.06em;
  color: rgba(147, 197, 253, 0.6);
}

.forum__filter-label--sub {
  margin-left: 8px;
}

.forum__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.forum__chip {
  padding: 7px 16px;
  border-radius: 999px;
  border: 1px solid rgba(96, 165, 250, 0.22);
  background: rgba(10, 18, 40, 0.5);
  color: rgba(191, 219, 254, 0.8);
  font-size: 13px;
  cursor: pointer;
  transition:
    background 0.2s ease,
    color 0.2s ease,
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.forum__chip:hover {
  border-color: rgba(96, 165, 250, 0.55);
  color: #eaf2ff;
}

.forum__chip--active {
  background: linear-gradient(135deg, #2563eb, #1e40af);
  border-color: rgba(96, 165, 250, 0.8);
  color: #ffffff;
  box-shadow: 0 0 16px rgba(37, 99, 235, 0.4);
}

.forum__list {
  position: relative;
  z-index: 2;
  flex: 1;
  min-height: 0;
  width: min(820px, 100%);
  margin-top: 8px;
}

.forum__message {
  margin: 24px 0;
  color: rgba(191, 219, 254, 0.72);
  text-align: center;
}

.forum__message--error {
  color: #fca5a5;
}

@media (max-width: 1180px) {
  .forum__aside {
    display: none;
  }
}

@media (max-width: 640px) {
  .forum {
    padding: 12px 12px 12px;
  }

  .forum__topbar {
    top: 12px;
    left: 12px;
    gap: 6px;
  }

  .forum__body {
    transform: none;
  }

  .forum__filter-row {
    align-items: flex-start;
    flex-direction: column;
    gap: 6px;
  }
}
</style>
