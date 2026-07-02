"""内存文档存储：维护已索引文档，供检索使用"""
from typing import List, Dict
from collections import defaultdict


class DocStore:
    """线程安全的内存文档存储"""

    def __init__(self):
        self._docs: Dict[str, List[str]] = defaultdict(list)  # kb_id -> [chunk1, chunk2, ...]
        self._meta: Dict[str, List[dict]] = defaultdict(list)  # kb_id -> [meta1, meta2, ...]

    def add_chunks(self, kb_id: str, chunks: List[str], metas: List[dict] = None):
        self._docs[kb_id].extend(chunks)
        if metas:
            self._meta[kb_id].extend(metas)
        else:
            self._meta[kb_id].extend([{} for _ in chunks])

    def get_chunks(self, kb_id: str) -> List[str]:
        return self._docs.get(kb_id, [])

    def get_metas(self, kb_id: str) -> List[dict]:
        return self._meta.get(kb_id, [])

    def clear_kb(self, kb_id: str):
        self._docs.pop(kb_id, None)
        self._meta.pop(kb_id, None)

    def kb_count(self) -> int:
        return len(self._docs)

    def total_chunks(self) -> int:
        return sum(len(v) for v in self._docs.values())


# 全局单例
_doc_store = DocStore()


def get_doc_store() -> DocStore:
    return _doc_store
