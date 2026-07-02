"""
向量嵌入器
- 模式1 (sentence-transformers): 加载 BGE-Large-ZH 模型，语义向量
- 模式2 (n-gram fallback): 字符n-gram TF-IDF 向量，关键词匹配
"""
import numpy as np
from typing import List
import threading
import hashlib
from concurrent.futures import ThreadPoolExecutor

from src.config import EMBEDDING_MODEL, EMBEDDING_DIM

# 尝试加载真实模型
try:
    from sentence_transformers import SentenceTransformer
    _REAL_MODEL = SentenceTransformer(EMBEDDING_MODEL)
    _USE_REAL = True
except (ImportError, Exception):
    _REAL_MODEL = None
    _USE_REAL = False


class NGramEmbedder:
    """基于字符n-gram的TF-IDF向量嵌入器，多线程编码"""

    def __init__(self, dim: int = 1024, n_range=(1, 3), max_workers=4):
        self.dim = dim
        self.n_range = n_range
        self.max_workers = max_workers
        self._rng = np.random.RandomState(42)
        self._proj_matrix = self._rng.randn(200000, dim).astype(np.float32) / np.sqrt(dim)
        self._lock = threading.Lock()

    @staticmethod
    def _extract_ngrams(text: str, n_range=(1, 3)) -> List[str]:
        """提取字符n-gram（中英文通用）"""
        cleaned = text.replace(" ", "").replace("\n", "").replace("\r", "")
        if not cleaned:
            return []
        ngrams = []
        for n in range(n_range[0], n_range[1] + 1):
            for i in range(len(cleaned) - n + 1):
                ngrams.append(cleaned[i:i + n])
        return ngrams

    def _ngrams_to_vector(self, ngrams: List[str]) -> np.ndarray:
        """将n-gram列表转为1024维向量"""
        if not ngrams:
            return np.zeros(self.dim, dtype=np.float32)
        vec = np.zeros(self.dim, dtype=np.float32)
        for ng in ngrams:
            h = int(hashlib.md5(ng.encode("utf-8")).hexdigest(), 16) % len(self._proj_matrix)
            vec += self._proj_matrix[h]
        norm = np.linalg.norm(vec)
        if norm > 0:
            vec = vec / norm
        return vec.astype(np.float32)

    def _encode_single(self, text: str) -> np.ndarray:
        """单条文本编码"""
        return self._ngrams_to_vector(self._extract_ngrams(text, self.n_range))

    def encode(self, texts: List[str], batch_size: int = 32) -> np.ndarray:
        """批量编码（自动选择串行/并行）
        - 小批量(<16): 串行，避免线程开销
        - 大批量(>=16): 多线程并行"""
        if not texts:
            return np.zeros((0, self.dim), dtype=np.float32)

        n = len(texts)
        workers = min(self.max_workers, max(1, n // 8))

        if workers <= 1:
            # 串行：小批量
            vectors = []
            for i in range(0, n, batch_size):
                batch = texts[i:i + batch_size]
                vecs = np.array([self._encode_single(t) for t in batch], dtype=np.float32)
                vectors.append(vecs)
            return np.vstack(vectors)

        # 多线程并行：大批量
        return self._encode_parallel(texts, workers)

    def _encode_parallel(self, texts: List[str], workers: int) -> np.ndarray:
        """多线程并行编码"""
        with ThreadPoolExecutor(max_workers=workers) as executor:
            results = list(executor.map(self._encode_single, texts, chunksize=max(1, len(texts) // (workers * 4))))
        return np.array(results, dtype=np.float32)

    @property
    def dimension(self) -> int:
        return self.dim


# 全局单例
_embedder = None


def get_embedder():
    global _embedder
    if _embedder is None:
        if _USE_REAL and _REAL_MODEL is not None:
            _embedder = _REAL_MODEL
        else:
            _embedder = NGramEmbedder(dim=EMBEDDING_DIM, max_workers=4)
    return _embedder
