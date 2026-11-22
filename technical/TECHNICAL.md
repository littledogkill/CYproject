# 基于 -<code>Agent添加网页资源</code>- 的<code>AI及其衍生产品</code>讨论与资源分享论坛网站

**本网页面向 AI/Agent 及其衍生项目的了解者，针对术语翻译不专业、项目复杂度高、论述方向过宽等障碍，搭建用户与项目之间的桥梁：以简洁明了的描述提供项目介绍，用户确认项目对自己有用后，可通过帖子中的项目地址深入探究。**

**项目实现功能**：前端基于 **Vue** 框架构建，后端由 **SpringBoot** 框架构建并严格遵循 **MVC 软件架构设计模式**，另以 **Python 脚本**实现 Agent 程序：网页端实现<code>登录/注册</code>、<code>帖子浏览与发布</code>、<code>帖子搜索</code>、<code>筛选</code>、<code>个人主页</code>、<code>关注</code>、<code>点赞</code>、<code>评论及私聊</code>等论坛常见功能；Python 脚本作为填充系统资源的 Agent 程序，实现<code>项目资源获取</code>、<code>项目筛选</code>、<code>项目总结</code>、<code>项目发布</code>全流程，让网页真正做到起作用。网站虽聚焦于 AI 及其衍生产品的讨论与资源分享，但资源分享暂不提供下载功能，而是通过 URL 路由到其他网站（主要是 GitHub）进行对应下载；对于用户上传的<code>资源内容</code>，网页会通过接入的<code>语言模型</code>对其进行审查，以保证资源与网页主题相关，并在此过程中限制用户发布次数，节约 token 成本；系统提供的<code>发帖功能</code>支持简易的<code>markdown</code>标记，使帖子更加生动形象；系统还会提供<code>线上非即时聊天</code>功能，让用户之间能够进行交流讨论。

## 🚀 关键或亮点功能的实现

<p align="center">
  <img src="https://img.shields.io/badge/Python-Agent-3776AB?style=for-the-badge&logo=python&logoColor=white" alt="Python Agent" />
  <img src="https://img.shields.io/badge/SpringBoot-AI%20Review-6DB33F?style=for-the-badge&logo=spring&logoColor=white" alt="AI Review" />
  <img src="https://img.shields.io/badge/Redis-Rate%20Limit-DC382D?style=for-the-badge&logo=redis&logoColor=white" alt="Rate Limit" />
  <img src="https://img.shields.io/badge/JWT-User%20Context-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white" alt="JWT Context" />
  <img src="https://img.shields.io/badge/Vue-Effects-4FC08D?style=for-the-badge&logo=vuedotjs&logoColor=white" alt="Vue Effects" />
</p>

> 本板块聚焦项目中最具代表性的 5 个技术实现，覆盖 **Python Agent**、**后端 AI 审查**、**发帖限流**、**JWT 身份上下文** 与 **前端特效**。

### 📑 目录

