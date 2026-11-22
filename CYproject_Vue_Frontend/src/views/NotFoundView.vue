<template>
  <div class="not-found">
    <h1 class="code">404</h1>
    <p class="desc">页面走丢了~ 你访问的地址不存在</p>
    <div class="actions">
      <button @click="goHome">返回首页</button>
      <button @click="goBack">返回上一页</button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => router.push({ name: 'home' })
const goBack = () => (window.history.length > 1 ? router.back() : router.push({ name: 'home' }))
</script>

<style scoped>
.not-found {
  min-height: 60vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  text-align: center;
}
.code {
  font-size: 96px;
  font-weight: 800;
  margin: 0;
  line-height: 1;
}
.desc {
  color: #e1c5c5;
  margin: 0;
}
.actions {
  display: flex;
  gap: 12px;
}
.actions button {
  padding: 8px 20px;
  border: 1px solid #ed0808;
  border-radius: 8px;
  background: #1c0000;
  cursor: pointer;
  transition: all 0.2s;
}
.actions button:hover {
  border-color: #620404;
  background: #f5f5f5;
}
</style>