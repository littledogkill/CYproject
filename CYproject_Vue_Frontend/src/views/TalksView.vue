<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import SpecularButton from '@/components/SpecularButton.vue'
import AnimatedList from '@/components/AnimatedList.vue'
import DotField from '@/components/DotField.vue'
import { getConversations, getCurrentUser } from '@/utils/communityApi'
import { vAuthedSrc } from '@/utils/authImage'
import { hasUnreadConversation } from '@/utils/conversationReadState'

const router = useRouter()

const rooms = ref([])
const isLoading = ref(true)
const loadError = ref('')

onMounted(async () => {
  try {
    const [currentUser, result] = await Promise.all([
      getCurrentUser(),
      getConversations({ page: 1, pageSize: 500 }),
    ])
    const ownerId = currentUser.id || currentUser.username
    rooms.value = result.items.map((room) => ({
      ...room,
      hasNew: hasUnreadConversation(room, ownerId),
    }))
  } catch (error) {
    loadError.value =
      error.response?.data?.message || error.message || '加载私聊列表失败，请稍后重试'
  } finally {
    isLoading.value = false
  }
})

function goProfile() {
  router.push('/profile')
}

function openRoom(room) {
  router.push(`/chat/${encodeURIComponent(room.id || room.username)}`)
}

function timeAgo(iso) {
  if (!iso) return ''
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
  <div class="talks">
    <!-- 背景 -->
    <div class="talks__bg" aria-hidden="true">
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
    <div class="talks__back">
      <SpecularButton
        size="md"
        :radius="999"
        text-color="#eaf2ff"
        line-color="#60a5fa"
        base-color="#1e3a8a"
        :intensity="1.1"
        :auto-animate="true"
        :speed="0.5"
        @click="goProfile"
      >
        返回主页
      </SpecularButton>
    </div>

    <!-- 中央房间列表 -->
    <div class="talks__center">
      <section class="talks__panel">
        <header class="talks__header">
          <h1 class="talks__title">我的私聊</h1>
          <span class="talks__count">{{ rooms.length }} 个会话</span>
        </header>

        <main class="talks__list">
          <p v-if="isLoading" class="talks__empty" role="status">正在加载私聊…</p>
          <p v-else-if="loadError" class="talks__empty talks__empty--error" role="alert">
            {{ loadError }}
          </p>
          <AnimatedList
            v-else-if="rooms.length"
            :items="rooms"
            :max-height="'100%'"
            :show-gradients="true"
            :enable-arrow-navigation="true"
            :display-scrollbar="true"
            @item-selected="openRoom"
          >
            <template #item="{ item }">
              <div class="room">
                <div class="room__avatar-wrap">
                  <img
                    v-if="item.avatar"
                    class="room__avatar"
                    v-authed-src="item.avatar"
                    alt="头像"
                  />
                  <div v-else class="room__avatar room__avatar--letter">
                    {{ item.name.slice(0, 1) }}
                  </div>
                  <span
                    class="room__online"
                    :class="item.online ? 'is-online' : 'is-offline'"
                    aria-hidden="true"
                  ></span>
                </div>

                <div class="room__body">
                  <div class="room__top">
                    <span class="room__name">
                      {{ item.name }}
                      <span
                        v-if="item.hasNew"
                        class="room__unread-dot"
                        aria-label="有新消息"
                      ></span>
                    </span>
                    <span class="room__time">{{ timeAgo(item.lastTime) }}</span>
                  </div>
                  <p class="room__preview">{{ item.lastMessage || '暂无消息' }}</p>
                </div>
              </div>
            </template>
          </AnimatedList>

          <p v-else class="talks__empty">暂时没有人发来消息</p>
        </main>
      </section>
    </div>
  </div>
</template>

<style scoped>
.talks {
  position: relative;
  width: 100%;
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
  color: #dbeafe;
  background:
    radial-gradient(900px 600px at 50% 0%, rgba(3, 64, 234, 0.1), transparent 60%), #03060d;
}

.talks__bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.talks__back {
  position: absolute;
  top: 22px;
  left: 22px;
  z-index: 3;
}

.talks__center {
  position: absolute;
  inset: 0;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.talks__panel {
  display: flex;
  flex-direction: column;
  width: min(640px, 100%);
  height: min(78vh, 720px);
  padding: 26px 26px 20px;
  border-radius: 24px;
  border: 1px solid rgba(96, 165, 250, 0.22);
  background: rgba(8, 15, 32, 0.62);
  -webkit-backdrop-filter: blur(18px);
  backdrop-filter: blur(18px);
  box-shadow:
    0 30px 80px rgba(0, 0, 0, 0.5),
    inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.talks__header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 18px;
}

.talks__title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 0.01em;
  color: #eaf2ff;
}

.talks__count {
  font-size: 13px;
  color: rgba(147, 197, 253, 0.6);
}

.talks__list {
  position: relative;
  flex: 1;
  min-height: 0;
}

.talks__empty {
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

.talks__empty--error {
  color: #fca5a5;
}

.room {
  display: flex;
  align-items: center;
  gap: 14px;
}

.room__avatar-wrap {
  position: relative;
  flex: none;
}

.room__avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid rgba(96, 165, 250, 0.4);
}

.room__avatar--letter {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
  font-weight: 600;
  color: #eaf2ff;
  background: linear-gradient(135deg, #2563eb, #1e40af);
}

.room__online {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 11px;
  height: 11px;
  border-radius: 50%;
  border: 2px solid #0b1428;
}

.room__online.is-online {
  background: #34d399;
  box-shadow: 0 0 10px rgba(52, 211, 153, 0.8);
}

.room__online.is-offline {
  background: #64748b;
}

.room__body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.room__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.room__name {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 600;
  color: #eaf2ff;
}

.room__time {
  flex: none;
  font-size: 12px;
  color: rgba(147, 197, 253, 0.5);
}

.room__preview {
  margin: 0;
  font-size: 13px;
  color: rgba(147, 197, 253, 0.62);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.room__unread-dot {
  display: inline-block;
  flex: none;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ef4444;
  box-shadow: 0 0 10px rgba(239, 68, 68, 0.8);
}

@media (max-width: 640px) {
  .talks__back {
    top: 12px;
    left: 12px;
  }

  .talks__center {
    padding: 12px;
  }

  .talks__panel {
    height: min(84vh, 720px);
    border-radius: 18px;
    padding: 20px 16px 16px;
  }
}
</style>
