# CY

一个关于 AI / Agent 及其衍生产品的讨论与资源分享社区。

**CY 想解决的问题**：GitHub 上有很多优秀的 AI 项目，但它们的 README 往往是英文、术语多、篇幅长，普通人很难快速判断「这东西对我有没有用」。CY 用自研 Agent 把这些项目自动整理成通俗易懂的中文帖子，同时提供社区功能，方便大家浏览、搜索、讨论和发布。

> 目前项目仍在开发阶段，暂未部署公开访问地址。

## 主要功能

- **Agent 自动整理项目**：Python Agent 从 GitHub 拉取 AI / Agent 相关仓库，读取 README、筛选、总结，最后以帖子形式发布到站内。不用翻墙、不用啃英文文档，看摘要就能判断要不要深入了解。
- **浏览与搜索**：作品帖（work）和问题帖（ask）分开，支持按时间、浏览量、点赞数排序。
- **发帖**：支持简易 Markdown 语法、图片上传；发布的资源内容会经过语言模型审查，保证与站内主题相关。
- **互动**：点赞、关注、评论、私信。

## 使用方式

1. 注册 / 登录账号。
2. 浏览项目帖，或搜索感兴趣的内容。
3. 发布问题或作品，参与点赞、关注、评论、私信。

## 技术栈

| 部分 | 技术 |
|------|------|
| 前端 | Vue 3 + Vite + Tailwind CSS |
| 后端 | Spring Boot + JPA + MySQL + Redis |
| Agent | Python + OpenAI Function Calling（DeepSeek） |

> 更详细的实现说明见 **[TECHNICAL.md](./TECHNICAL.md)**。

## 本地运行

### 环境要求

- Java 17+
- Node.js 22.18+（或 24.12+）
- MySQL 8、Redis
- Python 3.x
- DeepSeek API Key

### 后端

```bash
cd CYproject_Spring_Backend
mvn spring-boot:run
```

默认连接 <code>localhost:3306/cydb</code>（MySQL）和 <code>localhost:6379</code>（Redis）。相关配置可通过环境变量覆盖，见 <code>src/main/resources/application.yaml</code>：

| 环境变量 | 说明 |
|----------|------|
| <code>DB_URL</code> / <code>DB_USERNAME</code> / <code>DB_PASSWORD</code> | MySQL 连接信息 |
| <code>REDIS_HOST</code> / <code>REDIS_PORT</code> | Redis 连接信息 |
| <code>DEEPSEEK_API_KEY</code> | AI 审查所用模型的 API Key |
| <code>JWT_SECRET</code> | JWT 签名密钥 |

### 前端

```bash
cd CYproject_Vue_Frontend
npm install
npm run dev
```

开发服务器会把 <code>/api</code> 请求代理到 <code>http://localhost:8080</code>（配置见 <code>vite.config.js</code>）。

### Agent（可选）

```bash
cd CYproject_python_Plugin
python main.py
```

用于自动拉取、总结并发布 GitHub 项目。运行前需要在 <code>main.py</code> 中填入 DeepSeek 的 API Key。

## 项目结构

```
CYproject/
├── CYproject_Spring_Backend/   # 后端（Spring Boot）
├── CYproject_Vue_Frontend/     # 前端（Vue 3）
├── CYproject_python_Plugin/    # Python Agent
├── TECHNICAL.md                # 技术实现文档
└── image/                      # 文档配图
```

## 许可证

待补充。