| # | 功能 | 核心技术 |
|:-:|:-----|:--------|
| 1 | [Python Agent 的实现](#1-python-agent-的实现) | OpenAI Function Calling |
| 2 | [后端 AI 审查的实现](#2-后端-ai-审查的实现) | Servlet Filter + LLM |
| 3 | [多次发帖限制的实现](#3-多次发帖限制的实现) | Redis 计数 |
| 4 | [token 解析后 userid 保留](#4-后端-token-解析后-userid-保留的实现) | ThreadLocal |
| 5 | [前端特效页面的实现](#5-前端特效页面的实现) | vue-bits.dev |

---

### 1. Python Agent 的实现

> [!NOTE]
> Agent 的核心是 **OpenAI 的函数调用（Function Calling）**：将自定义 Python 函数封装成 AI 可调用的工具，让模型自主决策并按需调用。

**① 定义工具参数模型**

用 Pydantic 为 AI 描述工具的作用与调用时机：

```python
class ask_tool_one_params(BaseModel):
    """这个为你和用户之间提供交互工具，你发送信息给用户，用户返回信息给你
    当你对某一步骤有疑问时，可以调用，具体如下：
    你不知道用户想要的输出时；你发现你解决不了的问题时；还有其他，只要你觉得有疑问的地方，都应该调用这个工具
    """
    ask: str = Field(description= "你输入给用户的内容")
```

**② 关联工具与函数**

将工具类与具体函数绑定，注册成 AI 可调用的工具集：

```python
TOOLS = [
    pydantic_function_tool(ask_tool_one_params, name="ask_tool_one"),
    .......  #其他工具
]

TOOL_REGISTRY = {
    "ask_tool_one": AskToolOne.ask_tool_one,
    .......  #其他工具
}
```

**③ 工具清单（共 10 个）**

| 工具 | 作用 |
|------|------|
| <code>ask_tool_one</code> | agent 遇到问题时与用户的询问交互工具 |
| <code>reset_tool_one</code> | 重置记录中被筛选但是没被使用的记录 |
| <code>get_message_tool_one</code> | agent 获得记录统计数据的工具 |
| <code>search_tool_one</code> | 拉取 github 中指定主题仓库的工具 |
| <code>select_tool_one</code> | 筛选符合要求的记录 |
| <code>search_tool_two</code> | 获取项目的 readme 介绍 |
| <code>de_markdown_tool_one</code> | 将 readme 拆解、缩减，以免传入过大文本量 |
| <code>summarize_tool_one</code> | 将拆解的文本总结成 CY 网页合适的帖子格式 |
| <code>send_tool_one</code> | 以 CY 网页用户的身份发送帖子 |
| <code>send_tool_two</code> | 获取 token |

**④ Agent 工作流程**

<p align="center">
  <img src="image/agentwork.svg" alt="Agent 工作流程" />
</p>

---

### 2. 后端 AI 审查的实现

> [!TIP]
> 审查发生在**请求到达 Controller 之前**，通过自定义 Filter 拦截请求，在请求流被消费前完成 AI 审核。

**① 缓存请求体（一次性读取）**

```java
public CachedBodyRequestWrapper(HttpServletRequest request) throws IOException {
    super(request);
    // 一次性把原始流读完，缓存到内存
    this.cachedBody = request.getInputStream().readAllBytes();
}
```

**② 重建请求流**

> [!IMPORTANT]
> Spring 中的请求流是「一次性」的，被拦截读取后就会消失，因此必须重新构造请求流。

```java
public ServletInputStream getInputStream() {
    ByteArrayInputStream bais = new ByteArrayInputStream(cachedBody);
    return new ServletInputStream() {
        //构造请求
    };
}
```

**③ 在两者之间执行 AI 审查**

```java
//ai审查开始，aiReview就是我们定义的AI审查工具
if ("work".equals(category)) {
    String result = aiReview.askForWork(userInput);
} else if ("ask".equals(category)) {
    String result = aiReview.askForWork(userInput);
}
```

**④ AI 审查主体**

```java
public String askForWork(String text) {
    String TIP = """
                AI提示词
            """;
    return chatClient.prompt()
            .system(TIP)
            .user(text)
            .call()
            .content();          // 直接返回纯文本
}
```

---

### 3. 多次发帖限制的实现

> [!NOTE]
> 基于 **Redis** 非关系数据库实现发帖限流：每次发帖后计数 +1，达到阈值即抛出异常，次日自动重置。

```java
String today = LocalDate.now(ZoneId.of("Asia/Shanghai")).toString();
// 设置记录
String limitKeyPost = "post:limit:" + userid + ":" + today;
// 到当天结束的剩余秒数
long secondsUntilEndOfDay = Duration.between(
        LocalDateTime.now(),
        LocalDate.now().plusDays(1).atStartOfDay()
).getSeconds();
// 发帖后
Long postCount = postRedisTemplate.opsForValue().increment(limitKeyPost);
// 之后如果还postCount超过阈值，就会抛出异常
```

---

### 4. 后端 token 解析后 userid 保留的实现

> [!NOTE]
> 通过 <code>ThreadLocal</code> 为每个请求线程独立保存 userid，线程之间互不干扰，Service 层可直接获取当前用户。

**① 定义线程上下文**

```java
public final class UserContext {
    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();
    // 声明一个线程本地变量，为每个线程单独保存userid，线程之间互不干扰。
}
```

**② 在 JWT 拦截时写入 userid**

在 JWT 拦截请求时解析出 <code>userid</code>，存入 <code>USER_ID</code>，整个线程链路即可直接使用。

---

### 5. 前端特效页面的实现

> [!TIP]
> 前端动态特效组件均来自组件网站 [vue-bits.dev](https://www.vue-bits.dev/)。

<p align="center">
  <img src="image/vue-bits.png" alt="vue-bits 组件网站" width="640" />
</p>

**使用方式**：在项目文件内打开智能体，粘贴网站提供的提示词，即可完成组件开发。

## 各板块概括

### 一.前端

#### 前端主要Vue组件（网页或组件）

1. <code>HomeView.vue</code>网站的主页面，包含路由到登录页面等基础功能

2. <code>AuthView.vue</code>网页的登录注册页面，登录与注册两页面可以相互路由并实现登录注册|

3. <code>ProfileView.vue</code>用户的主页，展示用户的相关信息，用户与其他用户的关系，用户的相关浏览信息，以及跳转到其他页面等功能

4. <code>ForumView.vue</code>用于展示论坛的相关帖子，并且提供搜索功能，以及用户互动等功能

5. <code>FollowListView.vue</code>用于展示关注信息的组件

6. <code>ChatView.vue</code>用于用户间私聊的页面

7. <code>CreatePostView.vue</code>用于用户发布帖子的网页

8. <code>TalksView.vue</code>用于展示私聊列表的组件

9. <code>UserProfileView.vue</code>用于展示访问其他用户的页面

10. <code>PostDetailView.vue</code>用于展示帖子详情的页面

### 二.接口

#### 接口清单

| 功能             | 方法与路径                                | 认证 | 用途                                   |
| ---------------- | ----------------------------------------- | ---: | -------------------------------------- |
| 注册             | <code>POST /begain/register</code>                   |   否 | 创建账号并返回 token、用户资料         |
| 登录             | <code>POST /begain/login</code>                      |   否 | 使用邮箱登录                           |
| 当前用户资料     | <code>GET /users/me</code>                           |   是 | 初始化个人主页及账号设置               |
| 更新当前用户资料 | <code>PATCH /users/me</code>                         |   是 | 暂未实现，返回 <code>501</code>                   |
| 用户主页         | <code>GET /users/{userid}</code>                     |   是 | 获取用户资料、统计及当前用户的关注状态 |
| 用户帖子         | <code>GET /users/{userid}/posts</code>               |   是 | 获取该用户的帖子                       |
| 我的点赞         | <code>GET /users/me/likes</code>                     |   是 | 获取当前用户点赞的帖子                 |
| 帖子列表         | <code>GET /posts</code>                              |   是 | 社区帖子列表（暂不支持搜索/排序/分页） |
| 帖子详情         | <code>GET /posts/{postId}</code>                     |   是 | 获取帖子、作者、点赞状态及详情         |
| 创建帖子         | <code>POST /posts</code>                             |   是 | 发布作品帖或询问帖                     |
| 点赞             | <code>PUT /posts/{postId}/like</code>                |   是 | 点赞（幂等）                           |
| 取消点赞         | <code>DELETE /posts/{postId}/like</code>             |   是 | 取消点赞（幂等）                       |
| 帖子评论列表     | <code>GET /posts/{postId}/comments</code>            |   是 | 获取帖子评论                           |
| 发表评论         | <code>POST /posts/{postId}/comments</code>           |   是 | 在帖子下创建评论                       |
| 上传图片         | <code>POST /uploads/images</code>                    |   是 | 上传帖子图片并返回 URL                 |
| 关注列表         | <code>GET /users/me/following</code>                 |   是 | 获取当前用户关注的人                   |
| 粉丝列表         | <code>GET /users/me/followers</code>                 |   是 | 获取关注当前用户的人                   |
| 关注用户         | <code>PUT /users/{userid}/follow</code>              |   是 | 关注指定用户（幂等）                   |
| 取消关注         | <code>DELETE /users/{userid}/follow</code>           |   是 | 取消关注指定用户（幂等）               |
| 私聊会话列表     | <code>GET /conversations</code>                      |   是 | 获取会话列表及最近消息                 |
| 会话消息         | <code>GET /conversations/{peerId}/messages</code>    |   是 | 获取与指定用户的消息记录               |
| 发送私聊消息     | <code>POST /conversations/{peerId}/messages</code>   |   是 | 持久化一条消息                         |
| 标记会话已读     | <code>POST /conversations/{peerId}/read</code>       |   是 | 暂未实现未读计数，固定返回 <code>0</code>         |

### 三.后端

#### 后端代码结构

```text
│  .gitignore
│  pom.xml
│  README.md
├─src
│  ├─main
│     ├─java
│     │  └─org
│     │      └─chengyuan
│     │          └─cyproject
│     │              │  CYprojectApplication.java
│     │              │
│     │              ├─config(注册)
│     │              │      JwtFilterConfig.java
│     │              │      ReviewFilterConfig.java
│     │              │      WebConfig.java
│     │              │
│     │              ├─controller(控制)
│     │              │      AuthController.java
│     │              │      ConversationController.java
│     │              │      PostController.java
│     │              │      UploadController.java
│     │              │      UserController.java
│     │              │
│     │              ├─dto(请求响应)
│     │              │  ├─common
│     │              │  │      AuthorSummary.java
│     │              │  │      CommentDto.java
│     │              │  │      ConversationItem.java
│     │              │  │      MessageDto.java
│     │              │  │      PageResponse.java
│     │              │  │      Pagination.java
│     │              │  │      PostDto.java
│     │              │  │      UserDto.java
│     │              │  │      UserStats.java
│     │              │  │      UserSummary.java
│     │              │  │
│     │              │  ├─request
│     │              │  │      CreateCommentRequest.java
│     │              │  │      CreatePostRequest.java
│     │              │  │      LoginRequest.java
│     │              │  │      PostQuery.java
│     │              │  │      RegisterRequest.java
│     │              │  │      SendMessageRequest.java
│     │              │  │      UpdateUserRequest.java
│     │              │  │
│     │              │  └─response
│     │              │          AuthResponse.java
│     │              │          FollowResponse.java
│     │              │          LikeResponse.java
│     │              │          MessageListResponse.java
│     │              │          Response.java
│     │              │          UnreadCountResponse.java
│     │              │          UploadImageResponse.java
│     │              │          UserResponse.java
│     │              │
│     │              ├─entity(数据库的映射)
│     │              │  │  CommentEntity.java
│     │              │  │  ConversationEntity.java
│     │              │  │  FollowlistEntity.java
│     │              │  │  LikelistEntity.java
│     │              │  │  TagEntity.java
│     │              │  │  UserEntity.java
│     │              │  │  WorkEntity.java
│     │              │  │
│     │              │  └─id
│     │              │          FollowlistId.java
│     │              │          LikelistId.java
│     │              │
│     │              ├─error(自定义错误)
│     │              │  │  ErrorCode.java
│     │              │  │  GlobalExceptionHandler.java
│     │              │  │
│     │              │  └─errors
│     │              │          BizException.java
│     │              │
│     │              ├─filter(过滤器)
│     │              │      CachedBodyRequestWrapper.java
│     │              │      JwtFilter.java
│     │              │      ReviewFilter.java
│     │              │
│     │              ├─repository(JPA接口)
│     │              │      CommentRepository.java
│     │              │      ConversationRepository.java
│     │              │      FollowlistRepository.java
│     │              │      LikelistRepository.java
│     │              │      TagRepository.java
│     │              │      UserRepository.java
│     │              │      WorkRepository.java
│     │              │
│     │              ├─service(服务实现)
│     │              │  │  AuthService.java
│     │              │  │  ConversationService.java
│     │              │  │  PostService.java
│     │              │  │  UploadService.java
│     │              │  │  UserService.java
│     │              │  │
│     │              │  └─impl
│     │              │          AuthServiceImpl.java
│     │              │          ConversationServiceImpl.java
│     │              │          PostServiceImpl.java
│     │              │          UploadServiceImpl.java
│     │              │          UserServiceImpl.java
│     │              │
│     │              └─util(工具)
│     │                      AIReview.java
│     │                      GetUserMessage.java
│     │                      JwtUtil.java
│     │                      PostAssembler.java
│     │                      SearchUtil.java
│     │                      TimeUtil.java
│     │                      UserContext.java
│     │
│     └─resources
│         │  application-local.yaml(本地配置)
│         │  application.yaml(基本配置)
│         │
│         ├─static
│         │  └─avatarFiles("头像"存储)
│         │          avatar_1.png
│         │          avatar_2.png
│         │          avatar_3.png
│         │          avatar_4.png
│         │          avatar_5.png
│         │          avatar_6.png
│         │
│         └─templates
└─uploads(用户上传)
        .gitkeep
        0558f8f7-2b20-42a4-9544-d9000ef75faf.jpg
```

### 四.数据库

#### 数据库图示

![图示](image/database.svg)

### 五.python

#### python代码结构

```text
├─aisummarizeTool(总结工具)
│  │  aisummarize1.py
│  │  __init__.py
│
├─contentDealTool(摘要工具)
│  │  contentdeal1.py
│  │  __init__.py
│
├─data(数据存放)
│      detail.jsonl
│      finaldata.json
│      finaldata.jsonl
│      idea(cn).json
│      idea(en).json
│      searchForAgent.jsonl
│      searchForAI.jsonl
│      summary(cn).json
│      summary(en).json
│
├─requestTool(请求工具)
│  │  request1.py
│  │  request2.py
│  │  __init__.py
│
├─restoreTool(全局仓库)
│  │  restore1.py
│  │  __init__.py
│
├─searchTool(搜索工具)
│  │  search1.py
│  │  search2.py
│  │  __init__.py
│
├─selectTool(筛选工具)
│  │  select1.py
│  │  __init__.py
│
|  main.py(主程序)
|  schemas.py(工具构造)
```

## 前端

### 前端主要Vue组件（网页或组件）

1. <code>HomeView.vue</code>网站的主页面，包含路由到登录页面等基础功能

2. <code>AuthView.vue</code>网页的登录注册页面，登录与注册两页面可以相互路由并实现登录注册|

3. <code>ProfileView.vue</code>用户的主页，展示用户的相关信息，用户与其他用户的关系，用户的相关浏览信息，以及跳转到其他页面等功能

4. <code>ForumView.vue</code>用于展示论坛的相关帖子，并且提供搜索功能，以及用户互动等功能

5. <code>FollowListView.vue</code>用于展示关注信息的组件

6. <code>ChatView.vue</code>用于用户间私聊的页面

7. <code>CreatePostView.vue</code>用于用户发布帖子的网页

8. <code>TalksView.vue</code>用于展示私聊列表的组件

9. <code>UserProfileView.vue</code>用于展示访问其他用户的页面

10. <code>PostDetailView.vue</code>用于展示帖子详情的页面

## 接口

### 前后端接口约定

以下是根据当前后端实现整理出的接口清单与响应结构。路径以 Axios <code>baseURL: '/api'</code> 为基础（Vite 开发代理会移除 <code>/api</code> 前缀）；除登录、注册外，接口都需要认证。

### 通用约定

- 请求和响应使用 UTF-8 JSON；图片上传使用 <code>multipart/form-data</code>。**请求体必须是 UTF-8 编码**（中文等内容若以 GBK 等非 UTF-8 字节发送，会被判定为“请求体无法解析”并返回 400）。
- JSON 字段使用 camelCase。
- 认证请求使用 <code>Authorization</code> 请求头，值为 <code>Bearer</code> + 空格 + 访问令牌；登录、注册不需要该请求头。
- 所有接口（无论成功失败）统一返回 <code>Response&lt;T&gt;</code>：<code>int code</code>、<code>String message</code>、<code>T data</code>。成功时 <code>code</code> 为 <code>0</code>，<code>data</code> 为业务数据；失败时 <code>code</code> 为非 <code>0</code> 整数，<code>message</code> 描述错误，<code>data</code> 为 <code>null</code>。
- 同时使用合适的 HTTP 状态码：<code>400</code> 参数无效 / 请求体无法解析、<code>401</code> 未认证或 token 失效、<code>403</code> 无权限、<code>404</code> 资源不存在、<code>409</code> 用户名/邮箱冲突、<code>413</code> 上传文件过大、<code>500</code> 服务端错误、<code>501</code> 功能未实现。
- 除登录、注册外，所有路径中的 <code>{userid}</code> / <code>{postId}</code> / <code>{peerId}</code> 都是数据库中的字符串主键。**注意：当前实现的用户主键 <code>userid</code> 为 <code>email + password</code> 拼接**（历史设计），并非 <code>username</code>，也并非随机生成的 <code>user_xxx</code>。
- 列表接口当前固定返回 <code>page=1</code>、<code>pageSize=20</code>，<code>totalItems</code> 为实际条数，<code>totalPages</code> 按 <code>ceil(totalItems / pageSize)</code> 计算；目前尚未实现真正的分页、搜索、排序参数。

统一响应示例（成功及失败）：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "example": "业务响应主体"
  }
}
```

```json
{
  "code": 400,
  "message": "请求参数无效：邮箱格式不正确",
  "data": null
}
```

### 接口清单

| 功能             | 方法与路径                                | 认证 | 用途                                   |
| ---------------- | ----------------------------------------- | ---: | -------------------------------------- |
| 注册             | <code>POST /begain/register</code>                   |   否 | 创建账号并返回 token、用户资料         |
| 登录             | <code>POST /begain/login</code>                      |   否 | 使用邮箱登录                           |
| 当前用户资料     | <code>GET /users/me</code>                           |   是 | 初始化个人主页及账号设置               |
| 更新当前用户资料 | <code>PATCH /users/me</code>                         |   是 | 暂未实现，返回 <code>501</code>                   |
| 用户主页         | <code>GET /users/{userid}</code>                     |   是 | 获取用户资料、统计及当前用户的关注状态 |
| 用户帖子         | <code>GET /users/{userid}/posts</code>               |   是 | 获取该用户的帖子                       |
| 我的点赞         | <code>GET /users/me/likes</code>                     |   是 | 获取当前用户点赞的帖子                 |
| 帖子列表         | <code>GET /posts</code>                              |   是 | 社区帖子列表（暂不支持搜索/排序/分页） |
| 帖子详情         | <code>GET /posts/{postId}</code>                     |   是 | 获取帖子、作者、点赞状态及详情         |
| 创建帖子         | <code>POST /posts</code>                             |   是 | 发布作品帖或询问帖                     |
| 点赞             | <code>PUT /posts/{postId}/like</code>                |   是 | 点赞（幂等）                           |
| 取消点赞         | <code>DELETE /posts/{postId}/like</code>             |   是 | 取消点赞（幂等）                       |
| 帖子评论列表     | <code>GET /posts/{postId}/comments</code>            |   是 | 获取帖子评论                           |
| 发表评论         | <code>POST /posts/{postId}/comments</code>           |   是 | 在帖子下创建评论                       |
| 上传图片         | <code>POST /uploads/images</code>                    |   是 | 上传帖子图片并返回 URL                 |
| 关注列表         | <code>GET /users/me/following</code>                 |   是 | 获取当前用户关注的人                   |
| 粉丝列表         | <code>GET /users/me/followers</code>                 |   是 | 获取关注当前用户的人                   |
| 关注用户         | <code>PUT /users/{userid}/follow</code>              |   是 | 关注指定用户（幂等）                   |
| 取消关注         | <code>DELETE /users/{userid}/follow</code>           |   是 | 取消关注指定用户（幂等）               |
| 私聊会话列表     | <code>GET /conversations</code>                      |   是 | 获取会话列表及最近消息                 |
| 会话消息         | <code>GET /conversations/{peerId}/messages</code>    |   是 | 获取与指定用户的消息记录               |
| 发送私聊消息     | <code>POST /conversations/{peerId}/messages</code>   |   是 | 持久化一条消息                         |
| 标记会话已读     | <code>POST /conversations/{peerId}/read</code>       |   是 | 暂未实现未读计数，固定返回 <code>0</code>         |

### 认证与账号

#### 注册

<code>POST /begain/register</code>

```json
{
  "username": "xiaozhou",
  "email": "name@example.com",
  "password": "用户输入的密码"
}
```

后端会校验邮箱格式、必填项，并检查邮箱/用户名是否重复（重复返回 <code>409</code>）。

#### 登录

<code>POST /begain/login</code>

```json
{
  "email": "name@example.com",
  "password": "用户输入的密码"
}
```

登录和注册成功响应（完整 <code>Response</code> 结构）：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "签发的访问令牌",
    "user": {
      "id": "name@example.com用户输入的密码",
      "username": "xiaozhou",
      "name": "xiaozhou",
      "handle": "xiaozhou",
      "email": "name@example.com",
      "avatar": "http://localhost:8080/avatarFiles/image.webp",
      "bio": "hello world(现在不支持设置个性标签)",
      "online": true,
      "stats": {
        "posts": 0,
        "likes": 0,
        "followers": 0
      }
    }
  }
}
```

- <code>token</code> 为非空字符串，前端使用 <code>auth_token</code> 保存并作为 Bearer token 发送。**每次登录都会重新签发一个全新 token**，不会复用上次的缓存 token。
- 密码只通过请求提交，**任何响应都不得返回密码或密码哈希**。
- 当前 <code>name</code>、<code>handle</code> 与 <code>username</code> 相同，<code>id</code> 为 <code>email + password</code> 拼接。

#### 当前用户资料与更新

<code>GET /users/me</code> 返回完整 <code>Response&lt;T&gt;</code>，其中 <code>data</code> 为上例中的 <code>user</code> 对象（不含 <code>token</code>），且 <code>online</code> 恒为 <code>true</code>。

<code>PATCH /users/me</code> 返回完整<code>Response&lt;T&gt;</code>，其中<code>data</code> 为上例中的 <code>user</code> 对象

### 帖子、搜索、点赞与评论

#### 帖子数据结构

帖子对象统一使用以下字段；详情额外返回完整 <code>content</code> 与 <code>updatedAt</code>。列表接口将帖子对象放在 <code>data.items</code> 中：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "生成的 UUID",
    "title": "帖子主题",
    "excerpt": "从正文截取的摘要",
    "content": "支持 Markdown 的正文",
    "author": {
      "id": "xiaozhou@example.com密码",
      "username": "xiaozhou",
      "name": "xiaozhou",
      "avatar": "http://localhost:8080/avatarFiles/image.webp"
    },
    "category": "work",
    "tags": ["暂时无法设置标签"],
    "views": 0,
    "likes": 8,
    "likedByMe": false,
    "publishedAt": "2026-11-07T04:27:11.973Z",
    "updatedAt": "2026-11-07T04:27:11.973Z"
  }
}
```

- <code>category</code> 支持 <code>work</code>（作品帖）和 <code>ask</code>（询问帖）。
- <code>publishedAt</code>、<code>updatedAt</code> 使用 ISO 8601 时间。
- 当前实现限制：<code>tags</code> 未持久化，固定返回占位 <code>["暂时无法设置标签"]</code>；<code>views</code> 未做统计，固定为 <code>0</code>；<code>excerpt</code> 由正文截取前 120 个字符得到。

#### 帖子列表

<code>GET /posts</code>

当前实现返回全部帖子（按数据库顺序），不支持 <code>q</code>、<code>category</code>、<code>sort</code>、<code>page</code>、<code>pageSize</code> 参数。响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [
      {
        "id": "生成的 UUID",
        "title": "帖子主题",
        "excerpt": "从正文截取的摘要",
        "author": {
          "id": "xiaozhou@example.com密码",
          "username": "xiaozhou",
          "name": "xiaozhou",
          "avatar": null
        },
        "category": "work",
        "tags": ["暂时无法设置标签"],
        "views": 0,
        "likes": 8,
        "likedByMe": false,
        "publishedAt": "2026-11-07T04:27:12Z"
      }
    ],
    "pagination": {
      "page": 1,
      "pageSize": 20,
      "totalItems": 1,
      "totalPages": 1
    }
  }
}
```

#### 帖子详情与发布

<code>GET /posts/{postId}</code> 在 <code>data</code> 中返回完整帖子对象（含 <code>content</code>、<code>updatedAt</code>）。浏览量暂不统计，详情读取不会产生统计副作用。

<code>POST /posts</code> 请求：

```json
{
  "title": "帖子主题",
  "category": "ask",
  "tags": ["Vue", "WebGL"],
  "content": "支持 Markdown 的正文"
}
```

成功时在 <code>data</code> 中返回新建的完整帖子对象。<code>tags</code> 目前仅接受但不会持久化。

#### 删除帖子

<code>DELETE /posts/{postId}</code> 仅允许帖子作者本人操作。

```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

后端删除时会级联清理该帖子的点赞记录、评论记录、标签记录以及帖子本身。


#### 点赞

<code>PUT /posts/{postId}/like</code> 表示点赞，<code>DELETE /posts/{postId}/like</code> 表示取消点赞，两者幂等（重复请求返回相同结果，不会重复计数）。完整响应的 <code>data</code> 返回操作后的权威状态：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "liked": true,
    "likes": 9
  }
}
```

#### 评论

<code>GET /posts/{postId}/comments</code> 完整响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [
      {
        "id": "生成的 UUID",
        "author": {
          "id": "ray@example.com密码",
          "username": "ray",
          "name": "ray",
          "avatar": null
        },
        "content": "评论内容",
        "createdAt": "2026-11-07T04:27:29Z"
      }
    ],
    "pagination": {
      "page": 1,
      "pageSize": 20,
      "totalItems": 1,
      "totalPages": 1
    }
  }
}
```

<code>POST /posts/{postId}/comments</code> 请求：

```json
{
  "content": "评论内容"
}
```

成功时在 <code>data</code> 中返回新建的评论对象，结构与列表中的单条评论相同。

### 用户主页与关注

<code>GET /users/{userid}</code> 完整响应在 <code>data</code> 中直接返回用户对象（不是包在 <code>user</code> 字段里）：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "name@example.com用户输入的密码",
    "username": "xiaozhou",
    "name": "xiaozhou",
    "handle": "xiaozhou",
    "avatar": null,
    "bio": "hello world(现在不支持设置个性标签)",
    "online": false,
    "stats": {
      "posts": 3,
      "likes": 12,
      "followers": 20
    },
    "isFollowing": false
  }
}
```

