# AI 旅行规划师 — 运行说明

本项目用于课程评阅。后端端口固定为 `8080`，前端默认开发端口为 `5173`。

## 环境要求
- Node.js ≥ 18
- Java ≥ 17（JDK）
- Maven ≥ 3.8（或使用项目内 `mvnw`/`mvnw.cmd`）
- MySQL ≥ 8

## 数据库准备
- 在本机 MySQL 中创建数据库：`ai_travel_planner_db`
- 后端数据源配置文件：`backend/src/main/resources/application.yml`
  - 已写入必要的配置与密钥，老师无需修改密钥相关项。
  - 如本机 MySQL 账号/密码与仓库配置不一致，调整：`spring.datasource.url`、`spring.datasource.username`、`spring.datasource.password`。

## 启动后端
- 进入 `backend` 目录
- 执行：
  - `mvn -DskipTests spring-boot:run`
  - 或（Windows）`mvnw.cmd spring-boot:run`
  - 或使用IDEA通过(backend下的pom文件)导入backend项目，直接运行src/main/java/com/puxinheihei/backend/BackendApplication.java

## 前端启动
- 进入 `frontend` 目录
- 安装依赖：`npm ci`（推荐）或 `npm install`
- 启动开发服务器：`npm run dev`
- 浏览器访问终端提示地址（通常为 `http://localhost:5173/`）
