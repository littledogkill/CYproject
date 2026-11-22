<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SideRays from '@/components/SideRays.vue'
import SpecularButton from '@/components/SpecularButton.vue'
import AnimatedList from '@/components/AnimatedList.vue'
import PostCard from '@/components/PostCard.vue'
import { getUser, getUserPosts, setFollowing } from '@/utils/communityApi'
import { vAuthedSrc } from '@/utils/authImage'

const route = useRoute()
const router = useRouter()

const userid = computed(() => {
  const u = route.params.userid
  return Array.isArray(u) ? u[0] || '' : u || ''
})

const user = ref(null)
const posts = ref([])
const isLoading = ref(true)
const isSavingFollow = ref(false)
const pageError = ref('')
let requestId = 0
const stats = computed(
  () => user.value?.stats || { posts: 0, likes: 0, followers: 0 },
)
const avatarInitial = computed(() => (user.value?.name || '?').slice(0, 1))
const isFollowed = computed(() => Boolean(user.value?.isFollowing))

// 作品帖 / 询问帖切换（与论坛主标签保持一致）
const tabs = [
  { label: '全部', value: 'all' },
  { label: '作品帖', value: 'work' },
  { label: '询问帖', value: 'ask' },
]
const activeTab = ref('all')

const userPosts = computed(() =>
  activeTab.value === 'all'
    ? posts.value
    : posts.value.filter((post) => post.category === activeTab.value),
)

async function loadUser() {
  const currentRequestId = ++requestId
  isLoading.value = true
  pageError.value = ''
  user.value = null
  posts.value = []
  try {
    const [userResult, postsResult] = await Promise.all([
      getUser(userid.value),
      getUserPosts(userid.value, { page: 1, pageSize: 1000 }),
    ])
    if (currentRequestId !== requestId) return
    user.value = userResult
    posts.value = postsResult.items
  } catch (error) {
    if (currentRequestId === requestId) {
      pageError.value =
        error.response?.data?.message || error.message || '加载用户主页失败，请稍后重试'
    }
  } finally {
    if (currentRequestId === requestId) isLoading.value = false
  }
}

watch(userid, loadUser, { immediate: true })

function goBack() {
  if (window.history.state?.back) {
    router.back()
  } else {
    router.push('/forum')
  }
}

function openPost(item) {
  router.push(`/post/${item.id}`)
}

function openUser(item) {
  router.push(`/user/${encodeURIComponent(item.authorId || item.authorUsername)}`)
}

function openChat() {
  router.push(`/chat/${encodeURIComponent(userid.value)}`)
}

async function handleFollow() {
  if (isSavingFollow.value || !user.value) return
  isSavingFollow.value = true
  pageError.value = ''
  try {
    const result = await setFollowing(userid.value, !isFollowed.value)
    user.value = {
      ...user.value,
      isFollowing: result.following,
      stats: { ...user.value.stats, followers: result.followers },
    }
  } catch (error) {
    pageError.value =
      error.response?.data?.message || error.message || '关注操作失败，请稍后重试'
  } finally {
    isSavingFollow.value = false
  }
}
</script>

<template>
  <div class="user-profile">
    <!-- SideRays 背景，与 profile 保持一致 -->
    <div class="user-profile__bg" aria-hidden="true">
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

    <!-- 左上角返回 -->
    <div class="user-profile__back">
      <SpecularButton
        size="md"
        :radius="999"
        text-color="#eaf2ff"
        line-color="#60a5fa"
        base-color="#1e3a8a"
        :intensity="1.1"
        :auto-animate="true"
        :speed="0.5"
        @click="goBack"
      >
        返回
      </SpecularButton>
    </div>

    <!-- 页面上方：在线状态 -->
    <div v-if="user" class="user-profile__status" :class="user.online ? 'is-online' : 'is-offline'">
      <span class="user-profile__status-dot"></span>
      {{ user.online ? '在线' : '离线' }}
    </div>
    <p v-if="pageError && user" class="user-profile__message" role="alert">{{ pageError }}</p>

    <!-- 右上角：发私信 + 关注 -->
    <div v-if="user" class="user-profile__actions">
      <SpecularButton
        size="md"
        :radius="999"
        text-color="#eaf2ff"
        line-color="#93c5fd"
        base-color="#1e3a8a"
        :intensity="1.2"
        :auto-animate="true"
        :speed="0.5"
        @click="openChat"
      >
        发私信
      </SpecularButton>
      <SpecularButton
        size="md"
        :radius="999"
        text-color="#eaf2ff"
        line-color="#60a5fa"
        base-color="#1e3a8a"
        :intensity="1.1"
        :auto-animate="true"
        :speed="0.5"
        :disabled="isSavingFollow"
        @click="handleFollow"
      >
        {{ isSavingFollow ? '处理中…' : isFollowed ? '取消关注' : '关注' }}
      </SpecularButton>
    </div>

    <template v-if="user">
      <!-- 左侧 1/5：用户信息 -->
      <aside class="user-profile__info">
        <img
          v-if="user.avatar"
          class="user-profile__avatar"
          v-authed-src="user.avatar"
          alt="头像"
        />
        <div v-else class="user-profile__avatar user-profile__avatar--letter">
          {{ avatarInitial }}
        </div>

        <h1 class="user-profile__name">{{ user.name }}</h1>

        <ul class="user-profile__stats">
          <li>
            <strong>{{ stats.posts }}</strong>
            <span>作品数</span>
          </li>
          <li>
            <strong>{{ stats.likes }}</strong>
            <span>点赞数</span>
          </li>
          <li>
            <strong>{{ stats.followers }}</strong>
            <span>粉丝</span>
          </li>
        </ul>
      </aside>

      <!-- 右侧 4/5：TA 的帖子（复用论坛展示组件） -->
      <div class="user-profile__content">
        <div class="user-profile__tabs">
          <button
            v-for="t in tabs"
            :key="t.value"
            type="button"
            class="user-profile__tab"
            :class="{ 'user-profile__tab--active': activeTab === t.value }"
            @click="activeTab = t.value"
          >
            {{ t.label }}
          </button>
        </div>

        <main class="user-profile__list">
          <AnimatedList
            :items="userPosts"
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

          <p v-if="!userPosts.length" class="user-profile__empty">该分类下暂无帖子</p>
        </main>
      </div>
    </template>

    <!-- 用户不存在 -->
    <div v-else class="user-profile__missing">
      <p class="user-profile__missing-title">
        {{ isLoading ? '正在加载用户资料…' : pageError || '用户不存在或已注销' }}
      </p>
      <SpecularButton
        size="md"
        :radius="999"
        text-color="#eaf2ff"
        line-color="#60a5fa"
        base-color="#1e3a8a"
        :intensity="1.1"
        :auto-animate="true"
        :speed="0.5"
        @click="goBack"
      >
        返回社区
      </SpecularButton>
    </div>
  </div>
