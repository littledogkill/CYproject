// ============ 社区占位数据（后续替换为真实接口） ============

import { ref } from 'vue'

// 用户名（slug）是路由标识，name 是展示名，authorUsername 关联帖子
export const users = [
  {
    username: 'cy',
    name: 'CY',
    handle: '@cy_ai',
    avatar: '/cy.ico',
    bio: '探索前沿 AI 模型与智能体',
    online: true,
    followers: 1280,
  },
  {
    username: 'xiaozhou',
    name: '小舟',
    handle: '@xiaozhou',
    avatar: null,
    bio: '前端 / 设计爱好者',
    online: true,
    followers: 342,
  },
  {
    username: 'nova',
    name: 'Nova',
    handle: '@nova',
    avatar: null,
    bio: 'WebGL & motion 探索者',
    online: false,
    followers: 856,
  },
  {
    username: 'ray',
    name: 'Ray',
    handle: '@ray_ray',
    avatar: null,
    bio: '图形学与性能优化',
    online: true,
    followers: 511,
  },
  {
    username: 'mio',
    name: 'Mio',
    handle: '@mio',
    avatar: null,
    bio: 'Vue 生态长期学习者',
    online: false,
    followers: 230,
  },
  {
    username: 'luna',
    name: 'Luna',
    handle: '@luna',
    avatar: null,
    bio: 'UI / UX 设计师',
    online: true,
    followers: 697,
  },
  {
    username: 'ake',
    name: '阿珂',
    handle: '@ake',
    avatar: null,
    bio: '关注图标与工程化',
    online: false,
    followers: 189,
  },
]

export const posts = [
  {
    id: 1,
    title: '用 Vue 3 + ogl 打造赛博朋克风格的个人主页',
    excerpt: '分享一个基于 WebGL 的扫描线、粒子与镜面按钮特效主页实现，附完整源码与踩坑记录。',
    author: 'CY',
    authorUsername: 'cy',
    category: 'work',
    views: 8421,
    likes: 1024,
    publishedAt: '2025-10-20T10:00:00',
    tags: ['Vue', 'WebGL'],
    content: [
      '分享一个基于 WebGL 的个人主页实现。整体视觉围绕「扫描线 + 粒子 + 镜面按钮」展开，核心效果都跑在 GPU 上，主线程只负责交互与路由，性能开销很小。',
      '背景用了自定义的 Scanner 组件做纵向扫描，配合多层径向渐变压出纵深；标题文字用 TechText 做逐字光带描边，字母之间还有可拖拽的连线。右侧是一个可无限滚动的螺旋卡片流，用来展示热门的 AI 模型与智能体。',
      '按钮这块花的时间最多。SpecularButton 用 ogl 渲染圆角矩形的 SDF，通过一个椭圆法线把高光做成「朝向 / 背向光源两侧同时亮」的对称镜面效果，指针靠近时高光会跟着转，移开后回到缓慢扫光。',
      '完整源码已整理到仓库：[点击查看源码](https://github.com/example/cy-project)，欢迎 star。',
      '螺旋卡片流里用到的图标示例：',
      '![OpenAI 图标](/ai-icons/openai.png)',
    ].join('\n\n'),
  },
  {
    id: 2,
    title: '请教：Tailwind v4 的任意值在动态 class 里为什么失效？',
    excerpt: '在 v-for 里拼接类名时，部分任意值类没有生成对应的 CSS，想问下正确的做法。',
    author: '小舟',
    authorUsername: 'xiaozhou',
    category: 'ask',
    views: 1560,
    likes: 88,
    publishedAt: '2025-10-25T14:30:00',
    tags: ['Tailwind', 'Vue'],
  },
  {
    id: 3,
    title: '分享一个基于 motion-v 的旋转文字组件集成经验',
    excerpt: '把 Vue Bits 的 RotatingText 组件接入现有项目，整理了几个容易踩的坑。',
    author: 'Nova',
    authorUsername: 'nova',
    category: 'work',
    views: 3902,
    likes: 466,
    publishedAt: '2025-10-18T09:15:00',
    tags: ['motion-v', '组件'],
  },
  {
    id: 4,
    title: 'OGL 渲染的按钮在高 DPI 屏上模糊，有解决方案吗？',
    excerpt: '用 ogl 做 SpecularButton 时，canvas 在 2x/3x 屏幕上边缘发虚，求指点。',
    author: 'Ray',
    authorUsername: 'ray',
    category: 'ask',
    views: 2340,
    likes: 121,
    publishedAt: '2025-10-22T20:45:00',
    tags: ['ogl', 'WebGL'],
  },
  {
    id: 5,
    title: '发布：一个可拖拽的无限螺旋导航组件',
    excerpt: '基于 CSS 3D transform 实现的无限螺旋卡片流，支持鼠标拖拽与惯性滚动。',
    author: 'CY',
    authorUsername: 'cy',
    category: 'work',
    views: 12750,
    likes: 1988,
    publishedAt: '2025-10-12T11:00:00',
    tags: ['CSS', '组件'],
  },
  {
    id: 6,
    title: 'Vue Router 5 的路由懒加载写法有变化吗？',
    excerpt: '升级到 vue-router 5 之后，之前用 component 动态导入的写法还能用吗？',
    author: 'Mio',
    authorUsername: 'mio',
    category: 'ask',
    views: 980,
    likes: 42,
    publishedAt: '2025-10-26T16:20:00',
    tags: ['vue-router'],
  },
  {
    id: 7,
    title: '分享：论坛页面的搜索与标签筛选交互设计',
    excerpt: '主标签分类 + 副标签排序 + 搜索按钮的组合布局，整理了一套可复用的模板。',
    author: 'Luna',
    authorUsername: 'luna',
    category: 'work',
    views: 2105,
    likes: 209,
    publishedAt: '2025-10-21T08:00:00',
    tags: ['UI', '论坛'],
  },
  {
    id: 8,
    title: 'Hugeicons 的图标在 Vue 里如何按需引入？',
    excerpt: '只想用几个图标，但全量包有点大，问下 tree-shaking 的正确姿势。',
    author: '阿珂',
    authorUsername: 'ake',
    category: 'ask',
    views: 1180,
    likes: 57,
    publishedAt: '2025-10-19T13:10:00',
    tags: ['图标', '性能'],
  },
]

