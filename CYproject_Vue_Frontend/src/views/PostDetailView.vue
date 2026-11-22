<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SpecularButton from '@/components/SpecularButton.vue'
import PulseHeart from '@/components/PulseHeart.vue'
import DotField from '@/components/DotField.vue'
import MarkdownView from '@/components/MarkdownView.vue'
import { Clock01Icon, ViewIcon } from '@hugeicons/core-free-icons'
import { HugeiconsIcon } from '@hugeicons/vue'
import { vAuthedSrc } from '@/utils/authImage'
import {
  createPostComment,
  getCurrentUser,
  getPost,
  getPostComments,
  setPostLiked,
} from '@/utils/communityApi'

const route = useRoute()
const router = useRouter()

// ============ 帖子详情与评论 ============
const postId = computed(() => String(route.params.id || ''))
const post = ref(null)
const comments = ref([])
const isLoading = ref(false)
const isSubmittingComment = ref(false)
const isUpdatingLike = ref(false)
const loadError = ref('')
const actionError = ref('')
const currentUserId = ref('')
let requestId = 0

const authorAvatar = computed(() => {
  return post.value?.authorAvatar || null
})

const contentSource = computed(() => {
  if (!post.value) return ''
  return post.value.content || `> ${post.value.excerpt}`
})

const commentDraft = ref('')

async function loadPost() {
  const currentRequestId = ++requestId
  isLoading.value = true
  loadError.value = ''
  actionError.value = ''
  isSubmittingComment.value = false
  isUpdatingLike.value = false
  post.value = null
  comments.value = []
  try {
    const [postResult, commentResult] = await Promise.all([
      getPost(postId.value),
      getPostComments(postId.value, { page: 1, pageSize: 50 }),
    ])
    if (currentRequestId !== requestId) return
    post.value = postResult
    comments.value = commentResult.items
  } catch (error) {
    if (currentRequestId === requestId) {
      loadError.value =
        error.response?.data?.message || error.message || '加载帖子失败，请稍后重试'
    }
  } finally {
    if (currentRequestId === requestId) isLoading.value = false
  }
}

watch(postId, loadPost, { immediate: true })

onMounted(() => {
  getCurrentUser()
    .then((user) => {
      currentUserId.value = user.id || ''
    })
    .catch(() => {})
})

async function submitComment() {
  const text = commentDraft.value.trim()
  if (!text || !post.value || isSubmittingComment.value) return
  const postIdAtSubmit = postId.value
  isSubmittingComment.value = true
  actionError.value = ''
  try {
    const comment = await createPostComment(postIdAtSubmit, text)
    if (postIdAtSubmit === postId.value) {
      comments.value.push(comment)
      commentDraft.value = ''
    }
  } catch (error) {
    if (postIdAtSubmit === postId.value) {
      actionError.value =
        error.response?.data?.message || error.message || '评论发布失败，请稍后重试'
    }
  } finally {
    if (postIdAtSubmit === postId.value) isSubmittingComment.value = false
  }
}

function onCommentKeydown(e) {
  // Enter 发送，Shift+Enter 换行
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    submitComment()
  }
}

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/forum')
  }
}

async function openUser(userid) {
  if (!userid) return
  if (!currentUserId.value) {
    try {
      const user = await getCurrentUser()
      currentUserId.value = user.id || ''
    } catch (error) {
      // 获取当前用户失败时按普通用户主页跳转
    }
  }
  if (currentUserId.value && String(currentUserId.value) === String(userid)) {
    router.push('/profile')
    return
  }
  router.push(`/user/${encodeURIComponent(userid)}`)
}

async function onLike(liked) {
  if (!post.value || isUpdatingLike.value) return
  const postIdAtLike = postId.value
  isUpdatingLike.value = true
  actionError.value = ''
  try {
    const result = await setPostLiked(postIdAtLike, liked)
    if (postIdAtLike === postId.value && post.value) {
      post.value = {
        ...post.value,
        likedByMe: result.liked,
        likes: result.likes,
      }
    }
  } catch (error) {
    if (postIdAtLike === postId.value) {
      actionError.value =
        error.response?.data?.message || error.message || '点赞操作失败，请稍后重试'
    }
  } finally {
    if (postIdAtLike === postId.value) isUpdatingLike.value = false
  }
}

