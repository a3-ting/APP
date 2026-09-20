# APP - 校园课程管理应用

基于 Android 开发的校园课程管理与学习任务跟踪应用，集成 AI 学习规划推荐功能。同时包含配套的新闻速览演示页面。

## 项目结构

```
APP/
├── app/                    # Android 应用主模块
│   ├── src/main/java/com/example/myapplication/
│   │   ├── ui/             # 各功能模块界面
│   │   │   ├── main/       # 主界面（课程周视图、快捷入口）
│   │   │   ├── course/     # 课程管理（添加、周/月/周历视图）
│   │   │   ├── grade/      # 成绩管理与分析
│   │   │   ├── assignment/ # 作业管理
│   │   │   ├── study/      # 学习计划与进度
│   │   │   ├── ai/         # AI 聊天助手（接入 SiliconFlow API）
│   │   │   ├── auth/       # 登录/注册
│   │   │   └── user/       # 用户管理
│   │   ├── utils/          # 工具类（成绩分析、AI 推荐等）
│   │   ├── database/       # SQLite 数据库操作
│   │   └── service/        # 后台服务（提醒通知等）
│   └── src/main/res/       # 资源文件（布局、样式、图标）
├── index.html              # 热点新闻速览演示页面（GitHub Pages 部署）
└── README.md
```

## 主要功能

### Android 应用
- **课程管理**：支持周视图、月视图、网格视图查看课程表
- **成绩管理**：录入成绩，自动生成成绩分析与可视化
- **作业追踪**：记录作业截止日期，分类管理
- **学习计划**：基于 AI 的智能学习规划推荐
- **AI 聊天助手**：接入 SiliconFlow API，支持多轮对话
- **用户系统**：登录/注册，支持教师与学生双角色
- **数据备份**：支持本地备份与恢复

### 新闻速览页面
- 配套 Web 演示页面，展示全国及湖南省热点新闻
- 纯静态页面，无后端依赖，支持 GitHub Pages 部署

## 技术栈

- **语言**：Java
- **框架**：Android SDK, Material Design
- **AI 接口**：SiliconFlow API（GLM-4.7 / DeepSeek 等模型）
- **数据库**：SQLite（Room / 原生 DBHelper）
- **部署**：GitHub Pages（新闻页面）

## 构建与运行

```bash
# 使用 Android Studio 打开项目
open APP/

# 或通过命令行构建
./gradlew assembleDebug
```

## 关联仓库

- `campus-course-app`：本项目的增强版本，新增云同步与 Firebase 推送功能
- `xy`：本项目的扁平包结构版本（与 campus-course-app 核心代码一致）

## 更新记录

- 2026-09-18：初始版本，包含核心课程管理与 AI 学习规划功能
- 2026-09-20：补充新闻速览页面
