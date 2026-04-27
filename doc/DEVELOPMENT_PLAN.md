# Prompt Vault 功能扩展开发计划

> 基于现有功能架构的未来发展方向规划

---

## 📋 现有功能概览

### 核心功能
| 模块 | 已实现功能 |
|------|-----------|
| **用户系统** | 注册/登录/登出、JWT认证、头像上传、密码修改 |
| **提示词管理** | CRUD、搜索、分页、分类筛选、标签筛选 |
| **分类体系** | 多级嵌套分类、树形结构、拖拽排序、颜色/图标自定义 |
| **标签系统** | 标签CRUD、颜色自定义、多标签关联 |
| **变量模板** | `{{变量名}}`动态占位符、代码高亮预览 |
| **版本历史** | 提示词修改历史记录、版本回滚 |
| **AI测试** | 多Provider配置(OpenAI/Claude/Gemini/DeepSeek等)、流式响应、对话模式 |
| **导入导出** | JSON/Markdown格式导出、JSON导入 |
| **主题** | 浅色/深色模式切换 |
| **云端同步** | 多设备数据同步（基于用户体系） |

---

## 第一阶段：用户体验优化（低复杂度，高价值）

### 1. 快捷键系统

**功能描述**：为常用操作提供键盘快捷键支持，提升操作效率。

**快捷键映射**：
| 快捷键 | 功能 |
|--------|------|
| `Ctrl/Cmd + K` | 快速搜索提示词 |
| `Ctrl/Cmd + N` | 新建提示词 |
| `Ctrl/Cmd + S` | 保存提示词 |
| `Ctrl/Cmd + /` | 查看快捷键帮助 |
| `ESC` | 关闭弹窗/退出编辑 |
| `Ctrl/Cmd + D` | 复制当前提示词 |
| `Ctrl/Cmd + F` | 在编辑器中查找 |

**技术实现**：
- 前端使用 `vueuse/useMagicKeys` 或原生键盘事件监听
- 快捷键配置可自定义
- 首次使用显示快捷键引导

---

### 2. 提示词收藏/收藏夹

**功能描述**：允许用户收藏常用提示词，快速访问。

**数据模型扩展**：
```typescript
interface Prompt {
  isFavorite: boolean;
  favoriteAt?: string;
}
```

**功能点**：
- 提示词卡片添加收藏按钮（星标）
- 收藏夹独立视图
- 收藏排序（按收藏时间/名称/使用频率）
- 快速访问侧边栏收藏列表

**数据库变更**：
```sql
ALTER TABLE prompt ADD COLUMN is_favorite TINYINT DEFAULT 0;
ALTER TABLE prompt ADD COLUMN favorite_at DATETIME NULL;
CREATE INDEX idx_user_favorite ON prompt(user_id, is_favorite, favorite_at);
```

---

### 3. 最近使用记录

**功能描述**：记录用户最近使用的提示词，方便快速重新使用。

**数据模型**：
```typescript
interface PromptUsage {
  id: number;
  promptId: number;
  usedAt: string;
  context?: string; // 使用时的上下文（可选）
}
```

**功能点**：
- Dashboard显示最近使用列表（最近10条）
- 一键重新使用
- 使用频率统计

**数据库变更**：
```sql
CREATE TABLE prompt_usage_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    prompt_id BIGINT NOT NULL,
    context TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_time (user_id, created_at),
    INDEX idx_prompt (prompt_id)
);
```

---

### 4. 复制历史剪贴板

**功能描述**：记录用户最近复制的内容，提供快捷粘贴面板。

**功能点**：
- 记录最近复制的提示词（最多50条）
- 快捷键 `Ctrl/Cmd + Shift + V` 唤出剪贴板面板
- 支持搜索历史复制记录
- 一键重新复制

**技术实现**：
- 使用 `localStorage` 或 `IndexedDB` 本地存储
- 监听 `copy` 事件记录复制内容

---

## 第二阶段：协作与分享（中等复杂度）

### 5. 提示词分享功能

**功能描述**：生成分享链接，将提示词分享给他人。

**数据模型**：
```typescript
interface PromptShare {
  id: string; // 短链接ID（8位随机字符串）
  promptId: number;
  userId: number;
  shareType: 'public' | 'password' | 'expiry';
  password?: string;
  expiryAt?: string;
  accessCount: number;
  createdAt: string;
}
```

**功能点**：
- 生成分享链接（短链接格式：`/s/abc12345`）
- 三种分享模式：
  - 公开访问
  - 密码保护
  - 限时访问（24小时/7天/30天）
