from .recursive_chunker import RecursiveChunker
from .fixed_chunker import FixedChunker
from .semantic_chunker import SemanticChunker


class ChunkerFactory:
    """文本切割器工厂 — 三种策略"""

    _chunkers = {
        "recursive": RecursiveChunker(),   # 按文档结构切割：标题层级分割
        "fixed": FixedChunker(),           # 固定大小：等距窗口
        "semantic": SemanticChunker(),      # 语义优先：句子/段落边界
    }

    @classmethod
    def get_chunker(cls, strategy: str):
        chunker = cls._chunkers.get(strategy)
        if not chunker:
            raise ValueError(f"不支持的切割策略: {strategy}，可选: recursive/fixed/semantic")
        return chunker
