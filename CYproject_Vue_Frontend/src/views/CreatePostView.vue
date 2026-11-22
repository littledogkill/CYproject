<script setup>
import { computed, nextTick, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import SpecularButton from '@/components/SpecularButton.vue'
import DotField from '@/components/DotField.vue'
import MarkdownView from '@/components/MarkdownView.vue'
import { uploadImage } from '@/utils/imageUpload'
import { createPost } from '@/utils/communityApi'

const router = useRouter()

const categories = [
  { label: '作品帖', value: 'work' },
  { label: '询问帖', value: 'ask' },
]

const form = reactive({
  title: '',
  category: 'work',
  tags: '',
  content: '',
})

const canSubmit = computed(() => form.title.trim() && form.content.trim())

// ============ 内容编辑器状态 ============
const contentRef = ref(null)
const fileInputRef = ref(null)
const previewMode = ref(false)
const linkPanelOpen = ref(false)
const imagePanelOpen = ref(false)
const linkText = ref('')
const linkUrl = ref('')
const imageUrl = ref('')
const imageAlt = ref('')
const editorHint = ref('')
const isUploading = ref(false)
const isSubmitting = ref(false)

const MAX_IMAGE = 10 * 1024 * 1024 // 原图上限 10MB，入库前会先压缩

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/forum')
  }
}

function flashHint(msg) {
  editorHint.value = msg
  setTimeout(() => {
    if (editorHint.value === msg) editorHint.value = ''
  }, 2600)
}

function toggleLink() {
  linkPanelOpen.value = !linkPanelOpen.value
  imagePanelOpen.value = false
}

function toggleImage() {
  imagePanelOpen.value = !imagePanelOpen.value
  linkPanelOpen.value = false
}

function togglePreview() {
  previewMode.value = !previewMode.value
  if (!previewMode.value) {
    nextTick(() => contentRef.value?.focus())
  }
}

// 在光标处插入一段固定文本，插入后光标移到文本末尾
function insertAtCursor(text) {
  const el = contentRef.value
  const start = el?.selectionStart ?? form.content.length
  const end = el?.selectionEnd ?? start
  form.content = form.content.slice(0, start) + text + form.content.slice(end)
  nextTick(() => {
    if (!el) return
    el.focus()
    const pos = start + text.length
    el.setSelectionRange(pos, pos)
  })
}

// 用 before/after 包裹当前选中文本（无选中时用 placeholder），并保持选中以便继续输入
function wrapSelection(before, after, placeholder) {
  const el = contentRef.value
  const start = el?.selectionStart ?? form.content.length
  const end = el?.selectionEnd ?? start
  const selected = form.content.slice(start, end) || placeholder
  const insert = before + selected + after
  form.content = form.content.slice(0, start) + insert + form.content.slice(end)
  nextTick(() => {
    if (!el) return
    el.focus()
    el.setSelectionRange(start + before.length, start + before.length + selected.length)
  })
}

function insertBold() {
  wrapSelection('**', '**', '加粗文字')
}

function insertItalic() {
  wrapSelection('*', '*', '斜体文字')
}

function insertCode() {
  wrapSelection('`', '`', '代码')
}

function insertLink() {
  const url = linkUrl.value.trim()
  const label = linkText.value.trim() || url
  if (!url) {
    flashHint('请填写链接地址')
    return
  }
  insertAtCursor(`[${label}](${url})`)
  linkPanelOpen.value = false
  linkText.value = ''
  linkUrl.value = ''
}

function insertImageUrl() {
  const url = imageUrl.value.trim()
  if (!url) {
    flashHint('请填写图片地址')
    return
  }
  insertAtCursor(`![${imageAlt.value.trim() || '图片'}](${url})`)
  imagePanelOpen.value = false
  imageUrl.value = ''
  imageAlt.value = ''
}

function openImagePicker() {
  fileInputRef.value?.click()
}

async function insertImageFile(file) {
  if (!file) return
  if (!file.type.startsWith('image/')) {
    flashHint('请选择图片文件')
    return
  }
  if (file.size > MAX_IMAGE) {
    flashHint('图片过大（超过 10MB），请压缩后再试')
    return
  }

  isUploading.value = true
  editorHint.value = '图片处理中…'
  try {
    const url = await uploadImage(file)
    insertAtCursor(`![${file.name}](${url})`)
    imagePanelOpen.value = false
  } catch (error) {
    console.error('[insertImageFile] 上传失败：', error)
    editorHint.value =
      error?.response?.data?.message || error?.message || '图片上传失败，请检查网络后重试'
  } finally {
    isUploading.value = false
    const hint = editorHint.value
    setTimeout(() => {
      if (editorHint.value === hint) {
        editorHint.value = ''
      }
    }, 2400)
  }
}

