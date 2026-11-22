<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import LightTunnel from '@/components/LightTunnel.vue'
import SpecularButton from '@/components/SpecularButton.vue'
import SlingButton from '@/components/SlingButton.vue'
import {
  getConversationMessages,
  getCurrentUser,
  getUser,
  markConversationRead,
  sendConversationMessage,
} from '@/utils/communityApi'
import { markConversationReadLocally } from '@/utils/conversationReadState'

const route = useRoute()
const router = useRouter()

// ============ 私聊对象（/chat/:userid 时为具体用户） ============
const chatUserid = computed(() => {
  const u = route.params.userid
  return Array.isArray(u) ? u[0] || '' : u || ''
})
const isPrivate = computed(() => Boolean(chatUserid.value))
const peer = ref(null)
const peerName = computed(() => peer.value?.name || '对方')
const peerOnline = computed(() => peer.value?.online ?? false)
const privateMessages = ref([])
const currentUsername = ref('')
const currentUserId = ref('')
const isLoading = ref(false)
const loadError = ref('')
let conversationRequestId = 0

// ============ 消息状态 ============
// AI 助手模式使用本地消息；私聊模式读取共享会话数据
const aiMessages = ref([
  {
    id: 1,
    role: 'assistant',
    content: '你好，我是 CY→A 的智能助手。有什么想问的，尽管说。',
    time: formatTime(new Date()),
  },
])

const chatMessages = computed(() => {
  if (isPrivate.value) {
    return privateMessages.value.map((m) => ({
      ...m,
      time: formatTime(new Date(m.time)),
    }))
  }
  return aiMessages.value
})

const draft = ref('')
const listRef = ref(null)
const isReplying = ref(false)

let nextId = 2

// 第一版占位回复，后续接入真实模型 / 真实消息服务
const aiReplies = [
  '收到！这个问题我先记下了，等接入真实模型后我再认真回答你。',
  '好想法！目前还是第一版占位回复，等我们把对话引擎接上。',
  '明白了。你可以先继续补充细节，我都会保留在会话里。',
  '了解。第一版先保证界面和交互，内容部分我们下一步再细究。',
]

