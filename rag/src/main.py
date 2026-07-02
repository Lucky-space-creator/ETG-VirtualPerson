from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from contextlib import asynccontextmanager
from pathlib import Path
import uvicorn

from src.config import SERVER_PORT
from src.api import health, document, search, tourist_analysis

@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期：启动时加载模型，关闭时释放资源"""
    print("[RAG] Starting RAG Embedding Service...")
    yield
    print("[RAG] Shutting down...")

app = FastAPI(
    title="Agentic RAG Embedding Service",
    description="AI数字人导游 - Agentic RAG检索子系统",
    version="1.0.0",
    lifespan=lifespan,
    # 大文件处理配置
    request_read_timeout=120,
    max_request_body_size=100 * 1024 * 1024,  # 100MB
)

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# 注册路由
app.include_router(health.router, prefix="/api/rag", tags=["health"])
app.include_router(document.router, prefix="/api/rag", tags=["document"])
app.include_router(search.router, prefix="/api/rag", tags=["search"])
app.include_router(tourist_analysis.router, prefix="/api/rag/tourist", tags=["tourist-analysis"])

# 挂载静态文件（独立前端UI）
static_dir = Path(__file__).parent.parent / "static"
static_dir.mkdir(exist_ok=True)
app.mount("/static", StaticFiles(directory=str(static_dir), html=True), name="static")

# 根路径重定向到向量库管理页面
from fastapi.responses import RedirectResponse
@app.get("/")
async def root():
    return RedirectResponse(url="/static/index.html")

if __name__ == "__main__":
    uvicorn.run("src.main:app", host="0.0.0.0", port=SERVER_PORT, reload=True)
