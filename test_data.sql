-- ============================================
-- 提示词管理系统测试数据
-- ============================================

USE prompt_vault;

-- 1. 用户表测试数据
INSERT INTO user (id, username, email, password_hash, avatar, created_at, updated_at) VALUES
(1, 'admin', 'admin@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO', 'https://api.dicebear.com/7.x/avataaars/svg?seed=admin', '2024-01-15 10:00:00', '2024-01-15 10:00:00'),
(2, 'zhangsan', 'zhangsan@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO', 'https://api.dicebear.com/7.x/avataaars/svg?seed=zhangsan', '2024-01-16 14:30:00', '2024-01-16 14:30:00'),
(3, 'lisi', 'lisi@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO', 'https://api.dicebear.com/7.x/avataaars/svg?seed=lisi', '2024-01-17 09:15:00', '2024-01-17 09:15:00');

-- 2. 分类表测试数据
INSERT INTO category (id, user_id, name, parent_id, sort_order, icon, color, created_at) VALUES
(1, 1, '编程开发', NULL, 1, 'Code', '#3b82f6', '2024-01-15 10:30:00'),
(2, 1, '前端开发', 1, 1, 'Layout', '#60a5fa', '2024-01-15 10:35:00'),
(3, 1, '后端开发', 1, 2, 'Server', '#3b82f6', '2024-01-15 10:40:00'),
(4, 1, '写作辅助', NULL, 2, 'PenTool', '#10b981', '2024-01-15 11:00:00'),
(5, 2, '学习笔记', NULL, 1, 'BookOpen', '#f59e0b', '2024-01-16 15:00:00'),
(6, 2, '项目管理', NULL, 2, 'FolderKanban', '#8b5cf6', '2024-01-16 15:30:00');

-- 3. 标签表测试数据
INSERT INTO tag (id, user_id, name, color, created_at) VALUES
(1, 1, 'JavaScript', '#f7df1e', '2024-01-15 10:45:00'),
(2, 1, 'Python', '#3776ab', '2024-01-15 10:50:00'),
(3, 1, 'React', '#61dafb', '2024-01-15 11:00:00'),
(4, 1, 'Vue', '#4fc08d', '2024-01-15 11:05:00'),
(5, 1, 'AI', '#ea580c', '2024-01-15 11:10:00'),
(6, 2, '学习', '#10b981', '2024-01-16 15:45:00'),
(7, 2, '工作', '#3b82f6', '2024-01-16 16:00:00'),
(8, 3, '笔记', '#8b5cf6', '2024-01-17 09:30:00');

-- 4. 提示词表测试数据
INSERT INTO prompt (id, user_id, title, content, description, category_id, variables_json, is_public, usage_count, created_at, updated_at) VALUES
(1, 1, '代码审查助手', '请作为资深代码审查员，审查以下代码：\n\n```\n{{code}}\n```\n\n请从以下几个方面进行分析：\n1. 代码质量和可读性\n2. 潜在的性能问题\n3. 安全漏洞\n4. 最佳实践遵循情况\n5. 改进建议', '帮助开发者进行代码审查，提供专业的改进建议', 3, '[{"name": "code", "label": "待审查代码", "type": "textarea", "required": true}]', 1, 25, '2024-01-15 12:00:00', '2024-01-20 10:00:00'),

(2, 1, 'React组件生成器', '请帮我创建一个React组件，要求如下：\n\n组件名称：{{componentName}}\n功能描述：{{description}}\n是否需要TypeScript：{{useTypeScript}}\n\n请提供完整的组件代码，包括：\n1. 组件实现\n2. Props类型定义（如使用TS）\n3. 使用示例\n4. 必要的样式', '快速生成React组件代码模板', 2, '[{"name": "componentName", "label": "组件名称", "type": "text", "required": true}, {"name": "description", "label": "功能描述", "type": "textarea", "required": true}, {"name": "useTypeScript", "label": "使用TypeScript", "type": "select", "options": ["是", "否"], "required": true}]', 1, 42, '2024-01-16 09:00:00', '2024-01-18 15:30:00'),

(3, 1, 'SQL优化专家', '请帮我优化以下SQL查询：\n\n```sql\n{{sql}}\n```\n\n数据库类型：{{dbType}}\n\n请分析并提供：\n1. 当前查询的性能问题\n2. 优化后的SQL语句\n3. 索引建议\n4. 执行计划分析', 'SQL查询优化专家，提供性能改进建议', 3, '[{"name": "sql", "label": "SQL语句", "type": "textarea", "required": true}, {"name": "dbType", "label": "数据库类型", "type": "select", "options": ["MySQL", "PostgreSQL", "Oracle", "SQL Server"], "required": true}]', 0, 18, '2024-01-17 14:00:00', '2024-01-17 14:00:00'),

