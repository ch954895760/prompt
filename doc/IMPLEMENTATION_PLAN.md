# Prompt Vault 全栈开发计划

## 上下文

基于 `doc/splendid-tinkering-raven.md` 和 `doc/prototype.html`，开发一款全栈提示词管理软件 Prompt Vault。

用户决策：
- 目录结构遵循文档：`prompt-ui/` + `prompt-server/`
- AI 集成不调用真实 API，但实现配置层（base_url/api_key/model）供用户自主填写
- 不需要 Docker，用户本机有 Java/Node 环境
- 100% 还原原型图 UI
- 完整实现，不使用 mock 数据
- 完成后使用 Playwright 测试

---

## 技术栈

### 前端 (prompt-ui/)
- Vue 3 + Vite + TypeScript
- Element Plus（组件库）
- Pinia（状态管理）
- Vue Router 4
- Axios
- Tailwind CSS + SCSS
- Element Plus Icons / Lucide（原型图使用 lucide，保持统一）

### 后端 (prompt-server/)
- Spring Boot 3.x + Java 17
- MyBatis-Plus
- Spring Security + JWT
- MySQL 8.0
- Redis（会话、热点缓存）
- SpringDoc OpenAPI
- Maven

---

## 项目结构

```
prompt/
├── prompt-ui/                    # Vue3 前端
│   ├── src/
│   │   ├── api/                  # API 接口封装 (axios实例 + 各模块api)
│   │   ├── assets/               # 静态资源
│   │   ├── components/           # 公共组件
│   │   │   ├── PromptCard.vue
│   │   │   ├── VariableForm.vue
│   │   │   └── CategoryTree.vue
│   │   ├── views/                # 页面视图
│   │   │   ├── Login.vue
│   │   │   ├── Dashboard.vue
│   │   │   ├── PromptList.vue
│   │   │   ├── PromptEditor.vue
│   │   │   ├── CategoryManage.vue
│   │   │   └── Settings.vue
│   │   ├── router/               # 路由配置
│   │   ├── stores/               # Pinia 状态管理
│   │   ├── utils/                # 工具函数
│   │   ├── types/                # TypeScript 类型定义
│   │   └── App.vue
│   ├── package.json
│   ├── vite.config.ts
│   └── tsconfig.json
└── prompt-server/                # Spring Boot 后端
    ├── src/main/java/com/prompt
    │   ├── controller/
    │   ├── service/
    │   ├── mapper/
    │   ├── entity/
    │   ├── dto/
    │   ├── vo/
    │   ├── config/
    │   ├── security/
    │   ├── interceptor/
    │   ├── exception/
    │   └── util/
    └── pom.xml
```

---

## 数据库设计

