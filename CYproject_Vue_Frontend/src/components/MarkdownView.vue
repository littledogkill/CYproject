<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { renderMarkdown } from '@/utils/markdown'
import { loadAuthedImage, revokeImageUrl } from '@/utils/authImage'

const props = defineProps({
  source: { type: String, default: '' },
})

const container = ref(null)
const html = ref('')
// 记录本次渲染生成的 blob URL，离开/重新渲染时统一回收
const objectUrls = new Set()

function clearObjectUrls() {
  objectUrls.forEach((url) => revokeImageUrl(url))
  objectUrls.clear()
}

function hydrateImages() {
  const root = container.value
  if (!root) return
  root.querySelectorAll('img[data-src]').forEach((img) => {
    const src = img.getAttribute('data-src')
    if (!src) return
    loadAuthedImage(src).then((url) => {
      // 内容已切换、元素已不在文档中时，回收并放弃
      if (!container.value || !img.isConnected) {
        if (url && url.startsWith('blob:')) revokeImageUrl(url)
        return
      }
      if (url && url.startsWith('blob:')) objectUrls.add(url)
      if (url) {
        img.setAttribute('src', url)
        img.removeAttribute('data-src')
      }
    })
  })
}

function render() {
  const nextHtml = renderMarkdown(props.source)
  if (nextHtml === html.value) return
  clearObjectUrls()
  html.value = nextHtml
  nextTick(hydrateImages)
}

onMounted(render)

watch(
  () => props.source,
  () => {
    render()
  },
)

onBeforeUnmount(() => {
  clearObjectUrls()
})
</script>

<template>
  <div ref="container" class="markdown-view" v-html="html"></div>
</template>
