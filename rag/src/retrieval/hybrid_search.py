import math
import re
from typing import List, Dict

from src.embedder.bge_embedder import get_embedder
from src.config import VECTOR_TOP_K, BM25_TOP_K, RRF_K


class BM25Simple:
    """简易 BM25 实现（不需要 jieba/rank-bm25 依赖）"""

    def __init__(self, corpus: List[str]):
        self.corpus = corpus
        self.doc_count = len(corpus)
        self.avg_dl = sum(len(self._tokenize(d)) for d in corpus) / max(self.doc_count, 1)
        self.k1 = 1.5
        self.b = 0.75

    @staticmethod
    def _tokenize(text: str) -> List[str]:
        """简单分词：字母数字+中文连续字符"""
        return re.findall(r'[\u4e00-\u9fff]+|[a-zA-Z0-9]+', text.lower())

    def get_scores(self, query_tokens: List[str]) -> List[float]:
        scores = []
        for doc in self.corpus:
            doc_tokens = self._tokenize(doc)
            dl = len(doc_tokens)
            score = 0.0
            for qt in query_tokens:
                tf = doc_tokens.count(qt)
                df = sum(1 for d in self.corpus if qt in self._tokenize(d)) or 1
                idf = math.log((self.doc_count - df + 0.5) / (df + 0.5) + 1)
                score += idf * (tf * (self.k1 + 1)) / (tf + self.k1 * (1 - self.b + self.b * dl / self.avg_dl))
            scores.append(score)
        return scores


class HybridSearcher:
    """混合检索引擎：向量语义检索 + BM25 关键词检索 → RRF 融合"""

    def __init__(self):
        self._embedder = get_embedder()
        self._corpus: List[str] = []       # 索引语料
        self._bm25: BM25Simple = None      # BM25 索引
        self._doc_vecs = None               # 缓存：文档向量（避免重复BGE编码）

    def index(self, documents: List[str]):
        """构建 BM25 索引 + 预计算所有文档BGE向量"""
        self._corpus = documents
        self._bm25 = BM25Simple(documents)
        if documents:
            import numpy as np
            self._doc_vecs = np.array(self._embedder.encode(documents), dtype=np.float32)

    def vector_search(self, query: str, kb_id: int, top_k: int) -> List[Dict]:
        """纯向量语义检索（使用缓存文档向量）"""
        if not self._corpus:
            return []

        query_vec = self._embedder.encode([query])[0]
        doc_vecs = self._doc_vecs if self._doc_vecs is not None else self._embedder.encode(self._corpus)
        scores = self._cosine_similarity(query_vec, doc_vecs)

        # 排序取 Top-K
        indexed = sorted(enumerate(scores), key=lambda x: x[1], reverse=True)
        results = []
        for idx, score in indexed[:top_k]:
            # 最低相似度阈值（基于文本哈希的确定性向量，阈值适当降低）
            if score >= 0.01:
                results.append({
                    "chunk_id": idx,
                    "content": self._corpus[idx][:500],
                    "score": round(float(score), 4),
                    "source": "vector",
                })
        return results

    def hybrid_search(self, query: str, kb_id: int, top_k: int) -> List[Dict]:
        """混合检索：向量 + BM25 → RRF 融合"""
        if not self._corpus:
            return []

        # 1. 向量检索（使用缓存文档向量）
        query_vec = self._embedder.encode([query])[0]
        doc_vecs = self._doc_vecs if self._doc_vecs is not None else self._embedder.encode(self._corpus)
        vec_scores = self._cosine_similarity(query_vec, doc_vecs)
        vec_ranked = sorted(enumerate(vec_scores), key=lambda x: x[1], reverse=True)
        vec_ranks = {idx: rank + 1 for rank, (idx, _) in enumerate(vec_ranked)}

        # 2. BM25 关键词检索
        if self._bm25:
            query_tokens = self._bm25._tokenize(query)
            bm25_scores = self._bm25.get_scores(query_tokens)
            bm25_ranked = sorted(enumerate(bm25_scores), key=lambda x: x[1], reverse=True)
            bm25_ranks = {idx: rank + 1 for rank, (idx, _) in enumerate(bm25_ranked)}
        else:
            bm25_ranks = {}

        # 3. RRF 融合
        all_indices = set(list(vec_ranks.keys()) + list(bm25_ranks.keys()))
        rrf_scores = {}
        for idx in all_indices:
            rrf = 0
            if idx in vec_ranks:
                rrf += 1.0 / (RRF_K + vec_ranks[idx])
            if idx in bm25_ranks:
                rrf += 1.0 / (RRF_K + bm25_ranks[idx])
            rrf_scores[idx] = rrf

        # 4. 排序取 Top-K
        final = sorted(rrf_scores.items(), key=lambda x: x[1], reverse=True)[:top_k]
        results = []
        for idx, score in final:
            results.append({
                "chunk_id": idx,
                "content": self._corpus[idx][:500],
                "score": round(float(score), 6),
                "source": "hybrid(rrf)",
            })
        return results

    @staticmethod
    def _cosine_similarity(query_vec, doc_vecs) -> List[float]:
        """计算余弦相似度"""
        import numpy as np
        q = query_vec / (np.linalg.norm(query_vec) + 1e-8)
        d = doc_vecs / (np.linalg.norm(doc_vecs, axis=1, keepdims=True) + 1e-8)
        return (d @ q).tolist()
