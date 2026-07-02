from typing import List, Dict


def deduplicate(documents: List[Dict], threshold: float = 0.95) -> List[Dict]:
    """内容去重：基于文本哈希去除高度相似的文档块"""
    if not documents or len(documents) <= 1:
        return documents

    seen_hashes = set()
    result = []
    for doc in documents:
        content = doc.get("content", "")
        # 简单哈希去重
        h = hash(content[:200])
        if h not in seen_hashes:
            seen_hashes.add(h)
            result.append(doc)

    return result


def window_expand(
    chunks: List[Dict],
    window_size: int = 2,
    all_chunks: List[Dict] = None,
) -> List[Dict]:
    """上下文窗口扩展：拉取相邻 chunk

    Args:
        chunks: 检索返回的 top-k chunks
        window_size: 左右各扩展N个chunk
        all_chunks: 全部chunks列表（按原始顺序）
    """
    if not all_chunks:
        return chunks

    expanded_indices = set()
    for chunk in chunks:
        idx = chunk.get("chunk_id", -1)
        if idx < 0:
            expanded_indices.add(idx)
            continue
        for offset in range(-window_size, window_size + 1):
            neighbor = idx + offset
            if 0 <= neighbor < len(all_chunks):
                expanded_indices.add(neighbor)

    result = []
    for idx in sorted(expanded_indices):
        if idx < len(all_chunks):
            result.append(all_chunks[idx])

    return result
