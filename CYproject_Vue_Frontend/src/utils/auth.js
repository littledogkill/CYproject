export const AUTH_TOKEN_KEY = 'auth_token'

export function hasAuthToken() {
  return Boolean(localStorage.getItem(AUTH_TOKEN_KEY)?.trim())
}

export function setAuthToken(token) {
  if (typeof token !== 'string' || !token.trim()) {
    throw new Error('登录响应未提供有效 token')
  }
  localStorage.setItem(AUTH_TOKEN_KEY, token.trim())
}

export function clearAuthToken() {
  localStorage.removeItem(AUTH_TOKEN_KEY)
}