(4, 2, '文章标题生成器', '请为以下文章主题生成10个吸引人的标题：\n\n主题：{{topic}}\n目标读者：{{audience}}\n风格：{{style}}\n\n要求：\n1. 标题简洁有力\n2. 包含关键词\n3. 具有吸引力\n4. 适合SEO优化', '帮助内容创作者生成优质文章标题', 4, '[{"name": "topic", "label": "文章主题", "type": "text", "required": true}, {"name": "audience", "label": "目标读者", "type": "text", "required": true}, {"name": "style", "label": "标题风格", "type": "select", "options": ["专业严谨", "轻松活泼", "悬念吸引", "数字列表"], "required": true}]', 1, 56, '2024-01-18 10:00:00', '2024-01-19 16:00:00'),

(5, 2, '会议纪要整理', '请将以下会议记录整理成结构化的会议纪要：\n\n原始记录：\n{{notes}}\n\n请按以下格式输出：\n1. 会议基本信息（时间、地点、参与人）\n2. 会议议题\n3. 讨论要点\n4. 决议事项\n5. 行动计划（责任人+截止日期）', '自动整理会议纪要，提取关键信息', 6, '[{"name": "notes", "label": "会议记录", "type": "textarea", "required": true}]', 0, 33, '2024-01-19 11:30:00', '2024-01-19 11:30:00'),

(6, 1, 'API文档生成器', '请根据以下代码生成标准的API文档：\n\n```\n{{code}}\n```\n\n文档格式：{{format}}\n\n请包含：\n1. 接口描述\n2. 请求参数说明\n3. 响应数据格式\n4. 错误码说明\n5. 调用示例', '自动生成API接口文档', 3, '[{"name": "code", "label": "API代码", "type": "textarea", "required": true}, {"name": "format", "label": "文档格式", "type": "select", "options": ["OpenAPI/Swagger", "Markdown", "Postman Collection"], "required": true}]', 1, 29, '2024-01-20 09:00:00', '2024-01-21 10:00:00'),

(7, 3, '学习计划制定', '请帮我制定一个学习计划：\n\n学习目标：{{goal}}\n可用时间：{{time}}\n当前水平：{{level}}\n\n请提供：\n1. 学习路径规划\n2. 阶段性目标\n3. 推荐学习资源\n4. 时间安排建议\n5. 自我检测方法', '个性化学习计划制定助手', 5, '[{"name": "goal", "label": "学习目标", "type": "text", "required": true}, {"name": "time", "label": "每周可用时间", "type": "text", "required": true}, {"name": "level", "label": "当前水平", "type": "select", "options": ["零基础", "初级", "中级", "高级"], "required": true}]', 0, 12, '2024-01-21 14:00:00', '2024-01-21 14:00:00'),

(8, 1, 'Bug分析助手', '请帮我分析以下Bug：\n\n错误信息：\n```\n{{error}}\n```\n\n相关代码：\n```\n{{code}}\n```\n\n技术栈：{{techStack}}\n\n请分析：\n1. 错误原因\n2. 解决方案\n3. 预防措施\n4. 相关最佳实践', '帮助开发者快速定位和解决Bug', 1, '[{"name": "error", "label": "错误信息", "type": "textarea", "required": true}, {"name": "code", "label": "相关代码", "type": "textarea", "required": false}, {"name": "techStack", "label": "技术栈", "type": "text", "required": true}]', 1, 67, '2024-01-22 10:00:00', '2024-01-23 09:00:00');

-- 5. 提示词-标签关联表测试数据
INSERT INTO prompt_tag (prompt_id, tag_id) VALUES
(1, 2), (1, 5),
(2, 1), (2, 3),
(3, 2), (3, 5),
(4, 5),
(5, 7),
(6, 2), (6, 3),
(7, 6), (7, 8),
(8, 1), (8, 3), (8, 5);

