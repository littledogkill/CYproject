<script setup>
import Scanner from '@/components/Scanner.vue'
import PillNav from '@/components/PillNav.vue'
import TechText from '@/components/TechText.vue'
import InfiniteSpiral from '@/components/InfiniteSpiral.vue'

// 占位导航数据，后续会替换为真实路由
const navItems = [
  { label: '首页', to: '/' },
  { label: '登录/注册', to: '/login' },
  { label: '社区', to: '/forum' },
  { label: '关于', to: '/about' },
  { label: '主页', to: '/profile' },
]

const spiralItems = [
  { src: '/ai-icons/openai.png', alt: 'OpenAI', label: 'OpenAI', href: 'https://openai.com', target: '_blank' },
  { src: '/ai-icons/claude-color.png', alt: 'Claude', label: 'Claude', href: 'https://claude.ai', target: '_blank' },
  { src: '/ai-icons/gemini-color.png', alt: 'Gemini', label: 'Gemini', href: 'https://gemini.google.com', target: '_blank' },
  { src: '/ai-icons/deepseek-color.png', alt: 'DeepSeek', label: 'DeepSeek', href: 'https://chat.deepseek.com', target: '_blank' },
  { src: '/ai-icons/qwen-color.png', alt: 'Qwen', label: 'Qwen', href: 'https://tongyi.aliyun.com', target: '_blank' },
  { src: '/ai-icons/alibaba-color.png', alt: 'Alibaba', label: 'Alibaba', href: 'https://www.alibaba.com', target: '_blank' },
  { src: '/ai-icons/meta-color.png', alt: 'Meta AI', label: 'Meta AI', href: 'https://www.meta.ai', target: '_blank' },
  { src: '/ai-icons/doubao-color.png', alt: 'Doubao', label: 'Doubao', href: 'https://www.doubao.com', target: '_blank' },
  { src: '/ai-icons/codex-color.png', alt: 'Codex', label: 'Codex', href: 'https://openai.com/codex', target: '_blank' },
  { src: '/ai-icons/claudecode-color.png', alt: 'Claude Code', label: 'Claude Code', href: 'https://claude.ai/code', target: '_blank' },
  { src: '/ai-icons/codebuddy-color.png', alt: 'CodeBuddy', label: 'CodeBuddy', href: 'https://codebuddy.ai', target: '_blank' },
  { src: '/ai-icons/copilot-color.png', alt: 'Copilot', label: 'Copilot', href: 'https://copilot.microsoft.com', target: '_blank' },
  { src: '/ai-icons/githubcopilot.png', alt: 'GitHub Copilot', label: 'GitHub Copilot', href: 'https://github.com/features/copilot', target: '_blank' },
  { src: '/ai-icons/hermesagent.png', alt: 'Hermes Agent', label: 'Hermes Agent', href: 'https://nousresearch.com', target: '_blank' },
  { src: '/ai-icons/pi.png', alt: 'Pi', label: 'Pi', href: 'https://pi.ai', target: '_blank' },
]

function onNavSelect(index) {
  // Placeholder handler — routes will be wired up in later iterations.
  console.log('nav select', index)
}
</script>

<template>
  <div class="home">
    <div class="home__bg" aria-hidden="true"></div>

    <Scanner
      class="home__scanner"
      color1="#1E3A8A"
      color2="#2563EB"
      color3="#93C5FD"
      :speed="0.45"
      :sweep-speed="0.22"
      :sweep-width="1.4"
      scan-direction="vertical"
      :mouse-interaction="true"
      :mouse-radius="0.6"
      :mouse-strength="0.55"
      :brightness="0.85"
      :opacity="0.8"
    />

    <div class="home__content">
      <header class="home__nav">
        <PillNav :items="navItems" logo="/cy.ico" :active-index="0" @select="onNavSelect" />
      </header>

      <section class="home__title" aria-label="CY->A AI Community">
        <TechText
          text="CY -> A AI Community"
          :font-weight="700"
          :font-size="150"
          :letter-spacing="-0.04"
          color="#EAF2FF"
          accent-color="#60A5FA"
          reveal="letter"
          :reach="180"
          :softness="0.7"
          line-style="dashed"
          :dash-length="4"
          :dash-gap="2"
          :stroke-width="1.5"
          :specks="14"
          selection
          labels
          draggable
          sweep
          :speed="1"
        />
        <p class="home__tagline">探索 AI 模型与智能体等相关开发 · Explore · Connect · Create</p>
      </section>

      <aside class="home__spiral" aria-label="热门 AI 模型与智能体">
        <InfiniteSpiral
          :items="spiralItems"
          animation-mode="all"
          direction="up"
          :speed="0.45"
          :radius="118"
          :card-width="64"
          :card-height="64"
          :vertical-spacing="56"
          :perspective="1000"
          :card-radius="18"
          :center-scale="1.2"
          :edge-fade="0.3"
          :edge-blur="5"
          :cards-per-turn="8"
          :grayscale="0"
          image-fit="contain"
          pause-on-hover
        />
      </aside>
    </div>
  </div>
</template>

<style scoped>
.home {
  position: relative;
  width: 100%;
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
}

.home__bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  background:
    radial-gradient(1100px 760px at 72% 18%, rgba(2, 19, 56, 0.22), transparent 62%),
    radial-gradient(900px 720px at 16% 82%, rgba(3, 64, 234, 0.2), transparent 62%),
    radial-gradient(700px 500px at 50% 46%, rgba(5, 13, 37, 0.18), transparent 70%),
    linear-gradient(158deg, #03060d 0%, #050b18 48%, #07122a 100%);
}

.home__scanner {
  position: absolute;
  inset: 0;
  z-index: 1;
}

.home__content {
  position: absolute;
  inset: 0;
  z-index: 2;
  pointer-events: none;
}

.home__nav {
  position: absolute;
  top: 26px;
  left: 26px;
  pointer-events: auto;
}

.home__title {
  position: absolute;
  left: 47%;
  top: 38%;
  transform: translate(-50%, -50%);
  width: min(54vw, 960px);
  height: 210px;
  pointer-events: auto;
  text-align: center;
}

.home__tagline {
  margin: 4px 0 0;
  font-size: 15px;
  letter-spacing: 0.14em;
  color: rgba(147, 197, 253, 0.5);
  font-weight: 400;
  white-space: nowrap;
}

.home__spiral {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 320px;
  pointer-events: auto;
}

@media (max-width: 1024px) {
  .home__title {
    left: 46%;
    width: min(72vw, 860px);
  }
  .home__spiral {
    width: 300px;
    right: 0;
  }
}

@media (max-width: 820px) {
  .home__title {
    left: 50%;
    top: 34%;
    width: 92vw;
    height: 170px;
  }
  .home__tagline {
    font-size: 12px;
    white-space: normal;
  }
  .home__spiral {
    display: none;
  }
}
</style>
