import redis
import json
import time
import hashlib
from typing import Optional, Any

from src.config import REDIS_HOST, REDIS_PORT, REDIS_DB


class CacheManager:
    """Redis 热点缓存管理器（集成真实 Redis）

    缓存策略：
      - 热点问答缓存 TTL=1h
      - Agent 状态缓存 TTL=10min
      - 自动序列化/反序列化
    """

    def __init__(self, ttl_seconds: int = 3600):
        try:
            self._client = redis.Redis(
                host=REDIS_HOST, port=REDIS_PORT, db=REDIS_DB,
                decode_responses=True, protocol=2,  # RESP2 兼容老版本Redis
                socket_connect_timeout=3,
            )
            self._client.ping()
            self._use_redis = True
        except Exception:
            # Redis不可用时回退到本地缓存
            self._client = None
            self._use_redis = False
            self._local: dict = {}

        self._ttl = ttl_seconds

    def get(self, key: str) -> Optional[Any]:
        """获取缓存"""
        if self._use_redis:
            try:
                val = self._client.get(key)
                return json.loads(val) if val else None
            except Exception:
                return None
        else:
            item = self._local.get(key)
            if item and time.time() < item[1]:
                return item[0]
            return None

    def set(self, key: str, value: Any, ttl: int = None):
        """设置缓存"""
        ttl = ttl or self._ttl
        serialized = json.dumps(value, ensure_ascii=False)
        if self._use_redis:
            try:
                self._client.setex(key, ttl, serialized)
            except Exception:
                pass
        else:
            self._local[key] = (value, time.time() + ttl)
            # LRU 淘汰
            if len(self._local) > 1000:
                oldest = min(self._local.items(), key=lambda x: x[1][1])
                del self._local[oldest[0]]

    def invalidate(self, key: str):
        """删除缓存"""
        if self._use_redis:
            try:
                self._client.delete(key)
            except Exception:
                pass
        else:
            self._local.pop(key, None)

    def clear(self):
        """清空缓存"""
        if self._use_redis:
            try:
                self._client.flushdb()
            except Exception:
                pass
        else:
            self._local.clear()

    @staticmethod
    def make_key(query: str, kb_id: int) -> str:
        """生成缓存Key"""
        h = hashlib.md5(f"{kb_id}:{query}".encode()).hexdigest()[:12]
        return f"rag:kb:{kb_id}:query:{h}"

    @property
    def is_redis(self) -> bool:
        return self._use_redis
