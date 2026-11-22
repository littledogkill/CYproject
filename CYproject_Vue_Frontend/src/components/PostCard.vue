<script setup>
import { ViewIcon, HeartIcon, Clock01Icon } from '@hugeicons/core-free-icons'
import { HugeiconsIcon } from '@hugeicons/vue'
import { vAuthedSrc } from '@/utils/authImage'

defineProps({
  post: { type: Object, required: true },
})

defineEmits(['open-user'])

function formatCount(n) {
  if (n >= 10000) return (n / 10000).toFixed(1).replace(/\.0$/, '') + ' 万'
  if (n >= 1000) return (n / 1000).toFixed(1).replace(/\.0$/, '') + ' k'
  return String(n)
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
</script>

<template>
  <article class="post">
    <div class="post__head">
      <span
        class="post__badge"
        :class="post.category === 'work' ? 'post__badge--work' : 'post__badge--ask'"
      >
        {{ post.category === 'work' ? '作品帖' : '询问帖' }}
      </span>
      <h3 class="post__title">{{ post.title }}</h3>
    </div>

    <div class="post__meta">
      <button type="button" class="post__author" @click.stop="$emit('open-user', post)">
        <img
          v-if="post.authorAvatar"
          v-authed-src="post.authorAvatar"
          class="post__avatar"
          alt="作者头像"
        />
        <span v-else class="post__avatar post__avatar--letter">{{
          post.author.slice(0, 1)
        }}</span>
        {{ post.author }}
      </button>
      <span class="post__stat">
        <HugeiconsIcon :icon="ViewIcon" :size="14" :stroke-width="2" />
        {{ formatCount(post.views) }}
      </span>
      <span class="post__stat">
        <HugeiconsIcon :icon="HeartIcon" :size="14" :stroke-width="2" />
        {{ formatCount(post.likes) }}
      </span>
      <span class="post__time">
        <HugeiconsIcon :icon="Clock01Icon" :size="14" :stroke-width="2" />
        {{ timeAgo(post.publishedAt) }}
      </span>
    </div>
  </article>
</template>

<style scoped>
.post {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.post__head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.post__badge {
  flex: none;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 11px;
  letter-spacing: 0.04em;
}

.post__badge--work {
  background: rgba(37, 99, 235, 0.22);
  color: #93c5fd;
  border: 1px solid rgba(96, 165, 250, 0.4);
}

.post__badge--ask {
  background: rgba(16, 185, 129, 0.16);
  color: #6ee7b7;
  border: 1px solid rgba(52, 211, 153, 0.4);
}

.post__title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #eaf2ff;
  letter-spacing: 0.01em;
}

.post__meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 12px;
  color: rgba(147, 197, 253, 0.6);
}

.post__author {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 0;
  border: 0;
  background: transparent;
  font-size: inherit;
  font-family: inherit;
  color: #60a5fa;
  cursor: pointer;
  transition: color 0.2s ease;
}

.post__avatar {
  flex: none;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid rgba(96, 165, 250, 0.4);
}

.post__avatar--letter {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
  color: #eaf2ff;
  background: linear-gradient(135deg, #2563eb, #1e40af);
}

.post__author:hover {
  color: #93c5fd;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.post__stat,
.post__time {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}
</style>