### 用户表 (user)
```sql
CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    avatar VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### 提示词表 (prompt)
```sql
CREATE TABLE prompt (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    description VARCHAR(500),
    category_id BIGINT,
    variables_json JSON,
    is_public TINYINT DEFAULT 0,
    usage_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_category_id (category_id)
);
```

### 分类表 (category)
```sql
CREATE TABLE category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    parent_id BIGINT DEFAULT NULL,
    sort_order INT DEFAULT 0,
    icon VARCHAR(50),
    color VARCHAR(20) DEFAULT '#ea580c',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_parent_id (parent_id)
);
```

### 标签表 (tag)
```sql
CREATE TABLE tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    color VARCHAR(20) DEFAULT '#ea580c',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_name (user_id, name)
);
```

### 提示词-标签关联表 (prompt_tag)
```sql
CREATE TABLE prompt_tag (
    prompt_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (prompt_id, tag_id)
);
```

### 提示词历史表 (prompt_history)
```sql
CREATE TABLE prompt_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    prompt_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    version INT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_prompt_id (prompt_id)
);
```

### 用户设置表 (user_setting)
```sql
CREATE TABLE user_setting (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    theme VARCHAR(20) DEFAULT 'light',
    default_model VARCHAR(50),
    api_base_url VARCHAR(255),
    api_key_encrypted VARCHAR(255),
    model VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

---

## 实施阶段

### 阶段一：基础架构与项目初始化

1. **初始化前端项目**
   - `npm create vue@latest prompt-ui` → 选择 TypeScript, Router, Pinia
   - 安装依赖: element-plus, @element-plus/icons-vue, axios, tailwindcss, lucide-vue-next
   - 配置 vite.config.ts（按需导入、代理）
   - 配置 Tailwind CSS
   - 配置 Axios 实例（baseURL, interceptor）

2. **初始化后端项目**
   - Spring Initializr 方式: Spring Boot 3.2 + Java 17 + Spring Web + Spring Security + MySQL Driver + Redis
   - pom.xml 添加: mybatis-plus-boot-starter, jjwt, lombok, springdoc-openapi
   - 配置 application.yml（数据库、Redis、JWT）
   - 配置全局异常处理、统一响应封装（Result<T>）
   - 配置跨域 CORS

3. **初始化数据库**
   - 创建 `prompt_vault` 数据库
   - 执行上述建表 SQL

### 阶段二：用户认证与基础UI框架

**后端:**
- `AuthController`: POST /api/auth/register, POST /api/auth/login
- `UserService`: 注册（BCrypt加密）、登录（JWT签发）
- `JwtAuthenticationFilter`: 解析并验证 JWT Token
- `SecurityConfig`: 放行 /api/auth/**，其余需认证
- 刷新 Token 机制（accessToken + refreshToken）

**前端:**
- `Login.vue` 100% 还原原型图（登录/注册切换、表单验证、装饰背景）
- 路由守卫：未登录跳登录页
- Axios 拦截器：自动携带 Token、401 跳转登录
- Pinia `userStore`：管理登录状态和用户信息
- `App.vue` 主布局：侧边栏 + 顶部导航 + 内容区
- 全局主题切换（light/dark）持久化到 localStorage

### 阶段三：提示词核心功能

**后端:**
- `PromptController`: CRUD + 分页列表 + 搜索
- `CategoryController`: CRUD + 树形查询
- `TagController`: CRUD
- `PromptTagService`: 关联管理
- Service 层通过 `SecurityContextHolder` 自动附加 user_id 查询条件

**前端:**
- `Dashboard.vue`: 统计卡片、最近编辑列表（100%还原原型图）
- `PromptList.vue`: 卡片/列表视图、分类筛选、标签筛选、排序、分页
- `PromptEditor.vue`: 标题、分类选择、标签输入、内容编辑器（变量高亮）、变量动态表单、实时预览、复制、测试运行按钮
- `CategoryManage.vue`: 分类树形结构、新增/编辑/删除模态框、颜色选择、拖拽排序
- 变量识别正则: `/\{\{(\w+)\}\}/g`
- 预览面板实时替换变量值

### 阶段四：高级功能

**后端:**
- 搜索接口：MySQL `MATCH AGAINST` 全文搜索 + 标题/内容/标签联合 LIKE 查询
- 版本历史：每次更新 prompt 时插入 prompt_history 表
- 导入导出：
  - JSON 导出完整数据结构
  - Markdown 导出（每个提示词一个文件，ZIP打包）

**前端:**
- 全局搜索框实时防抖搜索
- 提示词详情页：版本历史列表、回滚功能
- 设置页导入导出按钮
- 一键复制最终生成文本

### 阶段五：AI 配置与设置

**后端:**
- `UserSettingController`: GET/PUT 用户设置
- API Key AES 加密存储（密钥从环境变量注入）
- 提供 AI 测试接口框架（接收 prompt，构造请求体，但用户需自行配置 base_url 和 key）

**前端:**
- `Settings.vue` 100%还原原型图
  - 个人信息（头像、用户名、邮箱）
  - AI 集成（默认模型下拉、base_url、api_key、model 输入，显隐切换）
  - 外观（深色模式开关）
  - 数据管理（导入/导出按钮）

### 阶段六：Playwright 测试

- 安装 `@playwright/test`
- 编写测试用例覆盖：
  1. 注册 → 登录流程
  2. 创建分类 → 创建提示词（含变量）→ 预览 → 复制
  3. 搜索提示词
  4. 编辑提示词 → 版本历史
  5. 导出数据
  6. 主题切换
  7. 设置 AI 配置
- 运行测试并修复问题

---

## 关键实现细节

### 变量系统
- 前端编辑器识别 `{{variable}}` 语法并高亮显示
- 提取变量后动态渲染输入表单
- 预览时替换为实际值，未填写的保留高亮

### 多用户数据隔离
- 所有业务表含 `user_id` 字段
- Service 层通过 `SecurityContextHolder.getContext().getAuthentication()` 获取当前用户 ID
- 所有查询自动附加 `user_id = ?` 条件

### AI 配置安全
- API Key 使用 AES 加密后存入数据库
- 加密密钥通过 `PROMPT_VAULT_AES_KEY` 环境变量注入
- 后端代理所有 AI 请求，前端不直接暴露 Key

### UI 还原要点
- 颜色系统：surface 色系 + accent (#ea580c) + ink
- 字体：Outfit (sans), Crimson Pro (serif/editor), JetBrains Mono (code)
- 组件效果：glass-card（hover边框高亮）、nav-item（active左侧指示条）、var-highlight（渐变背景）
- 动画：fadeIn, slideInLeft, scaleIn
- 响应式：lg 断点切换 sidebar 固定/滑出

---

## 验证方案

1. 本地启动 MySQL + Redis
2. 后端: `./mvnw spring-boot:run`，访问 `http://localhost:8080/swagger-ui.html`
3. 前端: `npm run dev`，访问 `http://localhost:5173`
4. Playwright: `npx playwright test`
5. 端到端验证：注册 → 登录 → 创建分类 → 创建提示词（含变量）→ 搜索 → 导出 → 编辑 → 版本回滚
