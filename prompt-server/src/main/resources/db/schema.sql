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