<code>GET /users/{userid}/posts</code> 的 <code>data</code> 使用帖子列表的 <code>items</code>、<code>pagination</code> 结构；当前不支持 <code>category</code> 筛选。

<code>GET /users/me/likes</code> 在 <code>data</code> 中返回当前用户点赞的帖子，使用帖子列表相同的 <code>items</code>、<code>pagination</code> 结构。

关注列表：

- <code>GET /users/me/following</code>
- <code>GET /users/me/followers</code>

两者的 <code>data</code> 都为 <code>{ "items": [UserSummary], "pagination": Pagination }</code>。<code>UserSummary</code> 包含 <code>id</code>、<code>username</code>、<code>name</code>、<code>avatar</code>、<code>bio</code>、<code>online</code>。其中 <code>id</code> 是用户主键（当前为 <code>email + password</code>），前端可用它跳转到该用户主页或发起私聊。

<code>PUT /users/{userid}/follow</code> 和 <code>DELETE /users/{userid}/follow</code> 完整成功响应（幂等）：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "following": true,
    "followers": 21
  }
}
```

取消关注时 <code>data</code> 返回 <code>following: false</code> 及更新后的粉丝数（目标用户的粉丝数）。不能关注自己（返回 <code>400</code>）。

### 图片上传

<code>POST /uploads/images</code> 使用 <code>multipart/form-data</code>，文件字段名为 <code>file</code>。后端会校验文件非空、大小不超过 10 MB、<code>Content-Type</code> 为 <code>image/*</code>，并拒绝无法解析为图片的文件。

完整成功响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "url": "http://localhost:8080/uploads/{uuid}.png",
    "width": 1200,
    "height": 800,
    "mime": "image/png",
    "size": 123456
  }
}
```

