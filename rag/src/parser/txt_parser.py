import re


class TxtParser:
    """纯文本 / Markdown / HTML 解析器"""

    def parse(self, content: bytes, filename: str) -> dict:
        try:
            # 尝试多种编码
            text = None
            for enc in ["utf-8", "gbk", "gb2312", "latin-1"]:
                try:
                    text = content.decode(enc)
                    break
                except UnicodeDecodeError:
                    continue

            if text is None:
                raise RuntimeError("无法解码文件，请检查编码")

            # HTML 标签清洗
            ext = filename.rsplit(".", 1)[-1].lower() if "." in filename else ""
            if ext == "html":
                text = self._clean_html(text)
            elif ext == "md":
                text = self._clean_markdown(text)

            # 清洗控制字符和多余空行
            text = re.sub(r'[\x00-\x08\x0b\x0c\x0e-\x1f\x7f]', '', text)
            text = re.sub(r'\n{3,}', '\n\n', text)
            text = text.strip()

        except Exception as e:
            raise RuntimeError(f"文本解析失败: {str(e)}")

        return {
            "text": text,
            "page_count": 1,
            "char_count": len(text),
        }

    @staticmethod
    def _clean_html(text: str) -> str:
        """移除HTML标签，保留文本"""
        from html.parser import HTMLParser

        class TextExtractor(HTMLParser):
            def __init__(self):
                super().__init__()
                self.parts = []

            def handle_data(self, data):
                self.parts.append(data)

        extractor = TextExtractor()
        extractor.feed(text)
        return "\n".join(extractor.parts)

    @staticmethod
    def _clean_markdown(text: str) -> str:
        """简单清洗Markdown格式"""
        text = re.sub(r'#{1,6}\s+', '', text)       # 标题
        text = re.sub(r'\*\*(.+?)\*\*', r'\1', text) # 粗体
        text = re.sub(r'\*(.+?)\*', r'\1', text)     # 斜体
        text = re.sub(r'`{1,3}[^`]*`{1,3}', '', text) # 代码
        text = re.sub(r'\[(.+?)\]\(.+?\)', r'\1', text) # 链接
        text = re.sub(r'!\[.*?\]\(.+?\)', '', text)    # 图片
        return text
