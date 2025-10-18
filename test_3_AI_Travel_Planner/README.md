# AI 旅行规划师 — 运行说明

本项目用于课程作业评阅，文档仅保留运行步骤与必要配置。

## 环境要求
- Node.js ≥ 18
- Java ≥ 17（JDK）
- Maven ≥ 3.8（或使用项目内 `mvnw`/`mvnw.cmd`）
- MySQL ≥ 8（本地数据库）

## 数据库准备
- 创建数据库：`ai_travel_planner_db`
- 创建表，执行 `backend/sql/*.sql`
- 配置文件：`backend/src/main/resources/application.yml`
  - 设置 `spring.datasource.url`、`spring.datasource.username`、`spring.datasource.password`

## 启动后端
- 打开终端，进入 `backend` 目录
- 运行：
  - `mvn -DskipTests spring-boot:run`
  - 或（Windows）`mvnw.cmd spring-boot:run`
  - 或直接启动该Spring boot项目
- 后端启动成功后：`http://localhost:8080`

## 启动前端
- 进入 `frontend` 目录
- 安装依赖：`npm install`
- 启动开发服务器：`npm run dev`
- 在终端输出的地址（如 `http://localhost:5173/`）打开浏览器访问