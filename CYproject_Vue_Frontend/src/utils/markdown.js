import MarkdownIt from 'markdown-it'

// 完整 Markdown 渲染：
// - html: false —— 原始 HTML 会被转义，防止 XSS
// - linkify: true —— 自动识别裸链接
// - breaks: true —— 单个换行也转为 <br>，贴近编辑器里的输入体验
// markdown-it 默认会拦截 javascript:/vbscript:/file:/data: 等危险协议
// （仅放行 data:image/(gif|png|jpeg|webp)，用于本地 base64 图片）。
const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
})

const escapeHtml = (str) => md.utils.escapeHtml(str)

// 无协议的地址（如 example.com、www.example.com）补全协议，
// 否则会被浏览器当成相对路径拼到当前 /post/xxx 下面。
function normalizeHref(href) {
  if (!href) return href
  if (/^(https?:|mailto:|tel:|ftp:|#|\/|\.{1,2}\/|blob:|data:)/i.test(href)) return href
  return `https://${href}`
}

// 链接一律在新窗口打开，避免在 /post/xxx 下拼接相对地址
const defaultLinkOpen =
  md.renderer.rules.link_open ||
  ((tokens, idx, options, env, self) => self.renderToken(tokens, idx, options))

md.renderer.rules.link_open = (tokens, idx, options, env, self) => {
  const token = tokens[idx]
  const href = token.attrGet('href')
  if (href) token.attrSet('href', normalizeHref(href))
  token.attrSet('target', '_blank')
  token.attrSet('rel', 'noopener noreferrer')
  return defaultLinkOpen(tokens, idx, options, env, self)
}

// 图片先输出 data-src（不直接写 src），由 MarkdownView 用带鉴权的
// fetch 拉取后再回填 src，避免 <img> 直接请求受认证保护的 /uploads/** 导致 401。
md.renderer.rules.image = (tokens, idx, options, env, self) => {
  const token = tokens[idx]
  const src = token.attrGet('src') || ''
  const alt = escapeHtml(self.renderInlineAsText(token.children, options, env))
  const title = token.attrGet('title')
  let html = `<img data-src="${escapeHtml(src)}" alt="${alt}" loading="lazy"`
  if (title) html += ` title="${escapeHtml(title)}"`
  html += '>'
  return html
}

export function renderMarkdown(src) {
  return md.render(String(src ?? ''))
}