function timeAgo(iso) {
  const diff = Date.now() - new Date(iso).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return '刚刚'
  if (mins < 60) return `${mins} 分钟前`
  const hours = Math.floor(mins / 60)
  if (hours < 24) return `${hours} 小时前`
  const days = Math.floor(hours / 24)
  if (days < 30) return `${days} 天前`
  return new Date(iso).toLocaleDateString('zh-CN')
}

function fullTime(iso) {
  return new Date(iso).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}
</script>

<template>
  <div class="post">
    <!-- 背景 -->
    <div class="post__bg" aria-hidden="true">
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

    <!-- 左上角返回 -->
    <div class="post__back">
      <SpecularButton
        size="sm"
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

    <!-- 右上角点赞 -->
    <div v-if="post" class="post__like">
      <PulseHeart
        :key="postId"
        :count="post.likes"
        :liked="post.likedByMe"
        :disabled="isUpdatingLike"
        show-count
        icon="heart"
        idle-outline
        liked-color="#ff4d6d"
        idle-color="#94a3b8"
        pill-color="#141c33"
        text-color="#eaf2ff"
        :size="40"
        :corner="32"
        :duration="560"
        :overshoot="1.7"
        :beat="3"
        :roll-duration="350"
        label="点赞"
        @change="onLike"
      />
    </div>

    <!-- 内容滚动区 -->
    <div v-if="isLoading" class="post__missing">
      <p class="post__missing-title">正在加载帖子…</p>
    </div>
    <div v-else-if="loadError" class="post__missing">
      <p class="post__missing-title">{{ loadError }}</p>
      <SpecularButton
        size="md"
        :radius="999"
        text-color="#eaf2ff"
        line-color="#60a5fa"
        base-color="#1e3a8a"
        :intensity="1.1"
        :auto-animate="true"
        :speed="0.5"
        @click="loadPost"
      >
        重试
      </SpecularButton>
    </div>
    <div v-else-if="post" class="post__scroll">
      <div class="post__inner">
        <!-- 帖子主体 -->
        <article class="post-card">
          <div class="post-card__head">
            <span
              class="post-card__badge"
              :class="post.category === 'work' ? 'post-card__badge--work' : 'post-card__badge--ask'"
            >
              {{ post.category === 'work' ? '作品帖' : '询问帖' }}
            </span>
            <h1 class="post-card__title">{{ post.title }}</h1>
          </div>

          <div class="post-card__meta">
            <button
              type="button"
              class="post-card__author"
              :disabled="!post.authorId"
              @click="openUser(post.authorId)"
            >
              <img
                v-if="authorAvatar"
                class="post-card__avatar"
                v-authed-src="authorAvatar"
                alt="作者头像"
              />
              <span v-else class="post-card__avatar post-card__avatar--letter">{{
                post.author.slice(0, 1)
              }}</span>
              {{ post.author }}
            </button>
            <span class="post-card__stat">
              <HugeiconsIcon :icon="Clock01Icon" :size="14" :stroke-width="2" />
              发布于 {{ fullTime(post.publishedAt) }}
            </span>
            <span class="post-card__stat">
              <HugeiconsIcon :icon="ViewIcon" :size="14" :stroke-width="2" />
              {{ post.views.toLocaleString() }} 浏览
            </span>
          </div>

          <div class="post-card__tags">
            <span v-for="tag in post.tags" :key="tag" class="post-card__tag"># {{ tag }}</span>
          </div>

          <div class="post-card__content">
            <MarkdownView :source="contentSource" />
          </div>
        </article>

        <!-- 评论区 -->
        <section class="comments">
          <h2 class="comments__title">
            评论
            <span class="comments__count">{{ comments.length }}</span>
          </h2>
          <p v-if="actionError" class="comment__error" role="alert">{{ actionError }}</p>

          <ul class="comments__list">
            <li v-for="c in comments" :key="c.id" class="comment">
              <div class="comment__avatar" aria-hidden="true">
                <img
                  v-if="c.avatar"
                  class="comment__avatar-img"
                  v-authed-src="c.avatar"
                  alt="头像"
                />
                <span v-else>{{ c.author.slice(0, 1) }}</span>
              </div>
              <div class="comment__body">
                <div class="comment__head">
                  <button
                    type="button"
                    class="comment__author"
                    :disabled="!c.authorId"
                    @click="openUser(c.authorId)"
                  >
                    {{ c.author }}
                  </button>
                  <span class="comment__time">{{ timeAgo(c.time) }}</span>
                </div>
                <p class="comment__content">{{ c.content }}</p>
              </div>
            </li>
          </ul>

          <form class="comment-composer" @submit.prevent="submitComment">
            <textarea
              v-model="commentDraft"
              class="comment-composer__input"
              rows="2"
              placeholder="写下你的评论…"
              autocomplete="off"
              @keydown="onCommentKeydown"
            ></textarea>
            <button
              class="comment-composer__submit"
              type="submit"
              :disabled="!commentDraft.trim() || isSubmittingComment"
            >
              {{ isSubmittingComment ? '发布中…' : '发布' }}
            </button>
          </form>
        </section>
      </div>
    </div>

    <!-- 帖子不存在 -->
    <div v-else class="post__missing">
      <p class="post__missing-title">帖子不存在或已删除</p>
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
  </div>