- 访问统计（访问次数、访问时间）
- 随时撤销分享

**数据库变更**：
```sql
CREATE TABLE prompt_share (
    id VARCHAR(16) PRIMARY KEY,
    prompt_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    share_type VARCHAR(20) NOT NULL DEFAULT 'public',
    password_hash VARCHAR(255),
    expiry_at DATETIME,
    access_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_prompt (prompt_id),
    INDEX idx_user (user_id)
);

CREATE TABLE prompt_share_access_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    share_id VARCHAR(16) NOT NULL,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    accessed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_share (share_id)
);
```

---

### 6. 提示词市场/模板库

**功能描述**：官方和用户共享的提示词模板库。

**功能点**：
- 官方精选提示词模板（按场景分类）
  - 写作创作
  - 编程开发
  - 设计创意
  - 学习研究
  - 办公效率
  - 生活助手
- 用户投稿审核机制
- 模板预览功能
- 一键导入到个人库
- 模板评分和评论

**数据库变更**：
```sql
CREATE TABLE prompt_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    description VARCHAR(500),
    category VARCHAR(50) NOT NULL,
    tags JSON,
    author_id BIGINT,
    is_official TINYINT DEFAULT 0,
    rating DECIMAL(2,1) DEFAULT 5.0,
    rating_count INT DEFAULT 0,
    import_count INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'pending', -- pending/approved/rejected
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_category (category),
    INDEX idx_status (status)
);

CREATE TABLE prompt_template_rating (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating INT NOT NULL,
    comment TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_template (user_id, template_id)
);
```

---

### 7. 团队工作空间

**功能描述**：支持团队协作，共享提示词库。

**数据模型**：
```typescript
interface Workspace {
  id: number;
  name: string;
  description?: string;
  ownerId: number;
  members: WorkspaceMember[];
  prompts: Prompt[]; // 共享提示词
  createdAt: string;
}

interface WorkspaceMember {
  userId: number;
  username: string;
  avatar?: string;
  role: 'owner' | 'admin' | 'editor' | 'viewer';
  joinedAt: string;
}
```

**功能点**：
- 创建工作空间
- 邀请成员（邮箱邀请/链接邀请）
- 角色权限管理：
  - Owner：完全控制
  - Admin：管理成员和设置
  - Editor：创建和编辑提示词
  - Viewer：仅查看和使用
- 共享分类和标签
- 团队使用统计
- 操作日志审计

**数据库变更**：
```sql
CREATE TABLE workspace (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    owner_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_owner (owner_id)
);

CREATE TABLE workspace_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    workspace_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'viewer',
    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_workspace_user (workspace_id, user_id),
    INDEX idx_user (user_id)
);

CREATE TABLE workspace_prompt (
    workspace_id BIGINT NOT NULL,
    prompt_id BIGINT NOT NULL,
    added_by BIGINT NOT NULL,
    added_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (workspace_id, prompt_id)
);
```

---

## 第三阶段：AI能力增强（中等复杂度）

### 8. 提示词优化助手

**功能描述**：AI辅助优化提示词内容，提供改进建议。

**功能点**：
- 提示词质量评分（1-10分）
- 自动优化建议：
  - 结构优化（添加角色设定、输出格式等）
  - 清晰度改进
  - 示例补充建议
- 一键应用优化建议
- 对比优化前后的效果

**技术实现**：
- 调用AI模型分析提示词
- 使用预设的优化Prompt模板
- 缓存优化结果避免重复调用

---

### 9. 批量AI测试

**功能描述**：同时测试多个提示词变体，对比效果。

**功能点**：
- 创建测试组（最多5个变体）
- 统一测试输入
- 并行调用AI获取结果
- 结果对比视图（并排显示）
- 性能统计（响应时间、token消耗）
- 导出测试结果

**数据模型**：
```typescript
interface PromptTestGroup {
  id: number;
  name: string;
  testInput: string;
  variants: PromptVariant[];
  results: TestResult[];
  createdAt: string;
}

interface PromptVariant {
  id: number;
  name: string;
  content: string;
}

interface TestResult {
  variantId: number;
  output: string;
  responseTime: number;
  tokenCount: number;
}
```

---

### 10. 提示词链/工作流

**功能描述**：将多个提示词串联成工作流，实现复杂任务自动化。

**数据模型**：
```typescript
interface PromptChain {
  id: number;
  name: string;
  description?: string;
  steps: ChainStep[];
  createdAt: string;
}

interface ChainStep {
  id: number;
  order: number;
  promptId: number;
  inputVars: string[]; // 输入变量
  outputVar: string;   // 输出变量名
  condition?: string;  // 条件分支（可选）
}
```

