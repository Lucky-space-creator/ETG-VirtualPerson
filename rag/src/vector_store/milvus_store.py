"""
向量存储 — SQLite 持久化（重启不丢失）
"""
import numpy as np
import sqlite3
import json
import os
import threading
from typing import List, Dict
from pathlib import Path

# 全局单例
_store = None

# 持久化文件路径（磁盘存储，环境变量 VECTOR_DB_PATH 可覆盖）
from src.config import VECTOR_DB_PATH
DB_PATH = Path(VECTOR_DB_PATH)


def get_milvus_store():
    global _store
    if _store is None:
        _store = MilvusStore()
    return _store


class MilvusStore:
    """向量存储 — SQLite持久化 + 内存LRU缓存"""

    def __init__(self):
        self._lock = threading.Lock()
        DB_PATH.parent.mkdir(exist_ok=True)

        self._conn = sqlite3.connect(str(DB_PATH), check_same_thread=False)
        self._conn.execute("PRAGMA journal_mode=WAL;")
        self._conn.execute("PRAGMA synchronous=NORMAL;")
        self._conn.execute("""
            CREATE TABLE IF NOT EXISTS vectors (
                kb_id INTEGER NOT NULL,
                chunk_id INTEGER NOT NULL,
                content TEXT,
                vector BLOB,
                PRIMARY KEY (kb_id, chunk_id)
            );
        """)
        self._conn.commit()

        # 内存缓存：{kb_id: [(chunk_id, content, vector_np), ...]}
        self._cache: Dict[int, List[tuple]] = {}
        self._load_cache()
        print(f"[VectorDB] SQLite持久化就绪: {DB_PATH} ({self.total_count()} 条向量)")

    def _load_cache(self):
        """启动时从SQLite加载全部向量到内存"""
        rows = self._conn.execute("SELECT kb_id, chunk_id, content, vector FROM vectors").fetchall()
        for kb_id, chunk_id, content, vec_bytes in rows:
            vec = np.frombuffer(vec_bytes, dtype=np.float32).copy()
            self._cache.setdefault(kb_id, []).append((chunk_id, content, vec))

    def total_count(self) -> int:
        return sum(len(v) for v in self._cache.values())

    # ==================== 写操作 ====================

    def create_collection(self, kb_id: int):
        self._cache.setdefault(kb_id, [])

    def insert(self, kb_id: int, chunks: List[Dict]):
        """批量插入向量"""
        with self._lock:
            self.create_collection(kb_id)
            for c in chunks:
                cid = c.get("chunk_id", 0)
                content = c.get("content", "")[:1000]
                vec = c.get("vector")
                if isinstance(vec, list):
                    vec = np.array(vec, dtype=np.float32)
                elif isinstance(vec, np.ndarray):
                    vec = vec.astype(np.float32)
                else:
                    vec = np.zeros(1024, dtype=np.float32)

                vec_bytes = vec.tobytes()
                self._conn.execute(
                    "INSERT OR REPLACE INTO vectors(kb_id, chunk_id, content, vector) VALUES(?,?,?,?)",
                    (kb_id, cid, content, vec_bytes)
                )
                # 更新缓存
                item = (cid, content, vec)
                existing = self._cache.get(kb_id, [])
                existing[:] = [x for x in existing if x[0] != cid]
                existing.append(item)
            self._conn.commit()

    def update_chunk(self, kb_id: int, chunk_id: int, content: str):
        """更新chunk内容+重新向量化"""
        try:
            from src.embedder.bge_embedder import get_embedder
            embedder = get_embedder()
            vec = embedder.encode([content])[0].astype(np.float32)
        except Exception:
            vec = np.zeros(1024, dtype=np.float32)

        with self._lock:
            self._conn.execute(
                "UPDATE vectors SET content=?, vector=? WHERE kb_id=? AND chunk_id=?",
                (content[:1000], vec.tobytes(), kb_id, chunk_id)
            )
            self._conn.commit()
            # 更新缓存
            items = self._cache.get(kb_id, [])
            items[:] = [(cid, content[:1000] if cid == chunk_id else c, 
                        vec if cid == chunk_id else v) for cid, c, v in items]

    def delete_chunk(self, kb_id: int, chunk_id: int):
        with self._lock:
            self._conn.execute("DELETE FROM vectors WHERE kb_id=? AND chunk_id=?", (kb_id, chunk_id))
            self._conn.commit()
            items = self._cache.get(kb_id, [])
            items[:] = [x for x in items if x[0] != chunk_id]

    def drop_collection(self, kb_id: int):
        with self._lock:
            self._conn.execute("DELETE FROM vectors WHERE kb_id=?", (kb_id,))
            self._conn.commit()
            self._cache.pop(kb_id, None)

    def clear_kb(self, kb_id: int):
        """清理指定KB的所有向量（重新嵌入前调用）"""
        self.drop_collection(kb_id)
        print(f"[VectorDB] 已清理 kb_id={kb_id}")

    # ==================== 读操作 ====================

    def search(self, kb_id: int, query_vector: np.ndarray, top_k: int = 5) -> List[Dict]:
        """余弦相似度检索（内存+SQLite）"""
        items = self._cache.get(kb_id, [])
        if not items:
            return []

        qv = np.array(query_vector, dtype=np.float32).flatten()
        scores = []
        for chunk_id, content, vec in items:
            sim = float(np.dot(qv, vec))
            scores.append((chunk_id, content, sim))

        scores.sort(key=lambda x: x[2], reverse=True)
        seen = set()
        result = []
        for chunk_id, content, score in scores:
            if chunk_id in seen:
                continue
            seen.add(chunk_id)
            result.append({
                "chunk_id": chunk_id,
                "content": (content or "")[:500],
                "score": round(score, 4),
            })
            if len(result) >= top_k:
                break
        return result

    def count(self, kb_id: int) -> int:
        return len(self._cache.get(kb_id, []))

    def get_chunks(self, kb_id: int) -> List[Dict]:
        """获取KB所有chunk"""
        return [{"chunk_id": cid, "content": (c or "")[:500]} for cid, c, _ in self._cache.get(kb_id, [])]
