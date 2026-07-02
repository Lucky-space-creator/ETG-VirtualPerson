"""
固定大小切割器 — 按字符数等距切片，不关心语义边界
适合：结构化数据、表格数据、无自然段落的文本
"""
from typing import List


class FixedChunker:
    """固定窗口切割，忽略语义"""

    def chunk(self, text: str, chunk_size: int = 512, chunk_overlap: int = 50) -> List[str]:
        text = text.strip()
        if not text:
            return []

        chunks = []
        start = 0
        step = max(chunk_size - chunk_overlap, 1)

        while start < len(text):
            end = start + chunk_size
            chunk = text[start:end].strip()
            if chunk:
                chunks.append(chunk)
            start += step
            if start >= len(text):
                break

        return chunks
