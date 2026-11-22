<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  logo: {
    type: String,
    default: '',
  },
  items: {
    type: Array,
    default: () => ['首页', 'AI 模型', '智能体', '社区'],
  },
  activeIndex: {
    type: Number,
    default: 0,
  },
})

const emit = defineEmits(['select'])

const navRef = ref(null)
const sliderStyle = ref({ transform: 'translateX(0px)', width: '0px' })
const hoverIndex = ref(-1)
const current = ref(props.activeIndex)

const normalizedItems = computed(() =>
  props.items.map((item, index) => {
    if (typeof item === 'string') {
      return { label: item, href: undefined, to: undefined, icon: undefined }
    }
    return {
      label: item.label ?? item.title ?? `导航 ${index + 1}`,
      href: item.href,
      to: item.to ?? item.path,
      icon: item.icon,
    }
  })
)

const targetIndex = () => (hoverIndex.value >= 0 ? hoverIndex.value : current.value)

const position = (index) => {
  const nav = navRef.value
  if (!nav) return
  const items = nav.querySelectorAll('.pill-nav__item')
  const el = items[index]
  if (!el) return
  const navRect = nav.getBoundingClientRect()
  const rect = el.getBoundingClientRect()
  sliderStyle.value = {
    transform: `translateX(${rect.left - navRect.left}px)`,
    width: `${rect.width}px`,
  }
}

const onEnter = (i) => {
  hoverIndex.value = i
  position(i)
}

const onLeaveNav = () => {
  hoverIndex.value = -1
  position(current.value)
}

const onSelect = (i) => {
  current.value = i
  position(i)
  emit('select', i)
}

let resizeObserver
let raf = 0

const schedulePosition = () => {
  cancelAnimationFrame(raf)
  raf = requestAnimationFrame(() => position(targetIndex()))
}

watch(
  () => props.items,
  () => schedulePosition()
)

watch(
  () => props.activeIndex,
  (value) => {
    current.value = value
    schedulePosition()
  }
)

onMounted(() => {
  schedulePosition()
  if (navRef.value) {
    resizeObserver = new ResizeObserver(schedulePosition)
    resizeObserver.observe(navRef.value)
  }
  if (document.fonts?.ready) document.fonts.ready.then(schedulePosition)
  window.addEventListener('resize', schedulePosition)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(raf)
  resizeObserver?.disconnect()
  window.removeEventListener('resize', schedulePosition)
})
</script>

<template>
  <nav ref="navRef" class="pill-nav" aria-label="主导航" @mouseleave="onLeaveNav">
    <span class="pill-nav__slider" :style="sliderStyle" aria-hidden="true"></span>

    <img v-if="logo" class="pill-nav__logo" :src="logo" alt="CY" />

    <component
      :is="item.to || item.href ? 'a' : 'button'"
      v-for="(item, index) in normalizedItems"
      :key="item.to ?? item.href ?? item.label ?? index"
      class="pill-nav__item"
      :class="{ 'is-active': index === current }"
      :href="item.to ?? item.href"
      :aria-current="index === current ? 'page' : undefined"
      @mouseenter="onEnter(index)"
      @click="onSelect(index)"
    >
      <img v-if="item.icon" class="pill-nav__item-icon" :src="item.icon" alt="" aria-hidden="true" />
      <span>{{ item.label }}</span>
    </component>
  </nav>
</template>

<style scoped>
.pill-nav {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 6px;
  border-radius: 9999px;
  background: rgba(10, 18, 40, 0.35);
  border: 1px solid rgba(96, 165, 250, 0.16);
  -webkit-backdrop-filter: blur(16px);
  backdrop-filter: blur(16px);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.06),
    0 8px 32px rgba(2, 6, 18, 0.5);
  isolation: isolate;
}

.pill-nav__logo {
  width: 26px;
  height: 26px;
  flex: 0 0 auto;
  margin-right: 4px;
  border-radius: 8px;
  object-fit: contain;
  display: block;
  -webkit-user-drag: none;
  user-select: none;
}

.pill-nav__slider {
  position: absolute;
  top: 6px;
  bottom: 6px;
  left: 0;
  border-radius: 9999px;
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.95), rgba(30, 64, 175, 0.95));
  box-shadow:
    0 0 16px rgba(37, 99, 235, 0.5),
    inset 0 1px 0 rgba(255, 255, 255, 0.22);
  transition:
    transform 0.32s cubic-bezier(0.23, 1, 0.32, 1),
    width 0.32s cubic-bezier(0.23, 1, 0.32, 1);
  z-index: -1;
  will-change: transform, width;
}

.pill-nav__item {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 36px;
  padding: 0 16px;
  border: none;
  border-radius: 9999px;
  background: transparent;
  color: rgba(191, 219, 254, 0.78);
  font-size: 14px;
  font-weight: 500;
  letter-spacing: 0.02em;
  white-space: nowrap;
  cursor: pointer;
  text-decoration: none;
  transition: color 0.25s ease;
  -webkit-tap-highlight-color: transparent;
  outline: none;
}

.pill-nav__item-icon {
  width: 16px;
  height: 16px;
  margin-right: 6px;
  object-fit: contain;
  display: block;
  -webkit-user-drag: none;
  user-select: none;
}

.pill-nav__item:hover,
.pill-nav__item.is-active {
  color: #ffffff;
}

.pill-nav__item:focus-visible {
  box-shadow: 0 0 0 2px rgba(96, 165, 250, 0.85);
}
</style>