</template>

<style scoped>
.post {
  position: relative;
  width: 100%;
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
  color: #dbeafe;
  background:
    radial-gradient(900px 600px at 50% 0%, rgba(3, 64, 234, 0.1), transparent 60%), #03060d;
}

.post__bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.post__back {
  position: absolute;
  top: 22px;
  left: 22px;
  z-index: 3;
}

.post__like {
  position: absolute;
  top: 22px;
  right: 22px;
  z-index: 3;
}

.post__scroll {
  position: absolute;
  inset: 0;
  z-index: 2;
  overflow-y: auto;
  padding: 96px 20px 48px;
  display: flex;
  justify-content: center;
  scroll-behavior: smooth;
}

.post__scroll::-webkit-scrollbar {
  width: 8px;
}

.post__scroll::-webkit-scrollbar-thumb {
  background: rgba(96, 165, 250, 0.25);
  border-radius: 999px;
}

.post__inner {
  width: min(760px, 100%);
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.post-card {
  padding: 30px 30px 26px;
  border-radius: 22px;
  border: 1px solid rgba(96, 165, 250, 0.22);
  background: rgba(8, 15, 32, 0.66);
  -webkit-backdrop-filter: blur(16px);
  backdrop-filter: blur(16px);
  box-shadow:
    0 24px 60px rgba(0, 0, 0, 0.45),
    inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.post-card__head {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.post-card__badge {
  flex: none;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12px;
  letter-spacing: 0.04em;
}

.post-card__badge--work {
  background: rgba(37, 99, 235, 0.22);
  color: #93c5fd;
  border: 1px solid rgba(96, 165, 250, 0.4);
}

.post-card__badge--ask {
  background: rgba(16, 185, 129, 0.16);
  color: #6ee7b7;
  border: 1px solid rgba(52, 211, 153, 0.4);
}

.post-card__title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  line-height: 1.4;
  letter-spacing: 0.01em;
  color: #eaf2ff;
}

.post-card__meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 18px;
  margin-top: 16px;
  font-size: 13px;
  color: rgba(147, 197, 253, 0.65);
}

.post-card__author {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0;
  border: none;
  background: transparent;
  font-family: inherit;
  font-size: inherit;
  color: #60a5fa;
  font-weight: 500;
  cursor: pointer;
  transition: color 0.2s ease;
}

.post-card__author:hover:not(:disabled) {
  color: #93c5fd;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.post-card__author:disabled {
  cursor: default;
}

.post-card__avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid rgba(96, 165, 250, 0.5);
}

