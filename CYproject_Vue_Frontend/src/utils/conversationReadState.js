const READ_STATE_PREFIX = 'conversation_read_state:'

function readMap(ownerId) {
  const key = `${READ_STATE_PREFIX}${ownerId || 'default'}`
  try {
    return JSON.parse(localStorage.getItem(key) || '{}')
  } catch {
    return {}
  }
}

function writeMap(ownerId, map) {
  const key = `${READ_STATE_PREFIX}${ownerId || 'default'}`
  localStorage.setItem(key, JSON.stringify(map))
}

export function markConversationReadLocally(ownerId, peerKey, lastMessageAt) {
  if (!ownerId || !peerKey) return
  const map = readMap(ownerId)
  map[peerKey] = lastMessageAt || new Date().toISOString()
  writeMap(ownerId, map)
}

export function getConversationLastReadAt(ownerId, peerKey) {
  if (!ownerId || !peerKey) return ''
  return readMap(ownerId)[peerKey] || ''
}

export function hasUnreadConversation(room, ownerId) {
  if (!room) return false

  // 后端如果已经返回未读数，优先采用
  if (Number(room.count) > 0) return true

  const peerKey = room.username || room.id
  if (!peerKey || !room.lastTime) return false

  const lastReadAt = getConversationLastReadAt(ownerId, peerKey)
  if (!lastReadAt) return true

  return new Date(room.lastTime).getTime() > new Date(lastReadAt).getTime()
}