export function findUser(username) {
  if (!username) return null
  return users.find((u) => u.username === username) || null
}

export function getPostsByUsername(username) {
  if (!username) return []
  return posts.filter((p) => p.authorUsername === username)
}

export function getStatsFor(username) {
  const list = getPostsByUsername(username)
  const user = findUser(username)
  return {
    posts: list.length,
    likes: list.reduce((sum, p) => sum + p.likes, 0),
    followers: user?.followers ?? 0,
  }
}

// ============ 当前登录用户（占位，后续替换为真实接口） ============
export const currentUser = {
  username: 'cy',
  name: 'CY',
  handle: '@cy_ai',
  avatar: '/cy.ico',
  email: 'cy@example.com',
  online: true,
}

// 当前用户点赞过的帖子（占位，后续由后端返回）
export const currentUserLikedPostIds = ref([2, 3, 7])

export function getLikedPosts() {
  return posts.filter((p) => currentUserLikedPostIds.value.includes(p.id))
}

export function isLikedPost(id) {
  return currentUserLikedPostIds.value.includes(id)
}

export function toggleLikePost(id) {
  if (isLikedPost(id)) {
    currentUserLikedPostIds.value = currentUserLikedPostIds.value.filter((x) => x !== id)
  } else {
    currentUserLikedPostIds.value.push(id)
  }
}

// ============ 关注关系（占位，后续由后端返回） ============
export const currentFollowing = ref(['xiaozhou', 'nova', 'luna'])
export const currentFollowers = ref(['ray', 'mio', 'xiaozhou', 'ake'])

export function isFollowing(username) {
  return currentFollowing.value.includes(username)
}

export function followUser(username) {
  if (username && !currentFollowing.value.includes(username)) {
    currentFollowing.value.push(username)
  }
}

export function unfollowUser(username) {
  currentFollowing.value = currentFollowing.value.filter((u) => u !== username)
}

export function getFollowingUsers() {
  return users.filter((u) => currentFollowing.value.includes(u.username))
}

export function getFollowerUsers() {
  return users.filter((u) => currentFollowers.value.includes(u.username))
}

// ============ 私聊会话（占位，后续由后端返回） ============
export const currentUserConversations = ref({
  xiaozhou: [
    {
      id: 1,
      role: 'peer',
      content: '你那个 WebGL 按钮的 demo 方便发我看下吗？',
      time: '2025-10-26T10:12:00',
    },
    { id: 2, role: 'user', content: '可以，晚点我整理下发你。', time: '2025-10-26T10:15:00' },
  ],
  nova: [
    { id: 3, role: 'peer', content: '你的螺旋组件我引用了，谢谢！', time: '2025-10-25T21:40:00' },
  ],
  ray: [
    { id: 4, role: 'peer', content: '高 DPI 那个问题解决了，多谢。', time: '2025-10-24T18:02:00' },
    { id: 5, role: 'user', content: '不客气，解决了就好。', time: '2025-10-24T18:10:00' },
  ],
})

export function getMessageRooms() {
  return Object.keys(currentUserConversations.value)
    .map((username) => {
      const user = findUser(username)
      const msgs = currentUserConversations.value[username] || []
      return {
        username,
        name: user?.name || username,
        avatar: user?.avatar || null,
        online: user?.online ?? false,
        lastMessage: msgs[msgs.length - 1]?.content || '',
        lastTime: msgs[msgs.length - 1]?.time || '',
        count: msgs.length,
      }
    })
    .sort((a, b) => new Date(b.lastTime) - new Date(a.lastTime))
}

export function getMessagesFor(username) {
  return currentUserConversations.value[username] || []
}

export function addMessage(username, role, content) {
  if (!username || !content) return
  if (!currentUserConversations.value[username]) {
    currentUserConversations.value[username] = []
  }
  const msgs = currentUserConversations.value[username]
  const id = msgs.length ? msgs[msgs.length - 1].id + 1 : 1
  msgs.push({ id, role, content, time: new Date().toISOString() })
}

export function sendMessage(username, content) {
  addMessage(username, 'user', content)
}

export function replyMessage(username, content) {
  addMessage(username, 'peer', content)
}
