# Prompt Vault - 全栈提示词管理软件

## 项目概述

Prompt Vault 是一款面向多用户的云端同步 Web 应用，支持个人提示词库的建立、分类管理、变量模板、AI 集成测试等核心能力。

## 技术栈

### 前端 (prompt-ui/)
- **框架**: Vue 3 + Vite + TypeScript
- **UI 库**: Element Plus
- **状态管理**: Pinia
- **路由**: Vue Router 4
- **HTTP 客户端**: Axios
- **样式**: Tailwind CSS + SCSS
- **图标**: Lucide Vue Next

### 后端 (prompt-server/)
- **框架**: Spring Boot 3.x + Java 17
- **ORM**: MyBatis-Plus
- **安全**: Spring Security + JWT
- **数据库**: MySQL 8.0
- **缓存**: Redis
- **文档**: SpringDoc OpenAPI
- **构建**: Maven

## 项目结构

```
prompt/
├── doc/                          # 项目文档
│   ├── splendid-tinkering-raven.md   # 原始开发计划
│   ├── prototype.html            # UI 原型图
│   └── IMPLEMENTATION_PLAN.md    # 实施计划
├── prompt-ui/                    # Vue3 前端项目
│   ├── src/
│   │   ├── api/                  # API 接口封装
│   │   ├── assets/               # 静态资源
│   │   ├── components/           # 公共组件
│   │   ├── views/                # 页面视图
│   │   ├── router/               # 路由配置
│   │   ├── stores/               # Pinia 状态管理
│   │   ├── utils/                # 工具函数
│   │   └── types/                # TypeScript 类型定义
│   ├── package.json
│   └── vite.config.ts
└── prompt-server/                # Spring Boot 后端项目
    ├── src/main/java/com/prompt/
    │   ├── controller/           # REST API 控制器
    │   ├── service/              # 业务逻辑层
    │   ├── mapper/               # MyBatis-Plus Mapper
    │   ├── entity/               # 数据实体
    │   ├── dto/                  # 数据传输对象
    │   ├── vo/                   # 视图对象
    │   ├── config/               # 配置类
    │   ├── security/             # JWT + Spring Security
    │   ├── interceptor/          # 拦截器
    │   ├── exception/            # 全局异常处理
    │   └── util/                 # 工具类
    └── pom.xml
```

## 开发环境要求

- **Node.js**: >= 18
- **Java**: 17
- **Maven**: >= 3.8
- **MySQL**: 8.0
- **Redis**: >= 6.0

## 快速开始

### 1. 初始化数据库

```sql
CREATE DATABASE prompt_vault CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

应用启动时会自动创建表（配置 `spring.jpa.hibernate.ddl-auto=update` 或使用 Flyway）。

### 2. 启动后端

```bash
cd prompt-server
./mvnw spring-boot:run
```

- API 文档: http://localhost:8080/swagger-ui.html
- API 基础路径: http://localhost:8080/api

### 3. 启动前端

```bash
cd prompt-ui
npm install
npm run dev
```

- 开发服务器: http://localhost:5173
- 已配置代理，API 请求自动转发到后端

## 后端架构约定

### 统一响应格式

所有 API 返回 `Result<T>` 统一包装：

```java
public class Result<T> {
    private Integer code;      // 200 成功，其他为错误码
    private String message;
    private T data;
    private Long timestamp;
}
```

### 全局异常处理

使用 `@RestControllerAdvice` 统一处理异常：
- `BusinessException` -> 业务错误（400）
- `AuthenticationException` -> 认证失败（401）
- `AccessDeniedException` -> 无权限（403）
- 其他异常 -> 服务器错误（500）

### 数据隔离

所有业务表均包含 `user_id` 字段。Service 层通过 `SecurityContextHolder` 获取当前登录用户 ID，自动附加到查询条件中。

### JWT 认证

- Access Token: 有效期 2 小时
- Refresh Token: 有效期 7 天
- Token 前缀: `Bearer `

## API 规范

### 认证相关
- `POST /api/auth/register` - 用户注册
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/refresh` - 刷新 Token

### 提示词管理
- `GET /api/prompts` - 分页列表
- `GET /api/prompts/{id}` - 详情
- `POST /api/prompts` - 创建
- `PUT /api/prompts/{id}` - 更新
- `DELETE /api/prompts/{id}` - 删除
- `GET /api/prompts/search?q=xxx` - 搜索

### 分类管理
- `GET /api/categories` - 树形列表
- `POST /api/categories` - 创建
- `PUT /api/categories/{id}` - 更新
- `DELETE /api/categories/{id}` - 删除

### 标签管理
- `GET /api/tags` - 列表
- `POST /api/tags` - 创建
- `DELETE /api/tags/{id}` - 删除

### 用户设置
- `GET /api/settings` - 获取设置
- `PUT /api/settings` - 更新设置

## 前端架构约定

### 目录规范
- `views/` - 页面组件，与路由一一对应
- `components/` - 可复用组件
- `api/` - 按模块组织 API 函数
- `stores/` - Pinia store，按模块组织
- `types/` - TypeScript 接口定义

### 状态管理
- `userStore` - 用户信息、登录状态
- `promptStore` - 提示词列表、当前编辑
- `categoryStore` - 分类树
- `settingStore` - 应用设置

### UI 规范
- 颜色系统: surface 色系 + accent (#ea580c) + ink
- 字体: Outfit (sans), Crimson Pro (serif), JetBrains Mono (mono)
- 暗黑模式: `dark` class 切换，localStorage 持久化
- 响应式断点: lg (1024px)

## 数据库表结构

### 核心表
1. **user** - 用户表
2. **prompt** - 提示词表
3. **category** - 分类表（支持多级嵌套）
4. **tag** - 标签表
5. **prompt_tag** - 提示词-标签关联表
6. **prompt_history** - 提示词版本历史
7. **user_setting** - 用户设置（含 AI 配置）

详见 `doc/IMPLEMENTATION_PLAN.md` 中的完整建表语句。

## 安全规范

- 密码使用 BCrypt 加密
- API Key 使用 AES 加密存储，密钥通过环境变量 `PROMPT_VAULT_AES_KEY` 注入
- 所有 AI 请求通过后端代理，前端不直接暴露 Key
- JWT Token 用于 API 认证

## 测试

### Playwright E2E 测试

```bash
cd prompt-ui
npx playwright test
```

测试覆盖:
1. 注册/登录流程
2. 创建分类和提示词
3. 变量预览和复制
4. 搜索功能
5. 版本历史
6. 导入导出
7. 主题切换
8. AI 配置设置