**功能点**：
- 可视化流程编辑器
- 步骤拖拽排序
- 变量传递配置
- 条件分支逻辑
- 执行日志和调试
- 保存为可复用模板

**应用场景**：
- 内容创作流水线：大纲 → 草稿 → 润色 → 翻译
- 代码生成流程：需求分析 → 代码生成 → 测试用例 → 文档
- 数据分析流程：数据清洗 → 分析 → 可视化 → 报告

---

## 第四阶段：数据与分析（中等复杂度）

### 11. 使用统计分析

**功能描述**：全面的使用数据统计和可视化分析。

**统计维度**：
```typescript
interface UsageStats {
  // 时间维度
  daily: { date: string; count: number }[];
  weekly: { week: string; count: number }[];
  monthly: { month: string; count: number }[];
  
  // 分类维度
  byCategory: { categoryId: number; categoryName: string; count: number }[];
  
  // 标签维度
  byTag: { tagId: number; tagName: string; count: number }[];
  
  // 提示词维度
  topPrompts: { promptId: number; title: string; usageCount: number }[];
  
  // 效率指标
  averageResponseTime: number;
  totalTokensConsumed: number;
}
```

**可视化图表**：
- 使用趋势折线图（日/周/月）
- 分类使用占比饼图
- 热门提示词排行柱状图
- 标签云图
- 效率指标仪表盘

**技术实现**：
- 前端使用 `echarts` 或 `chart.js`
- 后端聚合统计数据
- 支持自定义时间范围筛选

---

### 12. 数据备份与恢复

**功能描述**：自动备份和手动备份机制，保障数据安全。

**功能点**：
- 自动云端备份（每日/每周）
- 手动触发备份
- 备份历史版本管理（保留最近30个版本）
- 跨账号数据迁移
- 选择性恢复（单个提示词/分类/全部）
- 导出加密备份文件

**数据库变更**：
```sql
CREATE TABLE data_backup (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    backup_name VARCHAR(100),
    backup_type VARCHAR(20), -- auto/manual
    file_path VARCHAR(500),
    file_size BIGINT,
    prompt_count INT,
    category_count INT,
    tag_count INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id)
);
```

---

### 13. 高级搜索

**功能描述**：更强大的搜索功能，支持多条件组合。

**搜索条件**：
- 关键词搜索（标题/内容/描述）
- 分类筛选（多选）
- 标签筛选（多选）
- 时间范围（创建时间/更新时间）
- 使用次数范围
- 是否收藏
- 是否公开

**功能点**：
- 搜索条件保存（保存常用搜索）
- 搜索历史记录
- 搜索结果排序（相关度/时间/使用频率）
- 高亮显示匹配内容

**技术实现**：
- 后端使用 Elasticsearch 或 MySQL 全文索引
- 前端搜索条件面板
- 搜索URL可分享

---

## 第五阶段：集成与扩展（高复杂度）

### 14. 浏览器插件

**功能描述**：浏览器扩展，实现网页与Prompt Vault的无缝集成。

**功能点**：
- 网页划词快速保存为提示词
- 右键菜单快速调用提示词
- 侧边栏常驻面板
- 支持ChatGPT/Claude等网页版AI工具集成
  - 一键发送提示词
  - 自动填充变量
- 网页内容提取生成提示词

**技术栈**：
- Manifest V3 扩展开发
- 支持 Chrome/Edge/Firefox
- 与主应用API通信

---

### 15. API开放接口

**功能描述**：开放API供第三方应用集成。

**API设计**：
```yaml
openapi: 3.0.0
info:
  title: Prompt Vault API
  version: 1.0.0

paths:
  /api/v1/prompts:
    get:
      summary: 获取提示词列表
      parameters:
        - name: category_id
          in: query
          schema:
            type: integer
        - name: tag_id
          in: query
          schema:
            type: integer
        - name: keyword
          in: query
          schema:
            type: string
    post:
      summary: 创建提示词
      
  /api/v1/prompts/{id}:
    get:
      summary: 获取提示词详情
    put:
      summary: 更新提示词
    delete:
      summary: 删除提示词
      
  /api/v1/prompts/{id}/use:
    post:
      summary: 使用提示词
      
  /api/v1/prompts/search:
    get:
      summary: 搜索提示词
      
  /api/v1/categories:
    get:
      summary: 获取分类列表
      
  /api/v1/tags:
    get:
      summary: 获取标签列表
```

