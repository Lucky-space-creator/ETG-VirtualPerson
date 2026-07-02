import fitz  # pymupdf


class PdfParser:
    """PDF 文档解析器，使用 PyMuPDF"""

    def parse(self, content: bytes, filename: str) -> dict:
        text_parts = []
        try:
            doc = fitz.open(stream=content, filetype="pdf")
            page_count = len(doc)

            for page_num in range(page_count):
                page = doc[page_num]
                page_text = page.get_text("text", sort=True)
                if page_text.strip():
                    text_parts.append(f"[第{page_num + 1}页]\n{page_text.strip()}")

            doc.close()
        except Exception as e:
            raise RuntimeError(f"PDF解析失败: {str(e)}")

        full_text = "\n\n".join(text_parts)
        return {
            "text": full_text,
            "page_count": page_count if page_count > 0 else 1,
            "char_count": len(full_text),
        }
