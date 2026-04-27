# 头像上传功能部署指南

## 方案概述

本方案实现了本地开发环境和生产环境的头像上传兼容：

- **本地环境**：使用相对路径存储，Spring Boot 直接提供静态文件访问
- **生产环境**：使用绝对路径存储，Nginx 提供静态文件访问

## 配置文件说明

### 1. 后端配置文件

#### application-dev.yml（本地开发）
```yaml
file:
  upload:
    path: uploads                    # 相对路径，保存在项目目录下
    url-prefix: ""                   # 空表示使用相对路径访问
```

#### application-prod.yml（生产环境）
```yaml
file:
  upload:
    path: /opt/prompt-server/uploads # 绝对路径，保存在固定位置
    url-prefix: ""                   # 空表示使用相对路径，通过Nginx访问
```

### 2. 前端组件

`AvatarUpload.vue` 会自动处理不同格式的头像URL：
- 完整URL（http/https开头）：直接使用
- `/uploads/` 开头的路径：通过Nginx或Spring Boot访问
- 其他路径：添加 `/api` 前缀兼容旧数据

## 生产环境部署步骤

### 1. 创建上传目录

```bash
# 创建上传目录
sudo mkdir -p /opt/prompt-server/uploads/avatars

# 设置目录权限（确保Java应用有写入权限）
sudo chown -R www-data:www-data /opt/prompt-server/uploads
sudo chmod -R 755 /opt/prompt-server/uploads
```

如果使用 systemd 运行Java应用，确保运行用户有权限：
```bash
# 查看Java应用运行用户
ps aux | grep java

# 根据实际用户设置权限，例如：
sudo chown -R your-app-user:your-app-user /opt/prompt-server/uploads
```

### 2. 配置Nginx

```bash
# 复制配置文件
sudo cp nginx/nginx.conf /etc/nginx/sites-available/prompt-server

# 修改配置文件中的域名和路径
sudo nano /etc/nginx/sites-available/prompt-server

# 创建软链接
sudo ln -s /etc/nginx/sites-available/prompt-server /etc/nginx/sites-enabled/

# 测试配置
sudo nginx -t

# 重启Nginx
sudo systemctl restart nginx
```

### 3. 部署后端应用

```bash
# 打包应用
mvn clean package -DskipTests

# 使用 prod 配置运行
java -jar -Dspring.profiles.active=prod prompt-server.jar
```

### 4. 部署前端应用

```bash
# 构建前端
cd prompt-ui
npm run build

# 复制到服务器
sudo mkdir -p /opt/prompt-ui
sudo cp -r dist /opt/prompt-ui/
```

## 常见问题

### 1. 上传成功但无法访问头像

**检查点：**
- 确认文件是否实际保存到 `/opt/prompt-server/uploads/avatars/`
- 检查Nginx配置中的 `location /uploads` 路径是否正确
- 检查目录权限：`ls -la /opt/prompt-server/uploads/`

**调试命令：**
```bash
# 查看Nginx错误日志
sudo tail -f /var/log/nginx/error.log

# 检查文件是否存在
curl -I http://your-domain/uploads/avatars/xxx.jpg
```

### 2. 上传失败

**检查点：**
- 后端日志查看具体错误
- 检查目录写入权限
- 检查文件大小限制（默认5MB）

### 3. 权限问题

如果Java应用无法写入目录：
```bash
# 方法1：更改目录所有者
sudo chown -R $(whoami):$(whoami) /opt/prompt-server/uploads

# 方法2：更改目录权限
sudo chmod -R 777 /opt/prompt-server/uploads

# 方法3：将运行用户添加到www-data组
sudo usermod -a -G www-data your-app-user
```

## 高级配置

### 使用CDN或独立域名访问头像

如果需要使用CDN或独立域名访问头像，修改 `application-prod.yml`：

```yaml
file:
  upload:
    path: /opt/prompt-server/uploads
    url-prefix: "https://cdn.your-domain.com"  # 配置CDN域名
```

此时头像URL会返回完整路径：`https://cdn.your-domain.com/uploads/avatars/xxx.jpg`

### 修改上传文件大小限制

**后端配置（application-prod.yml）：**
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 20MB      # 单个文件最大大小
      max-request-size: 100MB  # 整个请求最大大小
```

**Nginx配置：**
```nginx
server {
    client_max_body_size 20M;  # 与后端配置保持一致
}
```

## 目录结构参考

```
/opt/
├── prompt-server/
│   ├── uploads/              # 上传文件目录
│   │   └── avatars/          # 头像文件
│   └── prompt-server.jar     # 后端JAR包
├── prompt-ui/
│   └── dist/                 # 前端构建文件
└── ...
```
