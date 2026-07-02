from .pdf_parser import PdfParser
from .docx_parser import DocxParser
from .txt_parser import TxtParser


class ParserFactory:
    """文档解析器工厂"""

    _parsers = {
        "pdf": PdfParser(),
        "docx": DocxParser(),
        "txt": TxtParser(),
        "md": TxtParser(),   # Markdown 用纯文本解析器
        "html": TxtParser(),  # HTML 用纯文本解析器
    }

    @classmethod
    def get_parser(cls, ext: str):
        parser = cls._parsers.get(ext)
        if not parser:
            raise ValueError(f"不支持的文档格式: {ext}")
        return parser
