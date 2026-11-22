<script setup>
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import SpecularButton from '@/components/SpecularButton.vue'
import AnimatedList from '@/components/AnimatedList.vue'
import DotField from '@/components/DotField.vue'
import { getFollowList } from '@/utils/communityApi'
import { vAuthedSrc } from '@/utils/authImage'

const router = useRouter()

const tab = ref('following')
const items = ref([])
const isLoading = ref(false)
const loadError = ref('')
let requestId = 0

async function loadItems() {
  const currentRequestId = ++requestId
  isLoading.value = true
  loadError.value = ''
  try {
    const result = await getFollowList(tab.value, { page: 1, pageSize: 50 })
    if (currentRequestId === requestId) items.value = result.items
  } catch (error) {
    if (currentRequestId === requestId) {
      items.value = []
      loadError.value =
        error.response?.data?.message || error.message || '加载关注列表失败，请稍后重试'
    }
  } finally {
    if (currentRequestId === requestId) isLoading.value = false
  }
}

watch(tab, loadItems, { immediate: true })

function goProfile() {
  router.push('/profile')
}

function openUser(user) {
  router.push(`/user/${encodeURIComponent(user.id || user.username)}`)
}
</script>

<template>
  <div class="follows">
    <!-- 背景 -->
    <div class="follows__bg" aria-hidden="true">
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
    <div class="follows__back">
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

    <!-- 中央列表面板 -->
    <div class="follows__center">
      <section class="follows__panel">
        <h1 class="follows__title">关注列表</h1>

        <div class="follows__tabs">
          <button
            type="button"
            class="follows__tab"
            :class="{ 'follows__tab--active': tab === 'following' }"
            @click="tab = 'following'"
          >
            我关注的人
          </button>
          <button
            type="button"
            class="follows__tab"
            :class="{ 'follows__tab--active': tab === 'followers' }"
            @click="tab = 'followers'"
          >
            关注我的人
          </button>
        </div>

        <main class="follows__list">
          <p v-if="isLoading" class="follows__message" role="status">正在加载…</p>
          <p v-else-if="loadError" class="follows__message follows__message--error" role="alert">
            {{ loadError }}
          </p>
          <AnimatedList
            v-else-if="items.length"
            :items="items"
            :max-height="'100%'"
            :show-gradients="true"
            :enable-arrow-navigation="true"
            :display-scrollbar="true"
            @item-selected="openUser"
          >
            <template #item="{ item }">
              <div class="follow">
                <img
                  v-if="item.avatar"
                  class="follow__avatar"
                  v-authed-src="item.avatar"
                  alt="头像"
                />
                <div v-else class="follow__avatar follow__avatar--letter">
                  {{ item.name.slice(0, 1) }}
                </div>
                <span class="follow__name">{{ item.name }}</span>
              </div>
            </template>
          </AnimatedList>

          <p v-else class="follows__empty">暂无用户</p>
        </main>
      </section>
    </div>
  </div>
</template>

<style scoped>
.follows {
  position: relative;
  width: 100%;
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
  color: #dbeafe;
  background:
    radial-gradient(900px 600px at 50% 0%, rgba(3, 64, 234, 0.1), transparent 60%), #03060d;
}

.follows__bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.follows__back {
  position: absolute;
  top: 22px;
  left: 22px;
  z-index: 3;
}

.follows__center {
  position: absolute;
  inset: 0;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.follows__panel {
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

.follows__title {
  margin: 0 0 18px;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 0.01em;
  color: #eaf2ff;
  text-align: center;
}

.follows__tabs {
  display: flex;
  gap: 10px;
  justify-content: center;
  margin-bottom: 14px;
}

.follows__tab {
  padding: 8px 22px;
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

.follows__tab:hover {
  border-color: rgba(96, 165, 250, 0.55);
  color: #eaf2ff;
}

.follows__tab--active {
  background: linear-gradient(135deg, #2563eb, #1e40af);
  border-color: rgba(96, 165, 250, 0.8);
  color: #ffffff;
  box-shadow: 0 0 16px rgba(37, 99, 235, 0.4);
}

.follows__list {
  position: relative;
  flex: 1;
  min-height: 0;
}

.follows__empty {
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

.follows__message {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  margin: 0;
  color: rgba(191, 219, 254, 0.72);
}

.follows__message--error {
  color: #fca5a5;
}

.follow {
  display: flex;
  align-items: center;
  gap: 12px;
}

.follow__avatar {
  flex: none;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid rgba(96, 165, 250, 0.4);
}

.follow__avatar--letter {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 600;
  color: #eaf2ff;
  background: linear-gradient(135deg, #2563eb, #1e40af);
}

.follow__name {
  font-size: 15px;
  font-weight: 600;
  color: #eaf2ff;
}

@media (max-width: 640px) {
  .follows__back {
    top: 12px;
    left: 12px;
  }

  .follows__center {
    padding: 12px;
  }

  .follows__panel {
    height: min(84vh, 720px);
    border-radius: 18px;
    padding: 20px 16px 16px;
  }
}
</style>
