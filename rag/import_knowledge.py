"""
导入景区知识数据到RAG系统
"""
import requests
import os
import sys
import json
from pathlib import Path

# 设置stdout编码为UTF-8
sys.stdout.reconfigure(encoding='utf-8')

# RAG API地址
RAG_API = "http://localhost:5001/api/rag"

# 知识库目录
KNOWLEDGE_DIR = Path(__file__).parent.parent / "knowledge_base" / "示范景区公开资料包"


def parse_document(file_path: str) -> dict:
    """解析文档"""
    with open(file_path, 'rb') as f:
        files = {'file': (os.path.basename(file_path), f)}
        resp = requests.post(f"{RAG_API}/parse", files=files)
        return resp.json()


def index_chunks(scenic_spot: str, chunks: list):
    """索引文档块"""
    resp = requests.post(f"{RAG_API}/search/index", json={
        "kb_id": "1",
        "scenic_spot": scenic_spot,
        "chunks": chunks
    })
    return resp.json()


def split_text(text: str, chunk_size: int = 500, overlap: int = 50) -> list:
    """将文本分割成块"""
    chunks = []
    # 按段落分割
    paragraphs = text.split('\n\n')
    current_chunk = ""

    for para in paragraphs:
        para = para.strip()
        if not para:
            continue

        if len(current_chunk) + len(para) <= chunk_size:
            current_chunk += "\n\n" + para if current_chunk else para
        else:
            if current_chunk:
                chunks.append(current_chunk.strip())
            current_chunk = para

    if current_chunk:
        chunks.append(current_chunk.strip())

    return chunks


def import_document(file_path: str, scenic_spot: str):
    """导入单个文档"""
    print(f"\n{'='*50}")
    print(f"导入文档: {os.path.basename(file_path)}")
    print(f"景区标识: {scenic_spot}")
    print(f"{'='*50}")

    # 1. 解析文档
    print("1. 解析文档...")
    result = parse_document(file_path)

    if 'text' not in result:
        print(f"   解析失败: {result}")
        return

    text = result['text']
    print(f"   文本长度: {len(text)} 字符")

    # 2. 分割文本
    print("2. 分割文本...")
    chunks = split_text(text)
    print(f"   分割为 {len(chunks)} 个块")

    # 3. 索引到RAG
    print("3. 索引到RAG系统...")
    result = index_chunks(scenic_spot, chunks)
    print(f"   索引结果: {result}")

    print(f"[OK] 导入完成: {os.path.basename(file_path)}")


def main():
    print("=" * 60)
    print("景区知识数据导入工具")
    print("=" * 60)

    # 检查RAG服务是否可用
    try:
        resp = requests.get(f"{RAG_API}/health", timeout=5)
        print(f"RAG服务状态: {resp.json()['status']}")
    except Exception as e:
        print(f"[ERROR] RAG服务不可用: {e}")
        print("请先启动RAG服务: cd rag && python -m uvicorn src.main:app --port 5001")
        return

    # 导入灵山胜境相关文档
    lingshan_files = [
        ("灵山胜境 景点结构化数据集.docx", "lingshan"),
        ("灵山胜境：历史、文化、景点特色与个性化游览指南.docx", "lingshan"),
    ]

    for filename, spot in lingshan_files:
        file_path = KNOWLEDGE_DIR / filename
        if file_path.exists():
            import_document(str(file_path), spot)
        else:
            print(f"[WARN] 文件不存在: {file_path}")

    # 测试检索
    print("\n" + "=" * 60)
    print("测试检索")
    print("=" * 60)

    test_queries = [
        "灵山大佛有多高？",
        "九龙灌浴是什么？",
        "灵山梵宫有什么特色？",
    ]

    for query in test_queries:
        print(f"\n查询: {query}")
        resp = requests.post(f"{RAG_API}/search/hybrid", json={
            "query": query,
            "scenic_spot": "lingshan",
            "top_k": 2
        })
        result = resp.json()
        if result.get('chunks'):
            for i, chunk in enumerate(result['chunks']):
                content = chunk if isinstance(chunk, str) else chunk.get('content', '')
                print(f"  结果{i+1}: {content[:100]}...")
        else:
            print("  无结果")


if __name__ == "__main__":
    main()