</template>

<style scoped>
.user-profile {
  position: relative;
  width: 100%;
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
  background: #05080f;
}

.user-profile__bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.user-profile__back {
  position: absolute;
  top: 22px;
  left: 22px;
  z-index: 5;
}

.user-profile__actions {
  position: absolute;
  top: 22px;
  right: 22px;
  z-index: 5;
  display: flex;
  gap: 12px;
}

.user-profile__status {
  position: absolute;
  top: 30px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 5;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 18px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.04em;
  border: 1px solid rgba(96, 165, 250, 0.24);
  background: rgba(8, 15, 32, 0.72);
  -webkit-backdrop-filter: blur(10px);
  backdrop-filter: blur(10px);
}

.user-profile__message {
  position: absolute;
  top: 76px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 6;
  margin: 0;
  color: #fca5a5;
  text-align: center;
}

.user-profile__status-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  flex: none;
}

.is-online {
  color: #6ee7b7;
}

.is-online .user-profile__status-dot {
  background: #34d399;
  box-shadow: 0 0 12px rgba(52, 211, 153, 0.85);
}

.is-offline {
  color: rgba(148, 163, 184, 0.85);
}

.is-offline .user-profile__status-dot {
  background: #64748b;
  box-shadow: 0 0 8px rgba(100, 116, 139, 0.5);
}

.user-profile__info {
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
  gap: 8px;
  padding: 24px 20px;
  border-right: 1px solid rgba(96, 165, 250, 0.14);
  background: linear-gradient(90deg, rgba(4, 9, 20, 0.72), rgba(4, 9, 20, 0.2));
  -webkit-backdrop-filter: blur(10px);
  backdrop-filter: blur(10px);
}

.user-profile__avatar {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid rgba(96, 165, 250, 0.5);
  box-shadow: 0 0 26px rgba(37, 99, 235, 0.45);
}

.user-profile__avatar--letter {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 34px;
  font-weight: 700;
  color: #eaf2ff;
  background: linear-gradient(135deg, #2563eb, #1e40af);
}

.user-profile__name {
  margin: 6px 0 0;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 0.01em;
  color: #eaf2ff;
}

.user-profile__stats {
  list-style: none;
  display: flex;
  gap: 18px;
  margin: 18px 0 0;
  padding: 0;
}

.user-profile__stats li {
  text-align: center;
}

.user-profile__stats strong {
  display: block;
  font-size: 20px;
  font-weight: 700;
  color: #eaf2ff;
}

.user-profile__stats span {
  display: block;
  margin-top: 2px;
  font-size: 12px;
  color: rgba(147, 197, 253, 0.55);
}

.user-profile__content {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 80%;
  z-index: 2;
  display: flex;
  flex-direction: column;
  padding: 84px 44px 32px;
}

.user-profile__tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
  margin-bottom: 8px;
}

.user-profile__tab {
  padding: 8px 20px;
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

.user-profile__tab:hover {
  border-color: rgba(96, 165, 250, 0.55);
  color: #eaf2ff;
}

.user-profile__tab--active {
  background: linear-gradient(135deg, #2563eb, #1e40af);
  border-color: rgba(96, 165, 250, 0.8);
  color: #ffffff;
  box-shadow: 0 0 16px rgba(37, 99, 235, 0.4);
}

.user-profile__list {
  position: relative;
  flex: 1;
  min-height: 0;
}

.user-profile__empty {
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

.user-profile__missing {
  position: absolute;
  inset: 0;
  z-index: 4;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 18px;
}

.user-profile__missing-title {
  margin: 0;
  font-size: 18px;
  color: rgba(191, 219, 254, 0.85);
}

@media (max-width: 640px) {
  .user-profile__info {
    width: 34%;
    padding: 16px 12px;
  }

  .user-profile__content {
    width: 66%;
    padding: 84px 16px 20px;
  }

  .user-profile__avatar {
    width: 64px;
    height: 64px;
  }

  .user-profile__avatar--letter {
    font-size: 26px;
  }

  .user-profile__name {
    font-size: 18px;
  }

  .user-profile__stats {
    gap: 10px;
  }

  .user-profile__stats strong {
    font-size: 16px;
  }

  .user-profile__actions {
    top: 12px;
    right: 12px;
    gap: 8px;
  }

  .user-profile__back {
    top: 12px;
    left: 12px;
  }
}
</style>
