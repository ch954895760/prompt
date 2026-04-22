# Prompt 开发计划

## 上下文
开发一款全栈提示词管理软件，支持个人提示词库的建立、分类管理、变量模板、AI集成测试等核心能力。面向多用户的云端同步Web应用场景。

## 技术架构

### 前端
- **框架**: Vue 3 + Vite + TypeScript
- **UI库**: Element Plus（组件丰富，适合管理系统）
- **状态管理**: Pinia
- **路由**: Vue Router 4
- **HTTP客户端**: Axios
- **样式**: Tailwind CSS + SCSS
- **图标**: Element Plus Icons / Iconify

### 后端
- **框架**: Spring Boot 3.x + Java 17
- **ORM**: MyBatis-Plus（简化CRUD）
- **安全**: Spring Security + JWT
- **数据库**: MySQL 8.0
- **缓存**: Redis（会话、热点数据缓存）
- **搜索**: MySQL全文索引（初期）+ 分页筛选
- **文档**: SpringDoc OpenAPI（自动生成API文档）
- **构建**: Maven

### 基础设施
- **容器化**: Docker + Docker Compose
- **反向代理**: Nginx（前端静态资源 + API代理）

## 数据库设计

### 核心表结构
1. **user** - 用户表（id, username, email, password_hash, avatar, created_at, updated_at）
2. **prompt** - 提示词表（id, user_id, title, content, description, category_id, variables_json, is_public, usage_count, created_at, updated_at）
3. **category** - 分类表（id, user_id, name, parent_id, sort_order, icon, color）
4. **tag** - 标签表（id, user_id, name, color）
5. **prompt_tag** - 提示词-标签关联表（prompt_id, tag_id）
6. **prompt_history** - 提示词版本历史（id, prompt_id, content, version, created_at）
7. **user_setting** - 用户设置（id, user_id, theme, default_model, api_keys_encrypted）

## 项目结构

```
prompt/
├── docker-compose.yml              # MySQL + Redis + Nginx 编排
├── prompt-ui/                  # Vue3 前端项目
│   ├── src/
│   │   ├── api/                    # API 接口封装
│   │   ├── assets/                 # 静态资源
│   │   ├── components/             # 公共组件
│   │   │   ├── PromptCard.vue      # 提示词卡片
│   │   │   ├── VariableForm.vue    # 变量填写表单
│   │   │   └── CategoryTree.vue    # 分类树组件
│   │   ├── views/                  # 页面视图
│   │   │   ├── Login.vue
│   │   │   ├── Register.vue
│   │   │   ├── Dashboard.vue       # 主控制台
│   │   │   ├── PromptList.vue      # 提示词列表
│   │   │   ├── PromptDetail.vue    # 提示词详情/编辑
│   │   │   ├── PromptEditor.vue    # 富文本/代码编辑器
│   │   │   ├── CategoryManage.vue  # 分类管理
│   │   │   └── Settings.vue        # 个人设置
│   │   ├── router/                 # 路由配置
│   │   ├── stores/                 # Pinia 状态管理
│   │   ├── utils/                  # 工具函数
│   │   └── App.vue
│   ├── package.json
│   └── vite.config.ts
└── prompt-server/              # Spring Boot 后端项目
    ├── src/main/java/com/prompt
    │   ├── controller/             # REST API 控制器
    │   ├── service/                # 业务逻辑层
    │   ├── mapper/                 # MyBatis-Plus Mapper
    │   ├── entity/                 # 数据实体
    │   ├── dto/                    # 数据传输对象
    │   ├── vo/                     # 视图对象
    │   ├── config/                 # 配置类
    │   ├── security/               # JWT + Spring Security
    │   ├── interceptor/            # 拦截器
    │   ├── exception/              # 全局异常处理
    │   └── util/                   # 工具类
    └── pom.xml
```

## 功能模块实施计划

