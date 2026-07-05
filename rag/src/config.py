import os
from pathlib import Path

# 项目根目录
BASE_DIR = Path(__file__).resolve().parent.parent

# 服务端口
SERVER_PORT = int(os.getenv("RAG_SERVER_PORT", 5001))

# Spring Boot 后端地址
SPRING_BOOT_URL = os.getenv("SPRING_BOOT_URL", "http://localhost:8080")

# RAG API认证密钥 (生产环境必须设置)
RAG_API_KEY = os.getenv("RAG_API_KEY", "")

# Milvus
MILVUS_HOST = os.getenv("MILVUS_HOST", "localhost")
MILVUS_PORT = int(os.getenv("MILVUS_PORT", "19530"))

# MySQL (敏感信息必须通过环境变量设置)
MYSQL_HOST = os.getenv("MYSQL_HOST", "localhost")
MYSQL_PORT = int(os.getenv("MYSQL_PORT", "3306"))
MYSQL_USER = os.getenv("MYSQL_USER", "root")
MYSQL_PASSWORD = os.getenv("MYSQL_PASSWORD", "")  # 生产环境必须设置
MYSQL_DB = os.getenv("MYSQL_DB", "virtualwife_admin")

# Redis
REDIS_HOST = os.getenv("REDIS_HOST", "localhost")
REDIS_PORT = int(os.getenv("REDIS_PORT", "6363"))
REDIS_DB = int(os.getenv("REDIS_DB", "5"))
REDIS_URL = os.getenv("REDIS_URL", f"redis://{REDIS_HOST}:{REDIS_PORT}/{REDIS_DB}")

# MinIO (敏感信息必须通过环境变量设置)
MINIO_ENDPOINT = os.getenv("MINIO_ENDPOINT", "localhost:9000")
MINIO_ACCESS_KEY = os.getenv("MINIO_ACCESS_KEY", "minioadmin")
MINIO_SECRET_KEY = os.getenv("MINIO_SECRET_KEY", "")  # 生产环境必须设置
MINIO_BUCKET = os.getenv("MINIO_BUCKET", "virtual-knowledge")

# 嵌入模型（通过环境变量指定模型路径）
EMBEDDING_MODEL = os.getenv("BGE_MODEL_PATH", os.getenv("EMBEDDING_MODEL", "BAAI/bge-large-zh-v1.5"))
EMBEDDING_DIM = int(os.getenv("EMBEDDING_DIM", "1024"))
EMBEDDING_BATCH_SIZE = int(os.getenv("EMBEDDING_BATCH_SIZE", "32"))

# 重排序模型
RERANK_MODEL = os.getenv("RERANK_MODEL", "BAAI/bge-reranker-v2-m3")

# 向量库路径（默认使用项目data目录）
VECTOR_DB_PATH = os.getenv("VECTOR_DB_PATH", str(BASE_DIR / "data" / "vectors.db"))

# 检索配置
VECTOR_TOP_K = 20
BM25_TOP_K = 15
RRF_K = 60
RERANK_TOP_K = 5
MIN_SCORE_THRESHOLD = 0.55
MAX_CONTEXT_TOKENS = 4096

# Agentic 配置
AGENTIC_ENABLED = os.getenv("AGENTIC_ENABLED", "true").lower() == "true"
AGENTIC_MAX_ITERATIONS = 5
AGENTIC_QUALITY_THRESHOLD = 0.8
AGENTIC_TIMEOUT_SECONDS = 10

# 切割配置
CHUNK_MIN_TOKENS = 100
CHUNK_MAX_TOKENS = 2048
CHUNK_OVERLAP_TOKENS = 50

# 上传目录
UPLOAD_DIR = BASE_DIR / "uploads"
UPLOAD_DIR.mkdir(exist_ok=True)

# LLM配置 (敏感信息必须通过环境变量设置)
LLM_API_URL = os.getenv("LLM_API_URL", "https://api.openai.com/v1")
LLM_API_KEY = os.getenv("LLM_API_KEY", "")  # 生产环境必须设置
LLM_MODEL = os.getenv("LLM_MODEL", "mimo-v2.5-pro")
