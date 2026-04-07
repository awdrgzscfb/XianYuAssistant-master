# 多阶段构建 Dockerfile
# 阶段1: 构建前端
FROM node:20-alpine AS frontend-builder

WORKDIR /app/vue-code

# 复制前端项目文件
COPY vue-code/package*.json ./
RUN npm install

COPY vue-code/ ./
RUN npm run build:spring

# 阶段2: 构建后端
FROM maven:3.9-eclipse-temurin-17 AS backend-builder

WORKDIR /app

# 复制 Maven 配置文件
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# 复制后端源代码
COPY src ./src

# 从前端构建阶段复制静态文件
COPY --from=frontend-builder /app/src/main/resources/static ./src/main/resources/static

# 构建后端
RUN mvn clean package -DskipTests

# 阶段3: 运行时镜像
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 创建数据目录
RUN mkdir -p /app/data

# 从构建阶段复制 JAR 文件
COPY --from=backend-builder /app/target/*.jar app.jar

# 暴露端口
EXPOSE 12400

# 设置环境变量
ENV JAVA_OPTS="-Xms256m -Xmx512m" \
    TZ=Asia/Shanghai

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:12400/api/health || exit 1

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
