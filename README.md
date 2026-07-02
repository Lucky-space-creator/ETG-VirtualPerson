# ETG-VirtualPerson — AI数字人导游智能导览系统

基于AI数字人技术的景区智能导览系统，为游客提供沉浸式、个性化的游览体验。

## 系统架构

```
┌─────────────────────────────────────────────────┐
│                 Android客户端                     │
│   Jetpack Compose + Three.js + three-vrm        │
│   VRM数字人渲染 · 语音交互 · 路线导航             │
└────────────────────┬────────────────────────────┘
                     │ HTTP/WebSocket
┌────────────────────┼────────────────────────────┐
│            Spring Boot 后端服务                    │
│   聊天API · RAG集成 · TTS语音 · RBAC权限          │
│   数字人管理 · 消费分析 · 数据大屏                  │
└────────┬───────────────────────────┬─────────────┘
         │                           │
   ┌─────┴─────┐              ┌──────┴──────┐
   │   MySQL   │              │    MinIO    │
   └───────────┘              └─────────────┘

┌─────────────────────────────────────────────────┐
│            RAG检索服务 (FastAPI)                   │
│   文档解析 · 向量嵌入 · 混合检索 · 重排序          │
└─────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────┐
│            Vue3 管理后台                           │
│   知识库管理 · 数字人配置 · 数据大屏 · 权限管理     │
└─────────────────────────────────────────────────┘
```

## 功能特性

### 游客端 (Android)
- **3D数字人** — Three.js + three-vrm 渲染，支持口型同步、表情控制、16种动作动画
- **智能问答** — 基于RAG的景区知识问答，回答准确率 > 90%
- **语音交互** — 语音输入识别 + TTS语音合成播放
- **路线导航** — GPS围栏触发景点自动讲解
- **个性化推荐** — 根据用户兴趣标签推荐路线和景点

### 管理后台 (Vue3)
- **知识库管理** — 支持PDF/DOCX/TXT/MD文档上传、自动切割、向量化
- **数字人管理** — VRM模型、衣服配置、声音选择
- **数据大屏** — 服务人次、热门问答、游客满意度、消费趋势
- **消费分析** — Excel导入消费数据，多维度分析游客画像
- **RBAC权限** — 角色权限管理，支持细粒度资源控制

### RAG检索 (FastAPI)
- **混合检索** — 向量检索 (BGE-large-zh) + BM25 关键词检索
- **RRF融合** — Reciprocal Rank Fusion 融合两路结果
- **重排序** — BGE-reranker 对结果精排
- **多景区支持** — 按景区隔离知识库

## 技术栈

| 层级 | 技术 | 说明 |
|------|------|------|
| Android | Kotlin + Jetpack Compose | 客户端开发 |
| 3D渲染 | Three.js + three-vrm | VRM数字人渲染 |
| 后端 | Spring Boot 3.2 + MyBatis Plus | API服务 |
| 前端 | Vue 3 + Element Plus | 管理后台 |
| RAG | FastAPI + BGE-large-zh | 知识检索 |
| 数据库 | MySQL 8.0 | 关系型存储 |
| 向量库 | Milvus / SQLite | 向量检索 |
| 存储 | MinIO | 文件存储 |
| 语音 | Edge-TTS | 语音合成 |
| LLM | GPT-4.1 / 通义千问 | 大语言模型 |

## 项目结构

```
ETG-VirtualPerson/
├── android-app/                    # Android客户端
│   └── app/src/main/
│       ├── assets/web/             # VRM渲染 (Three.js)
│       ├── java/.../ui/            # Compose界面
│       ├── java/.../viewmodel/     # ViewModel层
│       └── java/.../data/          # 数据层 (Retrofit + Room)
│
├── server/
│   ├── springboot-admin/           # Spring Boot后端
│   │   └── src/main/java/.../
│   │       ├── module/chat/        # 聊天模块
│   │       ├── module/avatar/      # 数字人管理
│   │       ├── module/knowledge/   # 知识库管理
│   │       ├── module/rag/         # RAG文档管理
│   │       ├── module/statistics/  # 数据统计
│   │       ├── module/tourist/     # 消费分析
│   │       ├── module/rbac/        # 权限管理
│   │       └── integration/        # LLM/RAG/TTS集成
│   │
│   ├── vue-admin/                  # Vue3管理后台
│   │   └── src/views/              # 页面组件
│   │
│   └── docker-compose.yml          # Docker部署
│
├── rag/                            # RAG检索服务
│   └── src/
│       ├── api/                    # FastAPI路由
│       ├── chunker/                # 文档切割
│       ├── embedder/               # 向量嵌入
│       ├── retrieval/              # 混合检索
│       └── vector_store/           # 向量存储
│
└── 设计文档.md                      # 产品设计文档
```

## 快速开始

### 环境要求
- JDK 17+
- Python 3.10+
- Node.js 18+
- MySQL 8.0
- Android Studio (客户端开发)

### 后端启动
```bash
# 1. 创建数据库
mysql -u root -p < server/springboot-admin/src/main/resources/db/schema.sql

# 2. 配置数据库连接
# 编辑 server/springboot-admin/src/main/resources/application-dev.yml

# 3. 启动后端
cd server/springboot-admin
mvn spring-boot:run
```

### RAG服务启动
```bash
cd rag
pip install -r requirements.txt
python src/main.py
```

### 管理后台启动
```bash
cd server/vue-admin
npm install
npm run dev
```

### Android客户端
用 Android Studio 打开 `android-app/` 目录，配置 `local.properties` 中的 SDK 路径后运行。

### Docker部署
```bash
cd server
docker-compose up -d
```

## 核心设计

### 数字人动画系统
- 16种预定义动作：点头、招手、鞠躬、思考、解释等
- 关键帧动画 + easeInOutCubic 缓动
- 多频率口型同步：200ms/400ms/800ms 混合
- 待机动画：呼吸浮动、眨眼、轻微摇摆

### RAG检索流程
```
用户查询 → 向量检索(BGE) + BM25检索 → RRF融合 → BGE重排序 → Top-K结果
```

### 消费分析维度
- 消费结构：门票、餐饮、购物、交通、娱乐
- 年龄分布：18-25 / 26-35 / 36-45 / 46-55 / 56+
- 满意度分布：非常满意 / 满意 / 一般 / 不满意
- 消费模式：高消费 / 购物主导 / 餐饮主导 / 家庭出游

## 许可证

本项目用于软件杯大赛参赛作品。