function formatTime(date) {
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function normalizeMessage(message) {
  return {
    ...message,
    id: String(message.id),
    role: message.senderUsername === currentUsername.value ? 'user' : 'peer',
    content: message.content,
    time: message.createdAt || message.time,
  }
}

function mergeMessage(message) {
  const incoming = normalizeMessage(message)
  const existingIndex = privateMessages.value.findIndex(
    (item) =>
      item.id === incoming.id ||
      (incoming.clientMessageId && item.clientMessageId === incoming.clientMessageId),
  )
  if (existingIndex === -1) {
    privateMessages.value.push(incoming)
  } else {
    privateMessages.value[existingIndex] = { ...privateMessages.value[existingIndex], ...incoming }
  }
  privateMessages.value.sort((a, b) => new Date(a.time) - new Date(b.time))
  scrollToBottom()
}

watch(
  chatUserid,
  async (userid) => {
    const currentRequestId = ++conversationRequestId
    loadError.value = ''
    isReplying.value = false
    peer.value = null
    privateMessages.value = []
    if (!userid) {
      isLoading.value = false
      return
    }

    isLoading.value = true
    try {
      const currentUser = await getCurrentUser()
      if (currentRequestId !== conversationRequestId) return
      currentUsername.value = currentUser.username
      currentUserId.value = currentUser.id || currentUser.username
      const [peerResult, messages] = await Promise.all([
        getUser(userid),
        getConversationMessages(userid, currentUsername.value, { limit: 50 }),
      ])
      if (currentRequestId !== conversationRequestId) return
      peer.value = peerResult
      privateMessages.value = messages
      const lastMessageTime = messages.length
        ? messages[messages.length - 1].time
        : new Date().toISOString()
      markConversationReadLocally(
        currentUserId.value,
        peerResult.username || userid,
        lastMessageTime,
      )
      try {
        await markConversationRead(userid)
      } catch (error) {
        loadError.value =
          error.response?.data?.message || error.message || '消息已加载，但未读状态更新失败'
      }
      scrollToBottom()
    } catch (error) {
      if (currentRequestId === conversationRequestId) {
        loadError.value =
          error.response?.data?.message || error.message || '加载私聊失败，请稍后重试'
      }
    } finally {
      if (currentRequestId === conversationRequestId) isLoading.value = false
    }
  },
  { immediate: true },
)

function goBack() {
  if (isPrivate.value) {
    if (window.history.state?.back) {
      router.back()
    } else {
      router.replace(`/user/${encodeURIComponent(chatUserid.value)}`)
    }
  } else {
    router.push('/')
  }
}

function pushAiMessage(role, content) {
  aiMessages.value.push({ id: nextId++, role, content, time: formatTime(new Date()) })
  scrollToBottom()
}

function scrollToBottom() {
  nextTick(() => {
    const el = listRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

async function send() {
  const text = draft.value.trim()
  if (!text || isReplying.value) return

  isReplying.value = true
  loadError.value = ''
  if (isPrivate.value) {
    const recipient = chatUserid.value
    const requestIdAtSend = conversationRequestId
    try {
      const message = await sendConversationMessage(recipient, text, currentUsername.value)
      if (requestIdAtSend === conversationRequestId) {
        mergeMessage(message)
        if (draft.value.trim() === text) draft.value = ''
        scrollToBottom()
      }
    } catch (error) {
      if (requestIdAtSend === conversationRequestId) {
        loadError.value =
          error.response?.data?.message || error.message || '消息发送失败，请稍后重试'
      }
    } finally {
      if (requestIdAtSend === conversationRequestId) isReplying.value = false
    }
    return
  }

  draft.value = ''
  pushAiMessage('user', text)
  scrollToBottom()
  setTimeout(() => {
    const reply = aiReplies[Math.floor(Math.random() * aiReplies.length)]
    pushAiMessage('assistant', reply)
    scrollToBottom()
    isReplying.value = false
  }, 700)
}

function onComposerKeydown(e) {
  // Enter 发送，Shift+Enter 换行
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    send()
  }
}
</script>

<template>
  <div class="chat">
    <!-- LightTunnel 背景 -->
    <div class="chat__bg" aria-hidden="true">
      <LightTunnel
        cable-color="#2563EB"
        pulse-color="#60A5FA"
        tunnel-color="#1E3A8A"
        :tunnel-opacity="0"
        :speed="0.12"
        flow-direction="outward"
        :pulse-speed="2"
        :pulse-length="0.3"
        :pulse-blend="1"
        :pulse-width="1"
        :cable-count="24"
        :thickness="0.35"
        :rim-width="0.15"
        :waviness="0.3"
        :sway="0.5"
        :size="1.0"
        :center-x="0"
        :center-y="0"
        :glow="1.1"
        :fade-near="0.5"
        :fade-far="2"
        :brightness="0.9"
        :color-variance="true"
        :grain="true"
        :grain-intensity="0.05"
        :opacity="0.9"
        :mouse-interaction="true"
        :mouse-strength="0.12"
      />
      <!-- 轻微压暗，保证文字可读 -->
      <div class="chat__veil"></div>
    </div>

    <!-- 左上角返回首页 -->
    <div class="chat__back">
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
        {{ isPrivate ? '返回' : '返回首页' }}
      </SpecularButton>
    </div>

    <!-- 中央聊天面板 -->
    <div class="chat__center">
      <section class="chat__panel">
        <header class="chat__header">
          <div class="chat__brand">
            <span
              class="chat__dot"
              :class="{ 'chat__dot--offline': isPrivate && !peerOnline }"
            ></span>
            <span class="chat__title">{{ isPrivate ? peerName : 'CY→A 智能对话' }}</span>
          </div>
          <span class="chat__status">
            {{ isPrivate ? (peerOnline ? '在线' : '离线 · 消息会保存') : '在线 · 占位模型' }}
          </span>
        </header>

        <div ref="listRef" class="chat__messages">
          <p v-if="isLoading" class="chat__empty" role="status">正在加载消息…</p>
          <p v-if="loadError" class="chat__empty chat__empty--error" role="alert">
            {{ loadError }}
          </p>
          <template v-for="msg in chatMessages" :key="msg.id">
            <div class="msg" :class="msg.role === 'user' ? 'msg--user' : 'msg--assistant'">
              <div class="msg__bubble">
                <p class="msg__text">{{ msg.content }}</p>
                <span class="msg__time">{{ msg.time }}</span>
              </div>
            </div>
          </template>

          <!-- 私聊无消息时的占位提示 -->
          <p v-if="isPrivate && !chatMessages.length && !isReplying" class="chat__empty">
            还没有消息，和 TA 打个招呼吧～
          </p>

          <!-- 正在输入 -->
          <div v-if="isReplying" class="msg msg--assistant">
            <div class="msg__bubble msg__bubble--typing">
              <span class="typing"><i></i><i></i><i></i></span>
            </div>
          </div>
        </div>

        <footer class="chat__composer">
          <textarea
            v-model="draft"
            class="chat__input"
            rows="1"
            placeholder="输入消息，Enter 发送，Shift+Enter 换行…"
            autocomplete="off"
            :disabled="isReplying || isLoading || (isPrivate && !peer)"
            @keydown="onComposerKeydown"
          ></textarea>
          <SlingButton
            :size="52"
            pad-color="#0f1c38"
            icon-color="#bfdbfe"
            accent-color="#60a5fa"
            well-color="#1e3a8a"
            band-color="#3b82f6"
            aria-label="发送"
            :disabled="!draft.trim() || isReplying || isLoading || (isPrivate && !peer)"
            @send="send"
          />
        </footer>
      </section>
    </div>
  </div>
</template>

<style scoped>
.chat {
  position: relative;
  width: 100%;
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
  background: #03060d;
}

.chat__bg {
  position: absolute;
  inset: 0;
  z-index: 0;
}

.chat__veil {
  position: absolute;
  inset: 0;
  background: radial-gradient(
    1000px 700px at 50% 46%,
    rgba(4, 9, 20, 0.35),
    rgba(3, 6, 13, 0.78) 72%
  );
}

.chat__back {
  position: absolute;
  top: 22px;
  left: 22px;
  z-index: 3;
}

.chat__center {
  position: absolute;
  inset: 0;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.chat__panel {
  display: flex;
  flex-direction: column;
  width: min(820px, 100%);
  height: min(78vh, 720px);
  border-radius: 24px;
  border: 1px solid rgba(96, 165, 250, 0.22);
  background: rgba(8, 15, 32, 0.62);
  -webkit-backdrop-filter: blur(18px);
  backdrop-filter: blur(18px);
  box-shadow:
    0 30px 80px rgba(0, 0, 0, 0.5),
    inset 0 1px 0 rgba(255, 255, 255, 0.05);
  overflow: hidden;
}

.chat__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 22px;
  border-bottom: 1px solid rgba(96, 165, 250, 0.16);
}

.chat__brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.chat__dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #34d399;
  box-shadow: 0 0 12px rgba(52, 211, 153, 0.8);
}

.chat__dot--offline {
  background: #64748b;
  box-shadow: 0 0 8px rgba(100, 116, 139, 0.5);
}

.chat__title {
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.02em;
  color: #eaf2ff;
}

.chat__status {
  font-size: 12px;
  letter-spacing: 0.04em;
  color: rgba(147, 197, 253, 0.55);
}

.chat__messages {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 24px 22px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  scroll-behavior: smooth;
}

.chat__messages::-webkit-scrollbar {
  width: 8px;
}

.chat__messages::-webkit-scrollbar-thumb {
  background: rgba(96, 165, 250, 0.25);
  border-radius: 999px;
}

.chat__empty {
  margin: auto;
  font-size: 13px;
  color: rgba(147, 197, 253, 0.5);
  text-align: center;
}

.chat__empty--error {
  color: #fca5a5;
}

.msg {
  display: flex;
}

.msg--user {
  justify-content: flex-end;
}

.msg--assistant {
  justify-content: flex-start;
}

.msg__bubble {
  max-width: min(72%, 560px);
  padding: 12px 16px;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.msg--user .msg__bubble {
  background: linear-gradient(135deg, #2563eb, #1e40af);
  border-bottom-right-radius: 6px;
  box-shadow: 0 8px 24px rgba(37, 99, 235, 0.35);
}

.msg--assistant .msg__bubble {
  background: rgba(10, 18, 40, 0.72);
  border: 1px solid rgba(96, 165, 250, 0.2);
  border-bottom-left-radius: 6px;
}

.msg__text {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  color: #eaf2ff;
}

.msg--assistant .msg__text {
  color: #dbeafe;
}

.msg__time {
  align-self: flex-end;
  font-size: 11px;
  color: rgba(147, 197, 253, 0.55);
}

.msg--user .msg__time {
  color: rgba(255, 255, 255, 0.65);
}

.msg__bubble--typing {
  padding: 14px 18px;
}

.typing {
  display: inline-flex;
  gap: 5px;
  align-items: center;
}

.typing i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #60a5fa;
  animation: typing-blink 1.2s infinite ease-in-out;
}

.typing i:nth-child(2) {
  animation-delay: 0.15s;
}

.typing i:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes typing-blink {
  0%,
  60%,
  100% {
    opacity: 0.3;
    transform: translateY(0);
  }
  30% {
    opacity: 1;
    transform: translateY(-3px);
  }
}

.chat__composer {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  padding: 14px 16px 16px;
  border-top: 1px solid rgba(96, 165, 250, 0.16);
  background: rgba(6, 12, 26, 0.5);
}

.chat__input {
  flex: 1;
  min-height: 44px;
  max-height: 140px;
  resize: none;
  padding: 12px 16px;
  border-radius: 14px;
  border: 1px solid rgba(96, 165, 250, 0.22);
  background: rgba(10, 18, 40, 0.6);
  color: #eaf2ff;
  font-size: 14px;
  line-height: 1.5;
  font-family: inherit;
  outline: none;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.chat__input::placeholder {
  color: rgba(147, 197, 253, 0.35);
}

.chat__input:focus {
  border-color: rgba(96, 165, 250, 0.7);
  box-shadow: 0 0 0 3px rgba(96, 165, 250, 0.16);
}

@media (max-width: 640px) {
  .chat__center {
    padding: 12px;
  }

  .chat__back {
    top: 12px;
    left: 12px;
  }

  .chat__panel {
    height: min(84vh, 720px);
    border-radius: 18px;
  }

  .msg__bubble {
    max-width: 86%;
  }
}
</style>