**功能点**：
- API Key管理
- 请求频率限制
- 使用统计
- Webhook支持（提示词更新通知）
- SDK开发（Python/JavaScript）

---

### 16. 桌面客户端

**功能描述**：基于Electron的跨平台桌面应用。

**功能点**：
- 系统托盘快捷访问
- 全局快捷键唤出
- 离线模式支持（本地SQLite存储）
- 自动同步（联网时同步到云端）
- 本地AI模型支持（Ollama集成）
- 开机自启动

**技术栈**：
- Electron + Vue3
- 本地SQLite数据库
- 自动更新机制

---

### 17. 移动端适配/PWA

**功能描述**：移动端优化和渐进式Web应用支持。

**功能点**：
- 响应式布局优化
- 触摸手势支持
- 离线缓存（Service Worker）
- 添加到主屏幕
- 推送通知（新功能/备份提醒）
- 移动端专属UI组件

---

## 第六阶段：高级功能（高复杂度）

### 18. 智能标签推荐

**功能描述**：基于AI自动推荐标签和分类。

**功能点**：
- 基于内容自动推荐标签（Top 5）
- 相似提示词检测（避免重复创建）
- 自动分类建议
- 标签云智能排序
- 用户反馈优化推荐算法

---

### 19. 提示词版本对比

**功能描述**：可视化对比不同版本的差异。

**功能点**：
- 文本Diff对比（行级/字符级）
- 变更高亮显示（新增/删除/修改）
- 合并冲突解决（多人协作场景）
- 版本差异统计
- 一键回滚到任意版本

**技术实现**：
- 使用 `diff-match-patch` 库
- 自定义差异渲染组件

---

### 20. 多语言支持

**功能描述**：界面国际化和提示词内容翻译。

**功能点**：
- 界面多语言（中文/英文/日文/韩文等）
- 提示词内容翻译
- 自动语言检测
- 翻译历史记录
- 支持专业术语词典

**技术实现**：
- 前端使用 `vue-i18n`
- 后端使用消息资源文件
- 集成翻译API（DeepL/Google Translate）

---

## 📊 推荐实施优先级

### 高优先级（立即实施）
适合快速迭代，提升用户体验的功能：

1. **快捷键系统** - 开发周期：2-3天
2. **提示词收藏功能** - 开发周期：3-4天
3. **最近使用记录** - 开发周期：2-3天
4. **高级搜索** - 开发周期：5-7天

### 中优先级（1-2个月）
需要一定开发量，但能显著提升产品价值：

5. **使用统计分析** - 开发周期：7-10天
6. **提示词分享功能** - 开发周期：5-7天
7. **提示词优化助手** - 开发周期：7-10天
8. **浏览器插件雏形** - 开发周期：10-14天

### 低优先级（长期规划）
需要大量开发资源，可作为产品差异化功能：

9. **团队工作空间** - 开发周期：3-4周
10. **提示词市场** - 开发周期：3-4周
11. **桌面客户端** - 开发周期：4-6周
12. **API开放平台** - 开发周期：3-4周

---

## 💡 技术实现建议

### 前端扩展
```bash
# 新增依赖建议
npm install @vueuse/core        # 快捷键、剪贴板等功能
npm install echarts             # 数据可视化
npm install localforage         # 本地存储
npm install diff-match-patch    # 文本对比
npm register-service-worker     # PWA支持
```

### 后端扩展
```xml
<!-- pom.xml 新增依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

### 数据库扩展
```sql
-- 通用索引优化
CREATE INDEX idx_prompt_user_updated ON prompt(user_id, updated_at DESC);
CREATE INDEX idx_prompt_user_usage ON prompt(user_id, usage_count DESC);
CREATE INDEX idx_prompt_public ON prompt(is_public, created_at DESC);
```

---

## 📅 开发里程碑建议

### Milestone 1: 体验优化（第1个月）
- [ ] 快捷键系统
- [ ] 提示词收藏
- [ ] 最近使用记录
- [ ] 复制历史剪贴板

### Milestone 2: 数据与分享（第2-3个月）
- [ ] 高级搜索
- [ ] 使用统计分析
- [ ] 提示词分享功能
- [ ] 数据备份与恢复

### Milestone 3: AI增强（第4-5个月）
- [ ] 提示词优化助手
- [ ] 批量AI测试
- [ ] 提示词链/工作流

### Milestone 4: 生态扩展（第6-12个月）
- [ ] 浏览器插件
- [ ] 移动端PWA
- [ ] 团队工作空间
- [ ] 提示词市场

---

*文档版本：v1.0*  
*最后更新：2026-04-27*
