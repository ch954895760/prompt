CREATE DATABASE IF NOT EXISTS prompt_vault CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE prompt_vault;

-- 用户表
CREATE TABLE IF NOT EXISTS user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    avatar VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 分类表
CREATE TABLE IF NOT EXISTS category (
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

-- 标签表
CREATE TABLE IF NOT EXISTS tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    color VARCHAR(20) DEFAULT '#ea580c',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_name (user_id, name),
    INDEX idx_user_id (user_id)
);

-- 提示词表
CREATE TABLE IF NOT EXISTS prompt (
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

-- 提示词-标签关联表
CREATE TABLE IF NOT EXISTS prompt_tag (
    prompt_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (prompt_id, tag_id)
);

-- 提示词历史表
CREATE TABLE IF NOT EXISTS prompt_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    prompt_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    version INT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_prompt_id (prompt_id)
);

-- 用户设置表
CREATE TABLE IF NOT EXISTS user_setting (
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

-- AI提供商配置表
CREATE TABLE IF NOT EXISTS ai_provider (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT DEFAULT NULL COMMENT '用户ID，NULL表示系统级公共模型',
    name VARCHAR(100) NOT NULL COMMENT '配置名称',
    provider VARCHAR(50) NOT NULL COMMENT '提供商类型: openai, claude, gemini, minimax等',
    api_base_url VARCHAR(255) NOT NULL COMMENT 'API基础URL',
    api_key_encrypted VARCHAR(255) NOT NULL COMMENT '加密的API Key',
    model VARCHAR(100) NOT NULL COMMENT '模型名称',
    is_default TINYINT DEFAULT 0 COMMENT '是否默认',
    sort_order INT DEFAULT 0 COMMENT '排序顺序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_user_default (user_id, is_default)
);

-- 系统AI提供商初始化数据
-- 注意：执行此SQL前，需要先用AesUtil加密API Key，然后将加密后的值填入api_key_encrypted字段
-- INSERT INTO ai_provider (user_id, name, provider, api_base_url, api_key_encrypted, model, is_default, sort_order) VALUES
-- (NULL, 'MiniMax 公共模型', 'minimax', 'https://api.minimaxi.com/v1', '加密后的API_KEY', 'MiniMax-M2.7', 1, 0);

-- 提示词使用记录表
CREATE TABLE IF NOT EXISTS prompt_usage_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    prompt_id BIGINT NOT NULL,
    context TEXT COMMENT '使用时的上下文信息',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_time (user_id, created_at),
    INDEX idx_prompt (prompt_id),
    INDEX idx_user_prompt (user_id, prompt_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提示词使用记录表';
