import { AUTH_TOKEN_KEY, clearAuthToken } from './auth'

// 后端返回的图片/头像 URL 会命中这些路径前缀。
// 统一转换成 /api 相对路径，以便走 Vite 代理、带上 Authorization 头，
// 同时避免开发环境下直接跨域请求后端。
const BACKEND_PATH_PREFIXES = [
  '/uploads/',
  '/avatarFiles/',
  '/avatars/',
  '/avatar/',
  '/files/',
]

// 缓存已拉取的 Blob（同一个图只需下载一次），objectURL 由调用方各自创建并回收。
const blobCache = new Map()

function normalizeHostname(hostname) {
  // 本地开发时 localhost / 127.0.0.1 / ::1 视为同一源
  if (hostname === 'localhost' || hostname === '127.0.0.1' || hostname === '::1') {
    return 'localhost'
  }
  return hostname
}

function isSameHostname(hostname) {
  try {
    return normalizeHostname(hostname) === normalizeHostname(window.location.hostname)
  } catch {
    return false
  }
}

/**
 * 把后端静态资源地址转成 /api 代理地址。
 * 例如 http://localhost:8080/uploads/a.png -> /api/uploads/a.png
 * 外部图片（https://example.com/...）保持原样。
 */
export function toProxyUrl(src) {
  if (!src || /^(blob:|data:)/.test(src)) return src
  if (src.startsWith('/api/')) return src
  try {
    const u = new URL(src, window.location.origin)
    if (
      isSameHostname(u.hostname) &&
      BACKEND_PATH_PREFIXES.some((prefix) => u.pathname.startsWith(prefix))
    ) {
      return '/api' + u.pathname + u.search
    }
  } catch {
    // 解析失败时原样返回
  }
  return src
}

async function fetchBlob(url) {
  const token = localStorage.getItem(AUTH_TOKEN_KEY)?.trim()
  const headers = token ? { Authorization: `Bearer ${token}` } : {}
  const res = await fetch(url, { headers })
  if (res.status === 401) {
    clearAuthToken()
    window.location.href = '/login'
    throw new Error('unauthorized')
  }
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.blob()
}

function getImageBlob(url) {
  let entry = blobCache.get(url)
  if (!entry) {
    entry = fetchBlob(url).catch((error) => {
      blobCache.delete(url)
      throw error
    })
    blobCache.set(url, entry)
  }
  return entry
}

/**
 * 解析图片地址：
 * - 后端受保护资源：带 token 通过 /api 代理拉取，返回 objectURL（blob:）
 * - 外部图片 / data: / blob:：原样返回，交给 <img> 直接加载
 */
export async function loadAuthedImage(src) {
  if (!src) return ''
  if (/^(blob:|data:)/.test(src)) return src

  let fetchUrl = src
  if (src.startsWith('/api/')) {
    fetchUrl = src
  } else {
    const proxied = toProxyUrl(src)
    if (proxied === src) return src // 非后端资源，直接加载
    fetchUrl = proxied
  }

  try {
    const blob = await getImageBlob(fetchUrl)
    return URL.createObjectURL(blob)
  } catch {
    // 拉取失败时回退到原始 URL，交给浏览器处理
    return src
  }
}

export function revokeImageUrl(url) {
  if (url && url.startsWith('blob:')) URL.revokeObjectURL(url)
}

/**
 * 指令：v-authed-src
 * 用于 <img> 头像等场景，异步解析受保护的图片地址并回填 src。
 */
export const vAuthedSrc = {
  async mounted(el, binding) {
    el.__authedSrcValue = binding.value
    el.__authedSrcRequest = (el.__authedSrcRequest || 0) + 1
    const requestId = el.__authedSrcRequest
    const url = await loadAuthedImage(binding.value)
    if (el.__authedSrcRequest !== requestId) {
      revokeImageUrl(url)
      return
    }
    if (url && url.startsWith('blob:')) el.__authedObjectUrl = url
    if (url) el.setAttribute('src', url)
  },
  async updated(el, binding) {
    if (el.__authedSrcValue === binding.value) return
    el.__authedSrcValue = binding.value
    el.__authedSrcRequest = (el.__authedSrcRequest || 0) + 1
    const requestId = el.__authedSrcRequest
    if (el.__authedObjectUrl) {
      revokeImageUrl(el.__authedObjectUrl)
      el.__authedObjectUrl = ''
    }
    el.removeAttribute('src')
    const url = await loadAuthedImage(binding.value)
    if (el.__authedSrcRequest !== requestId) {
      revokeImageUrl(url)
      return
    }
    if (url && url.startsWith('blob:')) el.__authedObjectUrl = url
    if (url) el.setAttribute('src', url)
  },
  unmounted(el) {
    if (el.__authedObjectUrl) revokeImageUrl(el.__authedObjectUrl)
  },
}
