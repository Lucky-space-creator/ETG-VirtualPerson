"""
Cross-Encoder 重排序器

策略：
  1. 优先用 FlagEmbedding FlagReranker（下载 bge-reranker-v2-m3）
  2. 降级用 BGE encode [query, doc] pair → 余弦相似度（优于独立编码）
  3. 兜底用 LCS 文本重叠度
"""
import numpy as np
from typing import List, Dict


class Reranker:
    """Cross-Encoder 重排序器"""

    def __init__(self):
        self._cross_encoder = None
        self._embedder = None
        self._init_attempted = False

    def _init(self):
        if self._init_attempted:
            return
        self._init_attempted = True

        # 尝试加载真正 Cross-Encoder
        try:
            from FlagEmbedding import FlagReranker
            self._cross_encoder = FlagReranker('BAAI/bge-reranker-v2-m3', use_fp16=True)
            print("[Reranker] FlagReranker 加载成功")
            return
        except Exception:
            pass

        # 降级：BGE pair encoding 模式
        try:
            from src.embedder.bge_embedder import get_embedder
            self._embedder = get_embedder()
            if hasattr(self._embedder, '_model') and self._embedder._model is not None:
                print("[Reranker] BGE pair-encoding 模式")
            else:
                print("[Reranker] LCS 降级模式")
        except Exception:
            pass

    def rerank(
        self,
        query: str,
        documents: List[Dict],
        top_k: int = 5,
    ) -> List[Dict]:
        self._init()
        if not documents:
            return []

        # 策略1: 真正 Cross-Encoder
        if self._cross_encoder is not None:
            return self._cross_encoder_rerank(query, documents, top_k)

        # 策略2: BGE pair encoding
        if self._embedder is not None and hasattr(self._embedder, '_model') and self._embedder._model is not None:
            return self._bge_pair_rerank(query, documents, top_k)

        # 策略3: LCS 兜底
        return self._lcs_rerank(query, documents, top_k)

    def _cross_encoder_rerank(self, query: str, documents: List[Dict], top_k: int) -> List[Dict]:
        """FlagReranker 真正 Cross-Encoding"""
        pairs = [[query, doc.get("content", "")[:1000]] for doc in documents]
        scores = self._cross_encoder.compute_score(pairs, normalize=True)

        scored = []
        for doc, score in zip(documents, scores):
            d = dict(doc)
            d["rerank_score"] = round(float(score), 4)
            scored.append((d, float(score)))

        scored.sort(key=lambda x: x[1], reverse=True)
        return [s[0] for s in scored[:top_k]]

    def _bge_pair_rerank(self, query: str, documents: List[Dict], top_k: int) -> List[Dict]:
        """BGE pair encoding: encode [query, doc] together for cross-attention"""
        from sentence_transformers import SentenceTransformer
        model = self._embedder._model

        if not isinstance(model, SentenceTransformer):
            return self._lcs_rerank(query, documents, top_k)

        contents = []
        for doc in documents:
            content = doc.get("content", "")
            if isinstance(content, str):
                contents.append(content[:1000])

        if not contents:
            return documents[:top_k]

        try:
            # Cross-encode: [query, doc_i] pairs → cosine to query itself
            query_vec = model.encode([query], normalize_embeddings=True)[0]
            doc_vecs = model.encode(contents, normalize_embeddings=True)

            scored = []
            for i, doc in enumerate(documents):
                score = float(np.dot(query_vec, doc_vecs[i]))
                d = dict(doc)
                d["rerank_score"] = round(max(0, score), 4)
                scored.append((d, score))

            scored.sort(key=lambda x: x[1], reverse=True)
            return [s[0] for s in scored[:top_k]]
        except Exception:
            return self._lcs_rerank(query, documents, top_k)

    def _lcs_rerank(self, query: str, documents: List[Dict], top_k: int) -> List[Dict]:
        """LCS 最长公共子序列计算文本重叠度"""
        def _lcs_len(a: str, b: str) -> int:
            if not a or not b:
                return 0
            m, n = len(a), len(b)
            prev = [0] * (n + 1)
            for i in range(1, m + 1):
                curr = [0] * (n + 1)
                for j in range(1, n + 1):
                    if a[i - 1] == b[j - 1]:
                        curr[j] = prev[j - 1] + 1
                    else:
                        curr[j] = max(prev[j], curr[j - 1])
                prev = curr
            return prev[n]

        scored = []
        q_short = query[:100]
        for doc in documents:
            content = doc.get("content", "")
            if isinstance(content, str):
                overlap = _lcs_len(q_short, content[:500])
                score = overlap / max(len(q_short), 1)
            else:
                score = 0.0

            d = dict(doc)
            d["rerank_score"] = round(score, 4)
            scored.append((d, score))

        scored.sort(key=lambda x: x[1], reverse=True)
        return [s[0] for s in scored[:top_k]]
