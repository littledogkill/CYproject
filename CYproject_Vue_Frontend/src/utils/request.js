import axios from 'axios'
import { AUTH_TOKEN_KEY, clearAuthToken } from './auth'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

function isAuthRequest(url = '') {
  return /\/begain\/(login|register)(?:\?|$)/.test(url)
}

function handleUnauthorized(url) {
  if (!isAuthRequest(url)) {
    clearAuthToken()
    window.location.href = '/login'
  }
}

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(AUTH_TOKEN_KEY)?.trim()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

request.interceptors.response.use(
  (response) => {
    const envelope = response.data
    if (
      !envelope ||
      !Number.isInteger(envelope.code) ||
      typeof envelope.message !== 'string' ||
      !Object.prototype.hasOwnProperty.call(envelope, 'data')
    ) {
      throw new Error('后端响应格式无效，必须包含 code、message 和 data')
    }
    if (envelope.code !== 0) {
      if (envelope.code === 401) {
        handleUnauthorized(response.config.url)
      }
      throw new Error(envelope.message || `请求失败，业务状态码 ${envelope.code}`)
    }
    return envelope.data
  },
  (error) => {
    if (error.response?.status === 401) {
      handleUnauthorized(error.config?.url)
    }
    return Promise.reject(error)
  },
)

export default request
