"""
语义优先切割器 — 优先在句子/段落边界切，保持语义完整性
适合：文章、论文、攻略、游记等自然语言文本

策略：
  1. 优先在段落边界（双换行）切
  2. 其次在句子边界（句号、问号、叹号）切
  3. 降级到递归切割
"""
import re
from typing import List


class SemanticChunker:
    """以语义完整性为优先的切割器"""

    def chunk(self, text: str, chunk_size: int = 512, chunk_overlap: int = 50) -> List[str]:
        text = text.strip()
        if not text:
            return []

        # 收集所有句子
        sentences = self._split_sentences(text)

        # 贪心合并句子，不超过 chunk_size
        chunks = []
        current = ""
        for sent in sentences:
            if len(current) + len(sent) <= chunk_size:
                current += sent
            else:
                if current.strip():
                    chunks.append(current.strip())
                # 单个句子超长 → 强制切
                if len(sent) > chunk_size:
                    # 尝试按逗号/分号切
                    sub_chunks = self._split_long_sentence(sent, chunk_size)
                    chunks.extend(sub_chunks[:-1])
                    current = sub_chunks[-1] if sub_chunks else ""
                else:
                    current = sent

        if current.strip():
            chunks.append(current.strip())

        # 重叠处理：相邻chunk尾部重叠
        if chunk_overlap > 0 and len(chunks) > 1:
            for i in range(len(chunks) - 1):
                overlap_text = chunks[i + 1][:chunk_overlap]
                if overlap_text:
                    chunks[i] = chunks[i] + overlap_text

        return chunks

    @staticmethod
    def _split_sentences(text: str) -> List[str]:
        """按句子边界切分，保留标点"""
        pattern = r'([^。！？\n]+[。！？]?\n*)'
        parts = re.findall(pattern, text)
        if not parts:
            return [text]
        return [p for p in parts if p.strip()]

    @staticmethod
    def _split_long_sentence(sentence: str, chunk_size: int) -> List[str]:
        """超长句子按子句（逗号/分号/冒号）切"""
        # 先按逗号、分号、冒号切
        pattern = r'([^，；：,;:]+[，；：,;:]?)'
        clauses = re.findall(pattern, sentence)
        if not clauses:
            # 兜底：强制按大小切
            return [sentence[i:i+chunk_size] for i in range(0, len(sentence), chunk_size - max(chunk_size//4, 10))]

        chunks = []
        current = ""
        for cl in clauses:
            if len(current) + len(cl) <= chunk_size:
                current += cl
            else:
                if current.strip():
                    chunks.append(current.strip())
                if len(cl) > chunk_size:
                    # 还超长 → 字符级强制切
                    for i in range(0, len(cl), chunk_size):
                        chunks.append(cl[i:i+chunk_size])
                else:
                    current = cl
        if current.strip():
            chunks.append(current.strip())
        return chunks
