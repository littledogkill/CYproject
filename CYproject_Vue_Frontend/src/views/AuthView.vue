<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SpecularButton from '@/components/SpecularButton.vue'
import Shuffle from '@/components/Shuffle.vue'
import BorderGlow from '@/components/BorderGlow.vue'
import { setAuthToken } from '@/utils/auth'
import { signIn, signUp } from '@/utils/communityApi'

const route = useRoute()
const router = useRouter()
const statusMessage = ref('')
const isSubmitting = ref(false)

const isLogin = computed(() => route.name === 'login')

const form = reactive({
  username: '',
  email: '',
  password: '',
})

const shuffleStyle = {
  fontSize: 'clamp(2.25rem, 7vw, 6.5rem)',
  color: 'rgba(96, 165, 250, 0.12)',
  textAlign: 'center',
  lineHeight: 1.05,
}

function goHome() {
  router.push('/')
}

function switchMode() {
  router.push({
    name: isLogin.value ? 'register' : 'login',
    query: route.query,
  })
}

async function onSubmit() {
  statusMessage.value = ''
  isSubmitting.value = true

  const payload = isLogin.value
    ? { email: form.email.trim(), password: form.password }
    : {
        username: form.username.trim(),
        email: form.email.trim(),
        password: form.password,
      }

  try {
    const data = isLogin.value ? await signIn(payload) : await signUp(payload)
    setAuthToken(data?.token)
    const redirect = route.query.redirect
    const target =
      typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('//')
        ? redirect
        : '/profile'
    await router.replace(target)
  } catch (error) {
    const data = error.response?.data
    statusMessage.value =
      data?.message ||
      (error.response?.status
        ? `请求失败，后端返回状态码 ${error.response.status}`
        : `请求失败：${error.message || '无法连接后端'}`)
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <div class="auth">
    <!-- Shuffle 背景 -->
    <div class="auth__bg" aria-hidden="true">
      <Shuffle
        text="LOGIN / REGISTER"
        tag="div"
        :loop="true"
        :loop-delay="0.5"
        :trigger-once="true"
        :trigger-on-hover="false"
        :duration="0.35"
        :stagger="0.02"
        scramble-charset="01#/|=+*"
        color-to="#60A5FA"
        :custom-style="shuffleStyle"
      />
    </div>

    <!-- 左上角返回首页 -->
    <div class="auth__back">
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

    <!-- 居中登录/注册卡片 -->
    <div class="auth__center">
      <div class="auth__card" :class="{ 'auth__card--compact': !isLogin }">
        <BorderGlow
          :edge-sensitivity="24"
          glow-color="214 90 70"
          background-color="#060D1C"
          :border-radius="24"
          :glow-radius="46"
          :glow-intensity="3.0"
          :cone-spread="22"
          :animated="true"
          :colors="['#2563eb', '#60a5fa', '#93c5fd']"
        >
          <div class="auth__panel" :class="{ 'auth__panel--compact': !isLogin }">
            <h2 class="auth__title">{{ isLogin ? '登录' : '注册' }}</h2>
            <p class="auth__subtitle">探索前沿 AI 模型与智能体</p>

            <form class="auth__form" @submit.prevent="onSubmit">
              <template v-if="isLogin">
                <label class="auth__label" for="email">邮箱</label>
                <input
                  id="email"
                  v-model="form.email"
                  class="auth__input"
                  type="email"
                  placeholder="name@example.com"
                  autocomplete="email"
                  required
                />
              </template>
              <template v-else>
                <label class="auth__label" for="username">用户名</label>
                <input
                  id="username"
                  v-model="form.username"
                  class="auth__input"
                  type="text"
                  placeholder="你的用户名"
                  autocomplete="username"
                  required
                />

                <label class="auth__label" for="email">邮箱</label>
                <input
                  id="email"
                  v-model="form.email"
                  class="auth__input"
                  type="email"
                  placeholder="name@example.com"
                  autocomplete="email"
                  required
                />
              </template>

              <label class="auth__label" for="password">密码</label>
              <input
                id="password"
                v-model="form.password"
                class="auth__input"
                type="password"
                placeholder="输入密码"
                :autocomplete="isLogin ? 'current-password' : 'new-password'"
                required
              />

              <button class="auth__submit" type="submit" :disabled="isSubmitting">
                {{ isSubmitting ? '请求中…' : isLogin ? '登录' : '注册' }}
              </button>
              <p v-if="statusMessage" class="auth__status" role="status">
                {{ statusMessage }}
              </p>
            </form>

            <p class="auth__switch">
              {{ isLogin ? '还没有账号？' : '已有账号？' }}
              <button type="button" class="auth__switch-btn" @click="switchMode">
                {{ isLogin ? '去注册' : '去登录' }}
              </button>
            </p>
          </div>
        </BorderGlow>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth {
  position: relative;
  width: 100%;
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
  background:
    radial-gradient(900px 700px at 50% 42%, rgba(3, 64, 234, 0.16), transparent 62%),
    linear-gradient(158deg, #03060d 0%, #050b18 48%, #07122a 100%);
}

.auth__bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 7vh 24px 24px;
  pointer-events: none;
}

.auth__back {
  position: absolute;
  top: 22px;
  left: 22px;
  z-index: 2;
}

.auth__center {
  position: absolute;
  inset: 0;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.auth__card {
  width: min(420px, 92vw);
  transform: translateY(7vh);
}

.auth__card--compact {
  width: min(380px, 90vw);
}

.auth__panel {
  width: 100%;
  padding: 38px 34px 30px;
  color: #dbeafe;
}

.auth__panel--compact {
  padding: 30px 28px 24px;
}

.auth__panel--compact .auth__subtitle {
  margin-bottom: 18px;
}

.auth__panel--compact .auth__form {
  gap: 12px;
}

.auth__panel--compact .auth__input {
  height: 42px;
}

.auth__panel--compact .auth__submit {
  margin-top: 4px;
  height: 42px;
}

.auth__title {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 0.01em;
  color: #eaf2ff;
}

.auth__subtitle {
  margin: 6px 0 26px;
  font-size: 13px;
  letter-spacing: 0.08em;
  color: rgba(147, 197, 253, 0.55);
}

.auth__form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.auth__label {
  font-size: 13px;
  color: rgba(191, 219, 254, 0.8);
  letter-spacing: 0.04em;
}

.auth__input {
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

.auth__input::placeholder {
  color: rgba(147, 197, 253, 0.35);
}

.auth__input:focus {
  border-color: rgba(96, 165, 250, 0.7);
  box-shadow: 0 0 0 3px rgba(96, 165, 250, 0.16);
}

.auth__submit {
  margin-top: 6px;
  height: 46px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #2563eb, #1e40af);
  color: #ffffff;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.06em;
  cursor: pointer;
  transition:
    filter 0.2s ease,
    transform 0.15s ease;
}

.auth__submit:hover {
  filter: brightness(1.08);
}

.auth__submit:active {
  transform: scale(0.98);
}

.auth__status {
  margin: 0;
  color: #fbbf24;
  font-size: 13px;
  line-height: 1.5;
}

.auth__response {
  max-height: 180px;
  overflow: auto;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.auth__submit:disabled {
  cursor: wait;
  opacity: 0.7;
}

.auth__switch {
  margin: 20px 0 0;
  font-size: 13px;
  color: rgba(191, 219, 254, 0.6);
  text-align: center;
}

.auth__switch-btn {
  border: none;
  background: transparent;
  padding: 0;
  color: #60a5fa;
  font-size: 13px;
  cursor: pointer;
  transition: color 0.2s ease;
}

.auth__switch-btn:hover {
  color: #93c5fd;
}
</style>
