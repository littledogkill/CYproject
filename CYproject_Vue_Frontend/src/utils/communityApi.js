import request from '@/utils/request'

function payloadOf(response, key) {
  return response?.[key] ?? response
}

function normalizeUser(value) {
  const user = payloadOf(value, 'user')
  return {
    ...user,
    id: user.id != null ? String(user.id) : undefined,
    username: user.username,
    name: user.name || user.username,
    handle: user.handle || (user.username ? `@${user.username}` : ''),
    avatar: user.avatar || null,
    online: Boolean(user.online),
    stats: user.stats || { posts: 0, likes: 0, followers: 0 },
  }
}

function normalizePost(value) {
  const post = payloadOf(value, 'post')
  const author = post.author
  const authorName = typeof author === 'string' ? author : author?.name
  const authorUsername =
    post.authorUsername || (typeof author === 'object' ? author?.username : undefined)
  const authorId = typeof author === 'object' && author?.id != null ? String(author.id) : undefined

  return {
    ...post,
    id: String(post.id),
    author: authorName || authorUsername || '未知用户',
    authorUsername,
    authorId,
    authorAvatar: typeof author === 'object' ? author.avatar || null : null,
    category: post.category,
    excerpt: post.excerpt || '',
    tags: Array.isArray(post.tags) ? post.tags : [],
    views: Number(post.views || 0),
    likes: Number(post.likes || 0),
    likedByMe: Boolean(post.likedByMe),
  }
}

function normalizeComment(value) {
  const comment = value.comment || value
  const author = comment.author
  return {
    ...comment,
    id: String(comment.id),
    author: typeof author === 'string' ? author : author?.name || author?.username || '未知用户',
    authorUsername: typeof author === 'object' ? author?.username : undefined,
    authorId: typeof author === 'object' && author?.id != null ? String(author.id) : undefined,
    avatar: typeof author === 'object' ? author?.avatar || null : null,
    time: comment.createdAt || comment.time,
  }
}

function normalizeMessage(value, currentUsername) {
  const message = value.message || value
  return {
    ...message,
    id: String(message.id),
    role: message.senderUsername === currentUsername ? 'user' : 'peer',
    content: message.content,
    time: message.createdAt || message.time,
  }
}

function listResponse(response, mapper) {
  if (!Array.isArray(response?.items)) {
    throw new Error('后端列表响应缺少 items 数组')
  }
  if (
    !response.pagination ||
    !Number.isInteger(response.pagination.page) ||
    !Number.isInteger(response.pagination.pageSize) ||
    !Number.isInteger(response.pagination.totalItems) ||
    !Number.isInteger(response.pagination.totalPages)
  ) {
    throw new Error('后端列表响应缺少有效 pagination 信息')
  }
  const items = response.items
  return {
    items: items.map(mapper),
    pagination: response.pagination,
  }
}

export async function signIn(payload) {
  return request.post('/begain/login', payload)
}

export async function signUp(payload) {
  return request.post('/begain/register', payload)
}

export async function getCurrentUser() {
  const response = await request.get('/users/me')
  return normalizeUser(response)
}

export async function updateCurrentUser(payload) {
  const response = await request.patch('/users/me', payload)
  return normalizeUser(response)
}

export async function getUser(userid) {
  const response = await request.get(`/users/${encodeURIComponent(userid)}`)
  return normalizeUser(response)
}

export async function getPosts(params = {}) {
  const response = await request.get('/posts', { params })
  return listResponse(response, normalizePost)
}

export async function getUserPosts(userid, params = {}) {
  const response = await request.get(`/users/${encodeURIComponent(userid)}/posts`, { params })
  return listResponse(response, normalizePost)
}

export async function getLikedPosts(params = {}) {
  const response = await request.get('/users/me/likes', { params })
  return listResponse(response, normalizePost)
}

export async function getPost(postId) {
  const response = await request.get(`/posts/${encodeURIComponent(postId)}`)
  return normalizePost(response)
}

export async function createPost(payload) {
  const response = await request.post('/posts', payload)
  return normalizePost(response)
}

export async function deletePost(postId) {
  return request.delete(`/posts/${encodeURIComponent(postId)}`)
}

export async function setPostLiked(postId, liked) {
  return request({
    method: liked ? 'put' : 'delete',
    url: `/posts/${encodeURIComponent(postId)}/like`,
  })
}

export async function getPostComments(postId, params = {}) {
  const response = await request.get(`/posts/${encodeURIComponent(postId)}/comments`, { params })
  return listResponse(response, normalizeComment)
}

export async function createPostComment(postId, content) {
  const response = await request.post(`/posts/${encodeURIComponent(postId)}/comments`, { content })
  return normalizeComment(response)
}

export async function uploadPostImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/uploads/images', formData, { timeout: 60000 })
}

export async function getFollowList(type, params = {}) {
  const path = type === 'following' ? '/users/me/following' : '/users/me/followers'
  const response = await request.get(path, { params })
  return listResponse(response, normalizeUser)
}

export async function setFollowing(userid, following) {
  return request({
    method: following ? 'put' : 'delete',
    url: `/users/${encodeURIComponent(userid)}/follow`,
  })
}

export async function getConversations(params = {}) {
  const response = await request.get('/conversations', { params })
  return listResponse(response, (room) => ({
    ...room,
    id: room.id != null ? String(room.id) : undefined,
    username: room.username,
    name: room.name || room.username,
    avatar: room.avatar || null,
    lastTime: room.lastMessageAt || room.lastTime || '',
    count: Number(room.unreadCount || 0),
  }))
}

export async function getConversationMessages(userid, currentUsername, params = {}) {
  const response = await request.get(`/conversations/${encodeURIComponent(userid)}/messages`, {
    params,
  })
  if (!Array.isArray(response?.items)) {
    throw new Error('后端消息响应缺少 items 数组')
  }
  return response.items.map((message) => normalizeMessage(message, currentUsername))
}

export async function markConversationRead(userid) {
  return request.post(`/conversations/${encodeURIComponent(userid)}/read`)
}

export async function sendConversationMessage(userid, content, currentUsername) {
  const response = await request.post(`/conversations/${encodeURIComponent(userid)}/messages`, {
    content,
  })
  return normalizeMessage(response, currentUsername)
}