- 文件保存到运行目录下的 <code>uploads/</code> 目录（由 <code>application.yaml</code> 的 <code>file.upload-dir</code> 配置），文件名使用随机 UUID 避免覆盖与路径注入。
- 后端通过 <code>WebConfig</code> 将 <code>/uploads/**</code> 映射到该上传目录，返回的 <code>url</code> 可通过 URL 访问；该路径**仍受登录认证保护**，访问时需携带 <code>Authorization: Bearer &lt;token&gt;</code>。

### 私聊

#### HTTP 加载和发送规则

- 当前版本不使用 WebSocket，也不会在登录后保持实时连接。
- 访问聊天列表时调用 <code>GET /conversations</code>，返回每个会话最近一条消息摘要、时间和未读数（未读数固定为 <code>0</code>）。
- 打开某个会话时，前端请求 <code>GET /conversations/{peerId}/messages</code> 获取该会话的完整消息历史。
- 发送消息调用 <code>POST /conversations/{peerId}/messages</code>，由服务端持久化并返回新消息。
- 消息不会即时推送；收件人需要重新访问/刷新聊天列表或会话才能获取新消息。

#### 会话列表

<code>GET /conversations</code> 的 <code>data</code> 返回会话摘要：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [
      {
        "id": "xiaozhou@example.com密码",
        "username": "xiaozhou",
        "name": "xiaozhou",
        "avatar": null,
        "online": true,
        "lastMessage": "你好，收到。",
        "lastMessageAt": "2026-11-07T04:27:44Z",
        "unreadCount": 0
      }
    ],
    "pagination": {
      "page": 1,
      "pageSize": 20,
      "totalItems": 1,
      "totalPages": 1
    }
  }
}
```

#### 打开会话与加载消息

<code>GET /conversations/{peerId}/messages</code> 完整响应（消息按时间正序返回，当前不做游标分页，<code>nextCursor</code> 恒为 <code>null</code>）：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [
      {
        "id": "生成的 UUID",
        "senderUsername": "xiaozhou",
        "recipientUsername": "cy",
        "content": "你好！",
        "createdAt": "2026-11-07T04:27:44Z"
      }
    ],
    "nextCursor": null
  }
}
```

#### 发送消息

<code>POST /conversations/{peerId}/messages</code> 请求体：

```json
{
  "content": "消息内容"
}
```

成功时完整响应的 <code>data</code> 返回已持久化的消息对象，结构与历史消息相同。

#### 标记会话已读

<code>POST /conversations/{peerId}/read</code> 当前未实现真正的未读计数，固定返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "unreadCount": 0
  }
}
```

## 后端

### 一.Controller

#### Controller文件概览

- <code>AuthController.java</code>
- <code>UserController.java</code>
- <code>PostController.java</code>
- <code>UploadController.java</code>
- <code>ConversationController.java</code>

#### 接口路径

Vite 开发代理会移除 <code>/api</code> 前缀。例如前端请求 <code>/api/begain/login</code>，Controller 实际接收 <code>/begain/login</code>。因此下面的 <code>@RequestMapping</code> 路径不包含 <code>/api</code>。

#### 认证方式

登录、注册之外的接口由 <code>JwtInterceptor</code>（一个 <code>OncePerRequestFilter</code>）统一校验 <code>Authorization: Bearer &lt;token&gt;</code> 请求头，校验通过后将用户 id 写入 <code>UserContext</code>（ThreadLocal），Service 层通过 <code>UserContext.getUserId()</code> 获取当前用户。**Controller 方法不再通过 <code>@RequestParam token</code> 接收 token。**

#### ControllerAPI详情

##### 1.AuthController.java

```java
@RestController
@RequestMapping("/begain")
public class AuthController {

    @PostMapping("/login")
    public Response<AuthResponse> login(@RequestBody LoginRequest request) {
        return Response.ok(authService.login(request));
    }

    @PostMapping("/register")
    public Response<AuthResponse> register(@RequestBody RegisterRequest request) {
        return Response.ok(authService.register(request));
    }
}
```

##### 2.UserController.java

```java
@RestController
@RequestMapping("/users")
public class UserController {

    @GetMapping("/me")
    public Response<UserDto> getCurrentUser() { ... }

    @PatchMapping("/me")
    public Response<Void> updateCurrentUser(@RequestBody UpdateUserRequest request) { ... } // 未实现，501

    @GetMapping("/{userid}")
    public Response<UserDto> getUser(@PathVariable String userid) { ... }

    @GetMapping("/{userid}/posts")
    public Response<PageResponse<PostDto>> getUserPosts(@PathVariable String userid) { ... }

    @GetMapping("/me/likes")
    public Response<PageResponse<PostDto>> getLikedPosts() { ... }

    @GetMapping("/me/following")
    public Response<PageResponse<UserSummary>> getFollowing() { ... }

    @GetMapping("/me/followers")
    public Response<PageResponse<UserSummary>> getFollowers() { ... }

    @PutMapping("/{userid}/follow")
    public Response<FollowResponse> follow(@PathVariable String userid) { ... }

    @DeleteMapping("/{userid}/follow")
    public Response<FollowResponse> unfollow(@PathVariable String userid) { ... }
}
```

##### 3.PostController.java

```java
@RestController
@RequestMapping("/posts")
public class PostController {

    @GetMapping
    public Response<PageResponse<PostDto>> getPosts() { ... }

    @GetMapping("/{postId}")
    public Response<PostDto> getPost(@PathVariable String postId) { ... }

    @PostMapping
    public Response<PostDto> createPost(@RequestBody CreatePostRequest request) { ... }

    @PutMapping("/{postId}/like")
    public Response<LikeResponse> likePost(@PathVariable String postId) { ... }

    @DeleteMapping("/{postId}/like")
    public Response<LikeResponse> unlikePost(@PathVariable String postId) { ... }

    @GetMapping("/{postId}/comments")
    public Response<PageResponse<CommentDto>> getComments(@PathVariable String postId) { ... }

    @PostMapping("/{postId}/comments")
    public Response<CommentDto> createComment(@PathVariable String postId,
                                              @RequestBody CreateCommentRequest request) { ... }

    @DeleteMapping("/{postId}")
    public Response<Void> deletePost(@PathVariable String postId) { ... }
}
```

##### 4.UploadController.java

```java
@RestController
@RequestMapping("/uploads")
public class UploadController {

    @PostMapping("/images")
    public Response<UploadImageResponse> uploadImage(@RequestPart("file") MultipartFile file) { ... }
}
```

##### 5.ConversationController.java

```java
@RestController
@RequestMapping("/conversations")
public class ConversationController {

    @GetMapping
    public Response<PageResponse<ConversationItem>> getConversations() { ... }

    @GetMapping("/{peerId}/messages")
    public Response<MessageListResponse> getMessages(@PathVariable String peerId) { ... }

    @PostMapping("/{peerId}/messages")
    public Response<MessageDto> sendMessage(@PathVariable String peerId,
                                            @RequestBody SendMessageRequest request) { ... }

    @PostMapping("/{peerId}/read")
    public Response<UnreadCountResponse> markAsRead(@PathVariable String peerId) { ... }
}
```

### 二.Dto

> getter、setter、构造函数等省略，<code>@Data</code> 注解自动生成；<code>@NoArgsConstructor</code> 生成无参构造；<code>@AllArgsConstructor</code> 生成全参构造。

- *目录*

```text
dto/
├── common/
│   ├── Pagination.java
│   ├── PageResponse.java
│   ├── AuthorSummary.java
│   ├── UserStats.java
│   ├── UserDto.java
│   ├── UserSummary.java
│   ├── PostDto.java
│   ├── CommentDto.java
│   ├── ConversationItem.java
│   └── MessageDto.java
├── request/
│   ├── RegisterRequest.java
│   ├── LoginRequest.java
│   ├── UpdateUserRequest.java
│   ├── CreatePostRequest.java
│   ├── CreateCommentRequest.java
│   ├── SendMessageRequest.java
│   └── PostQuery.java
└── response/
    ├── AuthResponse.java
    ├── UserResponse.java
    ├── LikeResponse.java
    ├── FollowResponse.java
    ├── UploadImageResponse.java
    ├── MessageListResponse.java
    ├── UnreadCountResponse.java
    └── Response.java
```

> 说明：<code>ErrorResponse.java</code> 已删除（统一使用 <code>Response&lt;T&gt;</code>）。<code>UserResponse.java</code>、<code>PostQuery.java</code> 当前未被代码引用，属预留。

#### Common

##### 1.Pagination.java

```java
public class Pagination {
    private Integer page;
    private Integer pageSize;
    private Long totalItems;
    private Integer totalPages;
    // 提供静态工厂 Pagination.of(page, pageSize, totalItems) 自动计算 totalPages
}
```

##### 2.PageResponse.java

```java
public class PageResponse<T> {
    private List<T> items;
    private Pagination pagination;
}
```

##### 3.AuthorSummary.java

```java
public class AuthorSummary {
    private String id;         // 作者主键（当前为 email + password），供前端跳转作者主页用
    private String username;
    private String name;
    private String avatar;
}
```

##### 4.UserStats.java

```java
public class UserStats {
    private Integer posts;
    private Integer likes;
    private Integer followers;
}
```

##### 5.UserDto.java

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto {
    private String id;
    private String username;
    private String name;
    private String handle;
    private String email;
    private String avatar;
    private String bio;
    private Boolean online;
    private UserStats stats;
    private Boolean isFollowing;   // 仅 /users/{userid} 有值
}
```

##### 6.UserSummary.java

```java
public class UserSummary {
    private String id;         // 用户主键（当前为 email + password），供前端跳转用
    private String username;
    private String name;
    private String avatar;
    private String bio;
    private Boolean online;
}
```

##### 7.PostDto.java

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostDTO {
    private String id;
    private String title;
    private String excerpt;
    private String content;        // 仅详情有
    private AuthorSummary author;
    private String category;       // work / ask
    private List<String> tags;
    private Integer views;
    private Integer likes;
    private Boolean likedByMe;
    private Instant publishedAt;
    private Instant updatedAt;     // 仅详情有
}
```

##### 8.CommentDto.java

```java
public class CommentDTO {
    private String id;
    private AuthorSummary author;
    private String content;
    private Instant createdAt;
}
```

##### 9.ConversationItem.java

```java
public class ConversationItem {
    private String id;         // 会话对方的主键（当前为 email + password），供前端打开会话用
    private String username;
    private String name;
    private String avatar;
    private Boolean online;
    private String lastMessage;
    private Instant lastMessageAt;
    private Integer unreadCount;
}
```

##### 10.MessageDto.java

```java
public class MessageDTO {
    private String id;
    private String senderUsername;
    private String recipientUsername;
    private String content;
    private Instant createdAt;
}
```

#### Request

##### 1.LoginRequest.java

```java
public class LoginRequest {
  private String email;
  private String password;
}
```

##### 2.RegisterRequest.java

```java
public class RegisterRequest {
  private String username;
  private String email;
  private String password;
}
```

##### 3.UpdateUserRequest.java

```java
public class UpdateUserRequest {
    private String username;
    private String email;
    private String password;   // null/空 = 不修改
}
```

##### 4.CreatePostRequest.java

```java
public class CreatePostRequest {
    private String title;
    private String category;
    private List<String> tags;
    private String content;
}
```

##### 5.CreateCommentRequest.java

```java
public class CreateCommentRequest {
    private String content;
}
```

##### 6.SendMessageRequest.java

```java
public class SendMessageRequest {
    private String content;
}
```

##### 7.PostQuery.java（预留，暂未使用）

```java
public class PostQuery {
    private String q;
    private String category;
    private String sort;
    private Integer page;
    private Integer pageSize;
}
```

##### 8.imageUpload

图片上传是 multipart/form-data，Controller 用 <code>@RequestPart("file") MultipartFile file</code>，不需要 Request 类。

#### Response

##### 0.Response.java

```java
public class Response<T> {
    private int code;
    private String message;
    private T data;
    // 静态工厂：Response.ok(data)、Response.fail(ErrorCode)、Response.fail(code, message)
}
```

##### 1.AuthResponse.java

```java
public class AuthResponse {
    private String token;
    private UserDTO user;
}
```

##### 2.UserResponse.java（预留，暂未使用）

```java
public class UserResponse {
    private UserDTO user;
}
```

##### 3.LikeResponse.java

```java
public class LikeResponse {
    private Boolean liked;
    private Integer likes;
}
```

##### 4.FollowResponse.java

```java
public class FollowResponse {
    private Boolean following;
    private Integer followers;
}
```

##### 5.UploadImageResponse.java

```java
public class UploadImageResponse {
    private String url;
    private Integer width;
    private Integer height;
    private String mime;
    private Long size;
}
```

##### 6.MessageListResponse.java

```java
public class MessageListResponse {
    private List<MessageDTO> items;
    private String nextCursor;
}
```

##### 7.列表类响应（复用 PageResponse<T>）

| 接口 | 返回类型 |
| --- | --- |
| GET /posts | PageResponse<PostDTO> |
| GET /posts/{id}/comments | PageResponse<CommentDTO> |
| GET /users/{userid}/posts | PageResponse<PostDTO> |
| GET /users/me/likes | PageResponse<PostDTO> |
| GET /users/me/following | PageResponse<UserSummary> |
| GET /users/me/followers | PageResponse<UserSummary> |
| GET /conversations | PageResponse<ConversationItem> |

### 三.Service（接口 + 实现）

- 按 <code>service</code> 接口 + <code>service.impl</code> 实现分层。Service 方法不再接收 <code>token</code> 参数，当前用户通过 <code>UserContext.getUserId()</code> 获取。

```text
service/
├── AuthService.java
├── UserService.java
├── PostService.java
├── UploadService.java
├── ConversationService.java
└── impl/
    ├── AuthServiceImpl.java
    ├── UserServiceImpl.java
    ├── PostServiceImpl.java
    ├── UploadServiceImpl.java
    └── ConversationServiceImpl.java
```

#### Service 接口

##### 1. AuthService.java

```java
public interface AuthService {
    AuthResponse login(LoginRequest request);
    AuthResponse register(RegisterRequest request);
}
```

##### 2. UserService.java

```java
public interface UserService {
    UserDto getCurrentUser();
    void updateCurrentUser(UpdateUserRequest request);
    UserDto getUser(String userid);
    PageResponse<PostDto> getUserPosts(String userid);
    PageResponse<PostDto> getLikedPosts();
    PageResponse<UserSummary> getFollowing();
    PageResponse<UserSummary> getFollowers();
    FollowResponse follow(String userid);
    FollowResponse unfollow(String userid);
}
```

##### 3. PostService.java

```java
public interface PostService {
    PageResponse<PostDto> getPosts();
    PostDto getPost(String postId);
    PostDto createPost(CreatePostRequest request);
    LikeResponse likePost(String postId);
    LikeResponse unlikePost(String postId);
    PageResponse<CommentDto> getComments(String postId);
    CommentDto createComment(String postId, CreateCommentRequest request);
    void deletePost(String postId);
}
```

##### 4. UploadService.java

```java
public interface UploadService {
    UploadImageResponse uploadImage(MultipartFile file);
}
```

##### 5. ConversationService.java

```java
public interface ConversationService {
    PageResponse<ConversationItem> getConversations();
    MessageListResponse getMessages(String peerId);
    MessageDto sendMessage(String peerId, SendMessageRequest request);
    UnreadCountResponse markAsRead(String peerId);
}
```

#### 其它工具类

- <code>util/UserContext.java</code>：ThreadLocal 保存当前登录用户 id。
- <code>util/TimeUtil.java</code>：LocalDateTime 与 Instant 之间带时区转换。
- <code>util/PostAssembler.java</code>：把 WorkEntity 组装为 PostDto，收敛重复代码。
- <code>util/JwtUtil.java</code>：JWT 签发与解析。
- <code>util/GetUserMessage.java</code>：用户资料 DTO 构建与 Redis 缓存。

## 数据库设计

### 数据关系E-R图

![E-R图](image/database.svg)

### 基本表

#### 1.user(User)最后还是决定加id作为主键，用户名是可以改变的

```sql
create table `user` (
  id varchar(255)  not null,
  username varchar(255) not null,
  password varchar(255) not null,
  email varchar(255) unique,
  avatar varchar(255),
  bio varchar(255),
  primary key (id)
);
```

#### 2.work(作品)

```sql
create table `work`
    (id varchar(255) primary key,
    userid varchar(255),
    worktopic varchar(255),
    worktype varchar(255),
    body text,
    views Integer,
    publishtime datetime,
    updatetime datetime,
    foreign key(userid) references user(id));
```

#### 3.like(点赞)

```sql
create table `likelist`
    (workid varchar(255),
    userid varchar(255),
    primary key (workid, userid),
    foreign key(workid) references work(id),
    foreign key(userid) references user(id));
```

#### 4.follow(关注)

```sql
create table `followlist`
    (followeeid varchar(255),
    followerid varchar(255),
    primary key (followeeid, followerid),
    foreign key(followeeid) references user(id),
    foreign key(followerid) references user(id));
```

#### 5.comment(评论)

```sql
create table `commentlist`
    (workid varchar(255),
    userid varchar(255),
    id varchar(255),
    body text,
    publishtime datetime,
    primary key(id),
    foreign key(workid) references work(id),
    foreign key(userid) references user(id))
```

#### 6.conversation(对话记录)

```sql
create table `conversationlist`
    (id varchar(255),
    senderid varchar(255),
    receiver varchar(255),
    publishtime datetime,
    body text,
    primary key(id),
    foreign key(senderid) references user(id),
    foreign key(receiver) references user(id))
```

#### 7.tag(新增内容：标签)

```sql
create table `taglist`
    (id varchar(255),
    tags varchar(255),
    primary key(id),
    foreign key(id) references work(id))
```