### 阶段一：基础架构与项目初始化
1. 初始化 Vue3 + Vite + TypeScript 项目，配置 Element Plus、Tailwind CSS、Pinia
2. 初始化 Spring Boot 3.x 项目，集成 MyBatis-Plus、Spring Security、JWT、Redis
3. 配置 Docker Compose（MySQL 8 + Redis + Nginx）
4. 设计并初始化数据库表结构（Flyway 或手动SQL脚本）
5. 配置跨域、全局异常处理、统一响应封装

### 阶段二：用户认证与基础UI框架
1. 后端：实现用户注册、登录、JWT认证、密码加密（BCrypt）
2. 后端：实现刷新Token机制
3. 前端：登录/注册页面，表单验证
4. 前端：主布局框架（侧边栏、顶部导航、路由切换）
5. 前端：Axios封装（自动携带Token、401跳转登录）
6. 前端：Pinia用户状态管理

### 阶段三：提示词核心功能
1. 后端：提示词的CRUD接口（分页列表、详情、创建、更新、删除）
2. 后端：分类的CRUD接口（支持多级嵌套）
3. 后端：标签的CRUD接口
4. 后端：提示词与标签的关联管理
5. 前端：提示词列表页（卡片/列表视图切换、分页）
6. 前端：提示词编辑器（支持变量占位符 `{{variable}}` 高亮）
7. 前端：分类管理页面（拖拽排序、树形结构）
8. 前端：标签管理（颜色选择器、快速输入）

### 阶段四：高级功能
1. **变量系统**：
   - 前端：编辑器中识别 `{{变量名}}` 语法
   - 前端：渲染动态表单让用户填写变量值
   - 后端：变量JSON存储与替换逻辑
2. **一键复制**：前端实现复制最终生成文本到剪贴板
3. **快速搜索**：
   - 后端：MySQL全文搜索 + 标题/内容/标签联合查询
   - 前端：搜索框实时防抖搜索、筛选器（分类/标签/时间范围）
4. **导入导出**：
   - JSON导入导出（完整数据结构）
   - Markdown导出（每个提示词为一个 `.md` 文件，ZIP打包）
5. **版本历史**：后端记录每次编辑的历史版本，支持回滚

### 阶段五：AI集成与同步
1. **AI工具集成**：
   - 用户设置中配置API Key（加密存储）
   - 支持 OpenAI、Claude、文心一言等主流API
   - 前端提供"测试提示词"按钮，直接调用API查看效果
   - 流式输出展示（SSE）
2. **云端同步**：
   - 基于用户ID的数据隔离
   - WebSocket或轮询机制实现多设备数据同步提示
   - 冲突处理策略（最后写入优先）

### 阶段六：优化与部署
1. 前端：响应式布局、暗黑模式、动画过渡
2. 后端：接口性能优化、SQL索引优化、Redis缓存策略
3. 部署：Docker Compose 一键启动
4. Nginx配置：前端静态资源 + API反向代理

## 关键设计决策

### 变量占位符实现
- 使用正则表达式 `\{\{(\w+)\}\}` 识别变量
- 存储格式：`variables_json` 字段保存变量默认值和类型
- 渲染时替换为实际值

### AI集成安全
- API Key 使用 AES 加密后存储在数据库
- 加密密钥通过环境变量注入，不提交到代码库
- 所有API调用通过后端代理，前端不直接暴露Key

### 多用户数据隔离
- 所有业务表都有 `user_id` 字段
- Service层通过ThreadLocal获取当前登录用户ID，自动附加查询条件
- 支持"公开"提示词（`is_public` 字段），用于未来社区功能扩展

## 验证方案
1. **本地启动**：`docker-compose up` 启动 MySQL + Redis
2. **后端测试**：运行Spring Boot应用，访问 `http://localhost:8080/swagger-ui.html` 查看API文档并测试接口
3. **前端测试**：`npm run dev` 启动开发服务器，验证页面功能
4. **端到端测试**：完整流程：注册 → 登录 → 创建分类 → 创建提示词（含变量）→ 搜索 → 导出 → AI测试
