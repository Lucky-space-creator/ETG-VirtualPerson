import re
from typing import List


class RecursiveChunker:
    """按文档结构切割引擎（支持标题层级分割）"""

    # 标题模式：匹配 Markdown 标题（# ## ### 等）
    HEADING_PATTERN = re.compile(r'^(#{1,6})\s+.+$', re.MULTILINE)

    # 分隔符优先级
    SEPARATORS = [
        "\n\n\n", "\n\n", "\n",
        "。", "！", "？", "；",
        ". ", "! ", "? ", "; ",
        "，", ", ",
        " ", ""
    ]

    def chunk(self, text: str, chunk_size: int = 512, chunk_overlap: int = 50) -> List[str]:
        """执行按文档结构切割"""
        text = self._preprocess(text)

        # 检测是否为结构化文档（包含标题）
        if self._is_structured_doc(text):
            chunks = self._split_by_headings(text, chunk_size)
        else:
            chunks = self._split(text, chunk_size)

        chunks = self._post_process(chunks, chunk_size, chunk_overlap)
        return chunks

    @staticmethod
    def _preprocess(text: str) -> str:
        text = re.sub(r'[\x00-\x08\x0b\x0c\x0e-\x1f]', '', text)
        text = re.sub(r'\r\n', '\n', text)
        text = re.sub(r'\n{4,}', '\n\n\n', text)
        text = re.sub(r'[ \t]{3,}', '  ', text)
        return text.strip()

    def _is_structured_doc(self, text: str) -> bool:
        """检测是否为结构化文档（包含标题）"""
        headings = self.HEADING_PATTERN.findall(text)
        return len(headings) >= 2  # 至少有2个标题才认为是结构化文档

    def _split_by_headings(self, text: str, max_size: int) -> List[str]:
        """按标题层级分割文档"""
        result = []
        lines = text.split('\n')
        current_chunk = []
        current_size = 0

        for line in lines:
            # 检测是否为标题行
            is_heading = bool(re.match(r'^#{1,6}\s+', line))

            if is_heading and current_chunk:
                # 遇到新标题，保存当前块
                chunk_text = '\n'.join(current_chunk).strip()
                if chunk_text:
                    result.append(chunk_text)
                current_chunk = []
                current_size = 0

            current_chunk.append(line)
            current_size += len(line) + 1  # +1 for newline

            # 如果当前块超过最大大小，强制分割
            if current_size >= max_size:
                chunk_text = '\n'.join(current_chunk).strip()
                if chunk_text:
                    result.append(chunk_text)
                current_chunk = []
                current_size = 0

        # 处理最后一块
        if current_chunk:
            chunk_text = '\n'.join(current_chunk).strip()
            if chunk_text:
                result.append(chunk_text)

        # 对过大的块进行二次分割
        final_result = []
        for chunk in result:
            if len(chunk) > max_size * 2:
                sub_chunks = self._split(chunk, max_size)
                final_result.extend(sub_chunks)
            else:
                final_result.append(chunk)

        return final_result

    def _split(self, text: str, max_size: int) -> List[str]:
        """迭代式语义切割（栈 + 最大迭代保护）"""
        result = []
        stack = [text]
        max_iters = len(text) * 2  # 保护上限

        while stack and len(result) < max_iters:
            current = stack.pop()
            if not current:
                continue
            if len(current) <= max_size:
                result.append(current)
                continue

            # 尝试分隔符切割
            handled = False
            for sep in self.SEPARATORS:
                if not sep or sep not in current:
                    continue

                parts = current.split(sep)
                if len(parts) < 2:
                    continue

                # 倒序入栈保持顺序
                for i in range(len(parts) - 1, -1, -1):
                    part = parts[i]
                    if i < len(parts) - 1:
                        part = part + sep
                    if part.strip():
                        stack.append(part)
                handled = True
                break

            # 找不到分隔符 → 按最大大小强制切
            if not handled:
                n = (len(current) + max_size - 1) // max_size
                for i in range(n - 1, -1, -1):
                    chunk = current[i * max_size: (i + 1) * max_size]
                    if chunk.strip():
                        result.append(chunk)

        # 处理剩余栈中小块
        while stack:
            part = stack.pop()
            if part and part.strip() and len(part) <= max_size:
                result.append(part)

        result.reverse()
        return result

    def _post_process(self, chunks: List[str], target_size: int, overlap: int) -> List[str]:
        """合并过小块、拆分超大块"""
        if not chunks:
            return []

        result = []
        pending = ""
        min_size = max(50, target_size // 4)

        for chunk in chunks:
            chunk = chunk.strip()
            if not chunk:
                continue

            if len(chunk) < min_size:
                pending = pending + chunk
                continue

            if pending:
                chunk = pending + chunk
                pending = ""

            if len(chunk) > target_size * 2:
                # 超限块按固定大小切
                n = (len(chunk) + target_size - 1) // target_size
                for i in range(n):
                    sub = chunk[i * target_size: (i + 1) * target_size]
                    if sub.strip():
                        result.append(sub)
            else:
                result.append(chunk)

        if pending and result:
            result[-1] = result[-1] + pending
        elif pending:
            result.append(pending)

        return result
