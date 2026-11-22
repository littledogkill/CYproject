import { uploadPostImage } from './communityApi'

const MAX_DIMENSION = 1600
const JPEG_QUALITY = 0.82
const MAX_UPLOAD = 10 * 1024 * 1024 // 与后端一致，10MB

/**
 * 判断 canvas 中是否有透明像素（alpha < 255）。
 * 有透明就输出 PNG 保留透明，否则输出 JPEG（体积更小）。
 */
function detectAlpha(ctx, w, h) {
  try {
    const data = ctx.getImageData(0, 0, w, h).data
    for (let i = 3; i < data.length; i += 4) {
      if (data[i] < 255) return true
    }
    return false
  } catch {
    // 读取不到像素时按“有透明”处理，避免误压成 JPEG 后丢失透明
    return true
  }
}

/**
 * 用 canvas 压缩图片：等比缩放到最长边 <= maxDimension，
 * 输出后端可解析的 JPEG 或 PNG。
 *
 * 注意：后端使用 JDK 自带的 ImageIO 解析图片（仅支持 JPEG/PNG/GIF/BMP/WBMP），
 * 不支持 WebP。因此这里不要输出 WebP，否则后端 ImageIO.read 返回 null，
 * 会抛出 BAD_REQUEST。
 */
export function compressImage(file, { maxDimension = MAX_DIMENSION } = {}) {
  return new Promise((resolve, reject) => {
    if (!file || !file.type.startsWith('image/')) {
      reject(new Error('非图片文件'))
      return
    }

    const objectUrl = URL.createObjectURL(file)
    const img = new Image()

    img.onload = () => {
      URL.revokeObjectURL(objectUrl)

      const scale = Math.min(1, maxDimension / Math.max(img.naturalWidth, img.naturalHeight))
      const w = Math.max(1, Math.round(img.naturalWidth * scale))
      const h = Math.max(1, Math.round(img.naturalHeight * scale))

      const canvas = document.createElement('canvas')
      canvas.width = w
      canvas.height = h
      const ctx = canvas.getContext('2d')
      if (!ctx) {
        reject(new Error('无法创建画布'))
        return
      }

      ctx.drawImage(img, 0, 0, w, h)

      const hasAlpha = detectAlpha(ctx, w, h)
      const mime = hasAlpha ? 'image/png' : 'image/jpeg'

      if (mime === 'image/jpeg') {
        // 透明图片转 JPEG 需要先铺白底，否则透明区域会变黑
        ctx.fillStyle = '#ffffff'
        ctx.fillRect(0, 0, w, h)
        ctx.drawImage(img, 0, 0, w, h)
      }

      const dataUrl = canvas.toDataURL(mime, mime === 'image/jpeg' ? JPEG_QUALITY : undefined)
      resolve({ dataUrl, width: w, height: h, mime })
    }

    img.onerror = () => {
      URL.revokeObjectURL(objectUrl)
      reject(new Error('图片解码失败'))
    }

    img.src = objectUrl
  })
}

/**
 * 压缩图片后上传，返回后端提供的稳定 URL。
 */
export async function uploadImage(file) {
  const { dataUrl, mime } = await compressImage(file)
  const blob = await fetch(dataUrl).then((response) => response.blob())
  if (blob.size > MAX_UPLOAD) {
    throw new Error('图片压缩后仍超过 10MB，请换一张更小的图片')
  }
  const extension = mime === 'image/png' ? 'png' : 'jpg'
  const result = await uploadPostImage(new File([blob], `upload.${extension}`, { type: mime }))
  if (typeof result?.url !== 'string' || !result.url.trim()) {
    throw new Error('图片上传响应缺少 url')
  }
  return result.url
}
