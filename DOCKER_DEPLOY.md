# Docker 部署指南

本文档详细说明如何使用 Docker 部署闲鱼自动化管理系统。

## 📋 前置要求

- Docker 20.10+
- Docker Compose 2.0+

### 安装 Docker

**Windows/Mac:**
- 下载并安装 [Docker Desktop](https://www.docker.com/products/docker-desktop)

**Linux (Ubuntu/Debian):**
```bash
# 安装 Docker
curl -fsSL https://get.docker.com | sh

# 安装 Docker Compose
sudo apt-get update
sudo apt-get install docker-compose-plugin

# 将当前用户添加到 docker 组
sudo usermod -aG docker $USER
```

## 🚀 快速部署

### 方式一：使用 Docker Compose（推荐）

这是最简单的部署方式，一条命令即可启动。

```bash
# 1. 克隆项目
# Gitee (国内推荐)
git clone https://gitee.com/lzy2018cn/xian-yu-assistant.git

# 或 GitHub
git clone https://github.com/IAMLZY2018/-XianYuAssistant.git

cd xian-yu-assistant

# 2. 启动服务
docker-compose up -d

# 3. 查看日志
docker-compose logs -f

# 4. 访问应用
# 打开浏览器访问: http://localhost:12400
```

**常用命令:**

```bash
# 停止服务
docker-compose down

# 重启服务
docker-compose restart

# 查看服务状态
docker-compose ps

# 查看实时日志
docker-compose logs -f

# 更新并重启服务
docker-compose pull
docker-compose up -d
```

### 方式二：手动构建和运行

如果你想更灵活地控制构建过程：

```bash
# 1. 构建镜像
docker build -t xianyu-assistant:latest .

# 2. 创建数据目录
mkdir -p data logs

# 3. 运行容器
docker run -d \
  --name xianyu-assistant \
  -p 8080:8080 \
  -v $(pwd)/data:/app/data \
  -v $(pwd)/logs:/app/logs \
  -e JAVA_OPTS="-Xms256m -Xmx512m" \
  -e TZ=Asia/Shanghai \
  --restart unless-stopped \
  xianyu-assistant:latest

# 4. 查看日志
docker logs -f xianyu-assistant

# 5. 停止容器
docker stop xianyu-assistant

# 6. 删除容器
docker rm xianyu-assistant
```

## 📁 数据持久化

Docker 部署会自动创建以下目录用于数据持久化：

```
xianyu-assistant/
├── data/              # 数据库文件
│   └── xianyu_assistant.db
└── logs/              # 应用日志
    └── 2024-01-01/
```

**重要提示:**
- 这些目录会被挂载到容器中，数据不会因容器重启而丢失
- 定期备份 `data` 目录以防数据丢失
- 日志文件会按日期自动分类存储

## ⚙️ 配置说明

### 环境变量

在 `docker-compose.yml` 中可以配置以下环境变量：

```yaml
environment:
  # JVM 内存配置
  - JAVA_OPTS=-Xms256m -Xmx512m
  
  # 时区设置
  - TZ=Asia/Shanghai
  
  # Spring 配置文件
  - SPRING_PROFILES_ACTIVE=prod
```

### 端口映射

默认映射端口为 `12400:12400`，如需修改：

```yaml
ports:
  - "9090:12400"  # 将容器的 12400 端口映射到主机的 9090 端口
```

### 资源限制

如需限制容器资源使用，在 `docker-compose.yml` 中添加：

```yaml
services:
  xianyu-assistant:
    # ... 其他配置
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 1G
        reservations:
          cpus: '0.5'
          memory: 512M
```

## 🔧 故障排查

### 1. 容器无法启动

**查看日志:**
```bash
docker-compose logs xianyu-assistant
```

**常见原因:**
- 端口 12400 已被占用 → 修改端口映射
- 内存不足 → 增加 JAVA_OPTS 中的内存配置
- 权限问题 → 确保 data 和 logs 目录有写权限

### 2. 无法访问应用

**检查容器状态:**
```bash
docker-compose ps
```

**检查端口映射:**
```bash
docker port xianyu-assistant
```

**检查防火墙:**
```bash
# Linux
sudo ufw allow 12400

# Windows
# 在 Windows 防火墙中允许 12400 端口
```

### 3. 数据丢失

**检查挂载卷:**
```bash
docker inspect xianyu-assistant | grep Mounts -A 20
```

**备份数据:**
```bash
# 备份数据库
cp -r data data_backup_$(date +%Y%m%d)

# 恢复数据库
cp -r data_backup_20240101 data
docker-compose restart
```

### 4. 构建失败

**清理缓存重新构建:**
```bash
docker-compose build --no-cache
docker-compose up -d
```

**检查磁盘空间:**
```bash
df -h
docker system df
```

**清理无用镜像:**
```bash
docker system prune -a
```

## 🔄 更新应用

### 方式一：使用 Docker Compose

```bash
# 1. 拉取最新代码
git pull

# 2. 重新构建并启动
docker-compose up -d --build

# 3. 查看日志确认启动成功
docker-compose logs -f
```

### 方式二：手动更新

```bash
# 1. 停止并删除旧容器
docker stop xianyu-assistant
docker rm xianyu-assistant

# 2. 删除旧镜像
docker rmi xianyu-assistant:latest

# 3. 拉取最新代码
git pull

# 4. 重新构建镜像
docker build -t xianyu-assistant:latest .

# 5. 启动新容器
docker run -d \
  --name xianyu-assistant \
  -p 12400:12400 \
  -v $(pwd)/data:/app/data \
  -v $(pwd)/logs:/app/logs \
  --restart unless-stopped \
  xianyu-assistant:latest
```

## 📊 监控和维护

### 查看资源使用情况

```bash
# 查看容器资源使用
docker stats xianyu-assistant

# 查看容器详细信息
docker inspect xianyu-assistant
```

### 健康检查

应用内置了健康检查端点，Docker 会自动监控：

```bash
# 手动检查健康状态
curl http://localhost:12400/api/health

# 查看健康检查日志
docker inspect --format='{{json .State.Health}}' xianyu-assistant | jq
```

### 日志管理

```bash
# 查看最近 100 行日志
docker-compose logs --tail=100 xianyu-assistant

# 查看实时日志
docker-compose logs -f xianyu-assistant

# 清理日志（谨慎操作）
docker-compose logs --no-log-prefix xianyu-assistant > backup.log
truncate -s 0 $(docker inspect --format='{{.LogPath}}' xianyu-assistant)
```

## 🌐 生产环境部署建议

### 1. 使用反向代理

推荐使用 Nginx 作为反向代理：

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://localhost:12400;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # WebSocket 支持
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

### 2. 配置 HTTPS

使用 Let's Encrypt 免费证书：

```bash
# 安装 certbot
sudo apt-get install certbot python3-certbot-nginx

# 获取证书
sudo certbot --nginx -d your-domain.com

# 自动续期
sudo certbot renew --dry-run
```

### 3. 定期备份

创建备份脚本 `backup.sh`:

```bash
#!/bin/bash
BACKUP_DIR="/backup/xianyu-assistant"
DATE=$(date +%Y%m%d_%H%M%S)

# 创建备份目录
mkdir -p $BACKUP_DIR

# 备份数据库
tar -czf $BACKUP_DIR/data_$DATE.tar.gz data/

# 保留最近 7 天的备份
find $BACKUP_DIR -name "data_*.tar.gz" -mtime +7 -delete

echo "Backup completed: $BACKUP_DIR/data_$DATE.tar.gz"
```

添加到 crontab:
```bash
# 每天凌晨 2 点备份
0 2 * * * /path/to/backup.sh
```

### 4. 监控告警

使用 Docker 自带的监控或第三方工具：

```bash
# 使用 docker stats 监控
docker stats xianyu-assistant --no-stream

# 或使用 Prometheus + Grafana
# 参考: https://prometheus.io/docs/guides/dockerswarm/
```

## 🔐 安全建议

1. **不要暴露敏感端口** - 只开放必要的端口
2. **定期更新** - 及时更新 Docker 和应用版本
3. **使用非 root 用户** - 在 Dockerfile 中创建专用用户
4. **限制资源** - 设置内存和 CPU 限制
5. **备份数据** - 定期备份数据库文件
6. **监控日志** - 定期检查应用日志

## 📞 获取帮助

如果遇到问题：

1. 查看 [常见问题](README.md#常见问题)
2. 查看容器日志: `docker-compose logs -f`
3. 提交 [Issue (Gitee)](https://gitee.com/lzy2018cn/xian-yu-assistant/issues) 或 [Issue (GitHub)](https://github.com/IAMLZY2018/-XianYuAssistant/issues)

---

**注意**: Docker 部署不需要上传镜像到 Docker Hub，用户可以在本地直接构建和运行。