-- 6. 提示词历史表测试数据
INSERT INTO prompt_history (id, prompt_id, content, version, created_at) VALUES
(1, 1, '请审查以下代码并提供改进建议：\n{{code}}', 1, '2024-01-15 12:00:00'),
(2, 1, '请作为代码审查员，审查以下代码并提供建议：\n{{code}}', 2, '2024-01-16 10:00:00'),
(3, 1, '请作为资深代码审查员，审查以下代码：\n\n```\n{{code}}\n```\n\n请从以下几个方面进行分析...', 3, '2024-01-20 10:00:00'),
(4, 2, '创建一个React组件：{{componentName}}', 1, '2024-01-16 09:00:00'),
(5, 2, '请帮我创建一个React组件，要求如下：\n组件名称：{{componentName}}...', 2, '2024-01-18 15:30:00'),
(6, 8, '分析Bug：{{error}}', 1, '2024-01-22 10:00:00'),
(7, 8, '请帮我分析以下Bug并提供解决方案...', 2, '2024-01-23 09:00:00');

-- 7. 用户设置表测试数据
INSERT INTO user_setting (id, user_id, theme, default_model, api_base_url, api_key_encrypted, model, created_at, updated_at) VALUES
(1, 1, 'dark', 'minimax', 'https://api.minimaxi.com/v1', '加密后的API_KEY_1', 'MiniMax-M2.7', '2024-01-15 10:00:00', '2024-01-20 15:00:00'),
(2, 2, 'light', 'openai', 'https://api.openai.com/v1', '加密后的API_KEY_2', 'gpt-4', '2024-01-16 14:30:00', '2024-01-16 14:30:00'),
(3, 3, 'light', NULL, NULL, NULL, NULL, '2024-01-17 09:15:00', '2024-01-17 09:15:00');

-- 8. AI提供商配置表测试数据（系统级 + 用户级）
INSERT INTO ai_provider (id, user_id, name, provider, api_base_url, api_key_encrypted, model, is_default, sort_order, created_at, updated_at) VALUES
-- 系统级公共模型
(1, NULL, 'MiniMax 公共模型', 'minimax', 'https://api.minimaxi.com/v1', '加密后的系统API_KEY_1', 'MiniMax-M2.7', 1, 0, '2024-01-01 00:00:00', '2024-01-01 00:00:00'),
(2, NULL, 'OpenAI GPT-4', 'openai', 'https://api.openai.com/v1', '加密后的系统API_KEY_2', 'gpt-4', 0, 1, '2024-01-01 00:00:00', '2024-01-01 00:00:00'),
-- 用户个人模型
(3, 1, '我的MiniMax', 'minimax', 'https://api.minimaxi.com/v1', '用户1加密的API_KEY', 'MiniMax-Text-01', 1, 0, '2024-01-15 10:30:00', '2024-01-20 15:00:00'),
(4, 1, '我的Claude', 'claude', 'https://api.anthropic.com', '用户1加密的Claude_KEY', 'claude-3-opus-20240229', 0, 1, '2024-01-18 11:00:00', '2024-01-18 11:00:00'),
(5, 2, '个人OpenAI', 'openai', 'https://api.openai.com/v1', '用户2加密的API_KEY', 'gpt-3.5-turbo', 1, 0, '2024-01-16 15:00:00', '2024-01-16 15:00:00');

-- 9. 提示词使用记录表测试数据
INSERT INTO prompt_usage_log (id, user_id, prompt_id, context, created_at) VALUES
(1, 1, 1, '{"code_length": 1500, "language": "java"}', '2024-01-20 10:30:00'),
(2, 1, 1, '{"code_length": 800, "language": "python"}', '2024-01-21 14:00:00'),
(3, 1, 2, '{"component_type": "Button", "complexity": "medium"}', '2024-01-22 09:00:00'),
(4, 2, 4, '{"topic": "人工智能", "generated_titles": 10}', '2024-01-23 11:00:00'),
(5, 2, 5, '{"meeting_duration": "2h", "participants": 8}', '2024-01-24 16:00:00'),
(6, 1, 8, '{"error_type": "NullPointerException", "resolved": true}', '2024-01-25 10:00:00'),
(7, 3, 7, '{"goal": "学习Python", "plan_weeks": 12}', '2024-01-26 14:30:00');

-- 重置自增ID（可选，如果需要固定ID）
-- ALTER TABLE user AUTO_INCREMENT = 4;
-- ALTER TABLE category AUTO_INCREMENT = 7;
-- ALTER TABLE tag AUTO_INCREMENT = 9;
-- ALTER TABLE prompt AUTO_INCREMENT = 9;
-- ALTER TABLE prompt_history AUTO_INCREMENT = 8;
-- ALTER TABLE user_setting AUTO_INCREMENT = 4;
-- ALTER TABLE ai_provider AUTO_INCREMENT = 6;
-- ALTER TABLE prompt_usage_log AUTO_INCREMENT = 8;