.post-card__avatar--letter {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  color: #eaf2ff;
  background: linear-gradient(135deg, #2563eb, #1e40af);
}

.post-card__stat {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.post-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 18px;
}

.post-card__tag {
  padding: 5px 14px;
  border-radius: 999px;
  font-size: 12px;
  color: #bfdbfe;
  background: rgba(37, 99, 235, 0.14);
  border: 1px solid rgba(96, 165, 250, 0.28);
}

.post-card__content {
  margin: 20px 0 0;
  font-size: 15px;
  line-height: 1.9;
  word-break: break-word;
  color: #dbeafe;
}

.post-card__content :deep(p) {
  margin: 0 0 14px;
}

.post-card__content :deep(p:last-child) {
  margin-bottom: 0;
}

.post-card__content :deep(a) {
  color: #60a5fa;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.post-card__content :deep(a:hover) {
  color: #93c5fd;
}

.post-card__content :deep(img) {
  max-width: 100%;
  border-radius: 10px;
  margin: 6px 0;
  display: block;
}

.post-card__content :deep(h1),
.post-card__content :deep(h2),
.post-card__content :deep(h3),
.post-card__content :deep(h4) {
  margin: 18px 0 10px;
  line-height: 1.4;
  color: #eaf2ff;
}

.post-card__content :deep(code) {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 0.9em;
  background: rgba(37, 99, 235, 0.16);
  color: #bfdbfe;
  padding: 2px 6px;
  border-radius: 6px;
}

.post-card__content :deep(pre) {
  padding: 12px 14px;
  border-radius: 10px;
  background: rgba(4, 9, 20, 0.8);
  border: 1px solid rgba(96, 165, 250, 0.16);
  overflow-x: auto;
  margin: 0 0 14px;
}

.post-card__content :deep(pre code) {
  background: transparent;
  padding: 0;
  color: #dbeafe;
}

.post-card__content :deep(blockquote) {
  margin: 0 0 14px;
  padding: 6px 14px;
  border-left: 3px solid #2563eb;
  background: rgba(37, 99, 235, 0.1);
  color: rgba(191, 219, 254, 0.85);
}

.post-card__content :deep(ul),
.post-card__content :deep(ol) {
  margin: 0 0 14px;
  padding-left: 22px;
}

.comments {
  padding: 22px 24px 26px;
  border-radius: 22px;
  border: 1px solid rgba(96, 165, 250, 0.18);
  background: rgba(8, 15, 32, 0.5);
  -webkit-backdrop-filter: blur(14px);
  backdrop-filter: blur(14px);
}

.comments__title {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin: 0 0 18px;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 0.02em;
  color: #eaf2ff;
}

.comments__count {
  font-size: 13px;
  font-weight: 500;
  color: #60a5fa;
}

.comments__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.comment {
  display: flex;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(96, 165, 250, 0.12);
}

.comment:last-child {
  padding-bottom: 0;
  border-bottom: none;
}

.comment__avatar {
  flex: none;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  font-size: 14px;
  font-weight: 600;
  color: #eaf2ff;
  background: linear-gradient(135deg, #2563eb, #1e40af);
  border: 1px solid rgba(96, 165, 250, 0.4);
}

.comment__avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.comment__body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.comment__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.comment__author {
  padding: 0;
  border: none;
  background: transparent;
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
  color: #60a5fa;
  cursor: pointer;
  transition: color 0.2s ease;
}

.comment__author:hover:not(:disabled) {
  color: #93c5fd;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.comment__author:disabled {
  cursor: default;
}

.comment__time {
  font-size: 12px;
  color: rgba(147, 197, 253, 0.5);
}

.comment__content {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
  color: #dbeafe;
}

.comment-composer {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  margin-top: 18px;
}

.comment-composer__input {
  flex: 1;
  min-height: 64px;
  resize: vertical;
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid rgba(96, 165, 250, 0.2);
  background: rgba(10, 18, 40, 0.5);
  color: #eaf2ff;
  font-size: 14px;
  line-height: 1.5;
  font-family: inherit;
  outline: none;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.comment-composer__input::placeholder {
  color: rgba(147, 197, 253, 0.35);
}

.comment-composer__input:focus {
  border-color: rgba(96, 165, 250, 0.7);
  box-shadow: 0 0 0 3px rgba(96, 165, 250, 0.16);
}

.comment-composer__submit {
  flex: none;
  height: 64px;
  padding: 0 22px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #2563eb, #1e40af);
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.04em;
  cursor: pointer;
  transition:
    filter 0.2s ease,
    transform 0.15s ease;
}

.comment-composer__submit:hover {
  filter: brightness(1.08);
}

.comment-composer__submit:active {
  transform: scale(0.98);
}

.comment-composer__submit:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.post__missing {
  position: absolute;
  inset: 0;
  z-index: 4;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 18px;
}

.post__missing-title {
  margin: 0;
  font-size: 18px;
  color: rgba(191, 219, 254, 0.85);
}

@media (max-width: 640px) {
  .post__back {
    top: 12px;
    left: 12px;
  }

  .post__like {
    top: 12px;
    right: 12px;
  }

  .post__scroll {
    padding: 92px 12px 32px;
  }

  .post-card {
    padding: 22px 20px 20px;
  }

  .post-card__title {
    font-size: 20px;
  }
}
</style>