function onImageFile(e) {
  const file = e.target.files?.[0]
  insertImageFile(file)
  e.target.value = ''
}

function onPaste(e) {
  const items = e.clipboardData?.items
  if (!items) return
  for (const item of items) {
    if (item.kind === 'file' && item.type.startsWith('image/')) {
      const file = item.getAsFile()
      if (file) {
        e.preventDefault()
        insertImageFile(file)
      }
      break
    }
  }
}

async function onSubmit() {
  if (!canSubmit.value || isSubmitting.value || isUploading.value) return
  isSubmitting.value = true
  editorHint.value = ''
  try {
    const post = await createPost({
      title: form.title.trim(),
      category: form.category,
      tags: form.tags
        .split(/[,，\s]+/)
        .map((t) => t.replace(/^#/, ''))
        .filter(Boolean),
      content: form.content.trim(),
    })
    await router.push(`/post/${encodeURIComponent(post.id)}`)
  } catch (error) {
    editorHint.value =
      error.response?.data?.message || error.message || '发布失败，请稍后重试'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <div class="create">
    <!-- 背景 -->
    <div class="create__bg" aria-hidden="true">
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
    <div class="create__back">
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

    <!-- 中央发帖表单 -->
    <div class="create__center">
      <form class="create__card" @submit.prevent="onSubmit">
        <header class="create__head">
          <h1 class="create__title">发布帖子</h1>
          <p class="create__subtitle">分享你的作品，或向社区提出疑问</p>
        </header>

        <label class="create__label" for="post-title">帖子主题</label>
        <input
          id="post-title"
          v-model="form.title"
          class="create__input"
          type="text"
          placeholder="一句话说清主题，例如：用 Vue 3 + ogl 打造赛博朋克主页"
          autocomplete="off"
        />

        <div class="create__row">
          <div class="create__field">
            <span class="create__label">分类</span>
            <div class="create__chips">
              <button
                v-for="c in categories"
                :key="c.value"
                type="button"
                class="create__chip"
                :class="{ 'create__chip--active': form.category === c.value }"
                @click="form.category = c.value"
              >
                {{ c.label }}
              </button>
            </div>
          </div>

          <div class="create__field create__field--grow">
            <label class="create__label" for="post-tags">标签</label>
            <input
              id="post-tags"
              v-model="form.tags"
              class="create__input"
              type="text"
              placeholder="Vue, WebGL（用逗号分隔）"
              autocomplete="off"
            />
          </div>
        </div>

        <div class="create__field">
          <div class="create__label-row">
            <label class="create__label" for="post-content">帖子内容</label>
            <span class="create__hint-inline">支持 Markdown · 链接 · 图片</span>
          </div>

          <div class="create__editor">
            <!-- 工具栏 -->
            <div class="create__toolbar">
              <button type="button" class="create__tool" @click="insertBold">加粗</button>
              <button type="button" class="create__tool" @click="insertItalic">斜体</button>
              <button type="button" class="create__tool" @click="insertCode">代码</button>
              <span class="create__toolbar-sep"></span>
              <button
                type="button"
                class="create__tool"
                :class="{ 'create__tool--active': linkPanelOpen }"
                @click="toggleLink"
              >
                链接
              </button>
              <button
                type="button"
                class="create__tool"
                :class="{ 'create__tool--active': imagePanelOpen }"
                @click="toggleImage"
              >
                图片
              </button>
              <span class="create__toolbar-sep"></span>
              <button
                type="button"
                class="create__tool"
                :class="{ 'create__tool--active': previewMode }"
                @click="togglePreview"
              >
                {{ previewMode ? '编辑' : '预览' }}
              </button>
              <span v-if="editorHint" class="create__tool-hint" :class="{ 'create__tool-hint--busy': isUploading }">
                {{ editorHint }}
              </span>
            </div>

            <!-- 链接面板 -->
            <div v-if="linkPanelOpen" class="create__panel">
              <input v-model="linkText" class="create__input create__input--panel" type="text" placeholder="链接文字（可选）" />
              <input
                v-model="linkUrl"
                class="create__input create__input--panel"
                type="text"
                placeholder="链接地址，例如 https://…"
                @keyup.enter="insertLink"
              />
              <button type="button" class="create__panel-btn" @click="insertLink">插入链接</button>
            </div>

            <!-- 图片面板 -->
            <div v-if="imagePanelOpen" class="create__panel">
              <input v-model="imageAlt" class="create__input create__input--panel" type="text" placeholder="图片描述（可选）" />
              <input
                v-model="imageUrl"
                class="create__input create__input--panel"
                type="text"
                placeholder="图片地址，例如 https://…"
                @keyup.enter="insertImageUrl"
              />
              <div class="create__panel-actions">
                <button type="button" class="create__panel-btn" @click="insertImageUrl">插入图片</button>
                <button
                  type="button"
                  class="create__panel-btn create__panel-btn--ghost"
                  :disabled="isUploading"
                  @click="openImagePicker"
                >
                  {{ isUploading ? '处理中…' : '上传本地图片' }}
                </button>
                <input ref="fileInputRef" type="file" accept="image/*" class="create__file" @change="onImageFile" />
              </div>
            </div>

            <textarea
              v-show="!previewMode"
              id="post-content"
              ref="contentRef"
              v-model="form.content"
              class="create__textarea"
              rows="10"
              placeholder="展开说说你的思路、实现细节或问题描述…&#10;&#10;支持 Markdown：**加粗**、*斜体*、`代码`、[链接](https://…)、![图片](https://…)，也可以直接粘贴或上传图片。"
              @paste="onPaste"
            ></textarea>

            <!-- 实时预览 -->
            <div v-if="previewMode" class="create__preview markdown-body">
              <MarkdownView :source="form.content" />
            </div>
          </div>
        </div>

        <div class="create__actions">
          <span class="create__hint">{{ canSubmit ? '准备好了，点击发布' : '请填写主题和内容' }}</span>
          <SpecularButton
            size="md"
            :radius="999"
            text-color="#eaf2ff"
            line-color="#60a5fa"
            base-color="#1e3a8a"
            :intensity="1.1"
            :auto-animate="true"
            :speed="0.5"
            :disabled="!canSubmit || isSubmitting || isUploading"
            type="submit"
          >
            {{ isSubmitting ? '发布中…' : '发布' }}
          </SpecularButton>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.create {
  position: relative;
  width: 100%;
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
  color: #dbeafe;
  background:
    radial-gradient(900px 600px at 50% 0%, rgba(3, 64, 234, 0.1), transparent 60%),
    #03060d;
}

.create__bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.create__back {
  position: absolute;
  top: 22px;
  left: 22px;
  z-index: 3;
}

.create__center {
  position: absolute;
  inset: 0;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 88px 24px 40px;
  overflow-y: auto;
}

.create__card {
  width: min(680px, 100%);
  padding: 30px 30px 26px;
  border-radius: 22px;
  border: 1px solid rgba(96, 165, 250, 0.22);
  background: rgba(8, 15, 32, 0.72);
  -webkit-backdrop-filter: blur(16px);
  backdrop-filter: blur(16px);
  box-shadow:
    0 24px 60px rgba(0, 0, 0, 0.45),
    inset 0 1px 0 rgba(255, 255, 255, 0.05);
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.create__head {
  margin-bottom: 4px;
}

.create__title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 0.01em;
  color: #eaf2ff;
}

.create__subtitle {
  margin: 6px 0 0;
  font-size: 13px;
  letter-spacing: 0.06em;
  color: rgba(147, 197, 253, 0.55);
}

.create__label {
  font-size: 13px;
  color: rgba(191, 219, 254, 0.8);
  letter-spacing: 0.04em;
}

.create__label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.create__hint-inline {
  font-size: 12px;
  color: rgba(147, 197, 253, 0.45);
}

.create__input,
.create__textarea {
  width: 100%;
  padding: 12px 16px;
  border-radius: 12px;
  border: 1px solid rgba(96, 165, 250, 0.2);
  background: rgba(10, 18, 40, 0.5);
  color: #eaf2ff;
  font-size: 14px;
  line-height: 1.6;
  font-family: inherit;
  outline: none;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.create__input {
  height: 44px;
}

.create__textarea {
  resize: vertical;
  min-height: 220px;
}

.create__input::placeholder,
.create__textarea::placeholder {
  color: rgba(147, 197, 253, 0.35);
}

.create__input:focus,
.create__textarea:focus {
  border-color: rgba(96, 165, 250, 0.7);
  box-shadow: 0 0 0 3px rgba(96, 165, 250, 0.16);
}

.create__row {
  display: flex;
  gap: 16px;
  align-items: flex-end;
}

.create__field {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.create__field--grow {
  flex: 1;
}

.create__chips {
  display: flex;
  gap: 8px;
}

.create__chip {
  padding: 8px 18px;
  border-radius: 999px;
  border: 1px solid rgba(96, 165, 250, 0.22);
  background: rgba(10, 18, 40, 0.5);
  color: rgba(191, 219, 254, 0.8);
  font-size: 13px;
  cursor: pointer;
  transition:
    background 0.2s ease,
    color 0.2s ease,
    border-color 0.2s ease;
}

.create__chip:hover {
  border-color: rgba(96, 165, 250, 0.55);
  color: #eaf2ff;
}

.create__chip--active {
  background: linear-gradient(135deg, #2563eb, #1e40af);
  border-color: rgba(96, 165, 250, 0.8);
  color: #ffffff;
  box-shadow: 0 0 16px rgba(37, 99, 235, 0.4);
}

/* ============ 编辑器 ============ */
.create__editor {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.create__toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.create__tool {
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
    border-color 0.2s ease;
}

.create__tool:hover {
  border-color: rgba(96, 165, 250, 0.55);
  color: #eaf2ff;
}

.create__tool--active {
  background: linear-gradient(135deg, #2563eb, #1e40af);
  border-color: rgba(96, 165, 250, 0.8);
  color: #ffffff;
}

.create__toolbar-sep {
  width: 1px;
  height: 18px;
  background: rgba(96, 165, 250, 0.22);
}

.create__tool-hint {
  font-size: 12px;
  color: #fca5a5;
}

.create__tool-hint--busy {
  color: #93c5fd;
}

.create__panel {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  border-radius: 14px;
  border: 1px solid rgba(96, 165, 250, 0.24);
  background: rgba(6, 12, 26, 0.6);
}

.create__input--panel {
  height: 40px;
}

.create__panel-actions {
  display: flex;
  gap: 8px;
}

.create__panel-btn {
  padding: 8px 16px;
  border-radius: 999px;
  border: 1px solid rgba(96, 165, 250, 0.4);
  background: linear-gradient(135deg, #2563eb, #1e40af);
  color: #ffffff;
  font-size: 13px;
  cursor: pointer;
  transition: filter 0.2s ease;
}

.create__panel-btn:hover {
  filter: brightness(1.08);
}

.create__panel-btn:disabled {
  opacity: 0.6;
  cursor: default;
}

.create__panel-btn--ghost {
  background: transparent;
  border-color: rgba(96, 165, 250, 0.3);
  color: #93c5fd;
}

.create__file {
  display: none;
}

.create__preview {
  min-height: 220px;
  padding: 12px 16px;
  border-radius: 12px;
  border: 1px solid rgba(96, 165, 250, 0.2);
  background: rgba(10, 18, 40, 0.5);
  color: #dbeafe;
  font-size: 14px;
  line-height: 1.8;
  word-break: break-word;
}

.create__preview :deep(a) {
  color: #60a5fa;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.create__preview :deep(a:hover) {
  color: #93c5fd;
}

.create__preview :deep(img) {
  max-width: 100%;
  border-radius: 10px;
  margin: 6px 0;
  display: block;
}

.create__preview :deep(p) {
  margin: 0 0 12px;
}

.create__preview :deep(p:last-child) {
  margin-bottom: 0;
}

.create__preview :deep(h1),
.create__preview :deep(h2),
.create__preview :deep(h3),
.create__preview :deep(h4) {
  margin: 18px 0 10px;
  line-height: 1.4;
  color: #eaf2ff;
}

.create__preview :deep(h1:first-child),
.create__preview :deep(h2:first-child),
.create__preview :deep(h3:first-child),
.create__preview :deep(h4:first-child) {
  margin-top: 0;
}

.create__preview :deep(code) {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 0.9em;
  background: rgba(37, 99, 235, 0.16);
  color: #bfdbfe;
  padding: 2px 6px;
  border-radius: 6px;
}

.create__preview :deep(pre) {
  padding: 12px 14px;
  border-radius: 10px;
  background: rgba(4, 9, 20, 0.8);
  border: 1px solid rgba(96, 165, 250, 0.16);
  overflow-x: auto;
  margin: 0 0 12px;
}

.create__preview :deep(pre code) {
  background: transparent;
  padding: 0;
  color: #dbeafe;
}

.create__preview :deep(blockquote) {
  margin: 0 0 12px;
  padding: 6px 14px;
  border-left: 3px solid #2563eb;
  background: rgba(37, 99, 235, 0.1);
  color: rgba(191, 219, 254, 0.85);
}

.create__preview :deep(ul),
.create__preview :deep(ol) {
  margin: 0 0 12px;
  padding-left: 22px;
}

.create__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 6px;
}

.create__hint {
  font-size: 12px;
  letter-spacing: 0.04em;
  color: rgba(147, 197, 253, 0.45);
}

@media (max-width: 640px) {
  .create__back {
    top: 12px;
    left: 12px;
  }

  .create__center {
    padding: 80px 12px 24px;
  }

  .create__card {
    padding: 22px 20px 20px;
  }

  .create__row {
    flex-direction: column;
    align-items: stretch;
    gap: 14px;
  }
}
</style>
