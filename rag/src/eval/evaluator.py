from typing import List, Dict, Tuple


class RAGEvaluator:
    """RAG 评测器：计算 Recall、MRR、NDCG 等指标"""

    @staticmethod
    def recall_at_k(retrieved_ids: List[int], relevant_ids: List[int], k: int) -> float:
        """Recall@K：相关文档中有多少被检索到"""
        if not relevant_ids:
            return 0.0
        top_k = set(retrieved_ids[:k])
        relevant = set(relevant_ids)
        hits = len(top_k & relevant)
        return hits / len(relevant)

    @staticmethod
    def mrr(retrieved_ids: List[int], relevant_ids: List[int]) -> float:
        """MRR (Mean Reciprocal Rank)：第一个相关文档的排名倒数"""
        for rank, rid in enumerate(retrieved_ids, start=1):
            if rid in relevant_ids:
                return 1.0 / rank
        return 0.0

    @staticmethod
    def ndcg_at_k(retrieved_ids: List[int], relevant_ids: List[int], k: int) -> float:
        """NDCG@K：归一化折损累积增益"""
        import math

        dcg = 0.0
        for i, rid in enumerate(retrieved_ids[:k]):
            relevance = 1.0 if rid in relevant_ids else 0.0
            dcg += relevance / math.log2(i + 2)

        idcg = 0.0
        ideal_hits = min(len(relevant_ids), k)
        for i in range(ideal_hits):
            idcg += 1.0 / math.log2(i + 2)

        return dcg / idcg if idcg > 0 else 0.0

    def evaluate(
        self,
        questions: List[Dict],
        retriever_fn,
    ) -> Dict:
        """批量评测

        Args:
            questions: [{"question": str, "relevant_ids": List[int]}, ...]
            retriever_fn: 检索函数(question, top_k) -> List[dict]

        Returns:
            {"recall_at_5": ..., "recall_at_10": ..., "mrr": ..., "ndcg_at_5": ...}
        """
        recall_5_scores = []
        recall_10_scores = []
        mrr_scores = []
        ndcg_5_scores = []

        for q in questions:
            results = retriever_fn(q["question"], top_k=10)
            retrieved_ids = [r.get("chunk_id", -1) for r in results]
            relevant = q.get("relevant_ids", [])

            recall_5_scores.append(self.recall_at_k(retrieved_ids, relevant, 5))
            recall_10_scores.append(self.recall_at_k(retrieved_ids, relevant, 10))
            mrr_scores.append(self.mrr(retrieved_ids, relevant))
            ndcg_5_scores.append(self.ndcg_at_k(retrieved_ids, relevant, 5))

        return {
            "recall_at_5": round(sum(recall_5_scores) / len(recall_5_scores), 4),
            "recall_at_10": round(sum(recall_10_scores) / len(recall_10_scores), 4),
            "mrr": round(sum(mrr_scores) / len(mrr_scores), 4),
            "ndcg_at_5": round(sum(ndcg_5_scores) / len(ndcg_5_scores), 4),
        }
