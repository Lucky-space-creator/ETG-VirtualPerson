from fastapi import APIRouter, UploadFile, File, Form, HTTPException
from typing import Optional, List
from pydantic import BaseModel
import json

from src.parser.parser_factory import ParserFactory
from src.chunker.chunker_factory import ChunkerFactory
from src.embedder.bge_embedder import get_embedder
from src.config import EMBEDDING_DIM

router = APIRouter()

ALLOWED_EXTENSIONS = {"pdf", "docx", "txt", "md", "html"}


class ParseResponse(BaseModel):
    doc_name: str
    doc_type: str
    text: str
    page_count: int = 1
    char_count: int = 0


class ChunkPreviewRequest(BaseModel):
    text: str
    chunk_strategy: str = "recursive"
    chunk_size: int = 512
    chunk_overlap: int = 50


class ChunkPreviewResponse(BaseModel):
    chunks: list
    chunk_count: int
    strategy: str


class EmbeddingRequest(BaseModel):
    texts: List[str]
    model_name: Optional[str] = None


class EmbeddingResponse(BaseModel):
    vectors: List[List[float]]
    dimension: int
    count: int


@router.post("/parse", response_model=ParseResponse)
async def parse_document(file: UploadFile = File(...)):
    """解析文档，提取纯文本（支持 PDF/DOCX/TXT/MD/HTML）"""
    filename = file.filename or "unknown"
    ext = filename.rsplit(".", 1)[-1].lower() if "." in filename else ""

    if ext not in ALLOWED_EXTENSIONS:
        raise HTTPException(400, f"不支持的文件格式: {ext}，支持: {', '.join(ALLOWED_EXTENSIONS)}")

    content = await file.read()

    try:
        parser = ParserFactory.get_parser(ext)
        result = parser.parse(content, filename)
    except Exception as e:
        raise HTTPException(500, f"文档解析失败: {str(e)}")

    return ParseResponse(
        doc_name=filename,
        doc_type=ext,
        text=result.get("text", ""),
        page_count=result.get("page_count", 1),
        char_count=result.get("char_count", 0),
    )


@router.post("/chunk/preview", response_model=ChunkPreviewResponse)
async def preview_chunking(req: ChunkPreviewRequest):
    """切割预览（不存库），用于调参"""
    try:
        chunker = ChunkerFactory.get_chunker(req.chunk_strategy)
        chunks = chunker.chunk(
            req.text,
            chunk_size=req.chunk_size,
            chunk_overlap=req.chunk_overlap,
        )
    except Exception as e:
        raise HTTPException(500, f"切割预览失败: {str(e)}")

    return ChunkPreviewResponse(
        chunks=chunks,
        chunk_count=len(chunks),
        strategy=req.chunk_strategy,
    )


@router.post("/chunk")
async def chunk_document(
    text: str = Form(...),
    chunk_strategy: str = Form("recursive"),
    chunk_size: int = Form(512),
    chunk_overlap: int = Form(50),
):
    """执行五阶段切割"""
    try:
        chunker = ChunkerFactory.get_chunker(chunk_strategy)
        chunks = chunker.chunk(text, chunk_size=chunk_size, chunk_overlap=chunk_overlap)

        result = []
        for i, chunk_text in enumerate(chunks):
            result.append({
                "chunk_index": i,
                "content": chunk_text,
                "token_count": len(chunk_text),
            })

        return {"chunks": result, "chunk_count": len(result), "strategy": chunk_strategy}
    except Exception as e:
        raise HTTPException(500, f"文本切割失败: {str(e)}")


@router.post("/embedding/batch")
async def batch_embedding(req: EmbeddingRequest):
    """批量文本向量化"""
    try:
        embedder = get_embedder()
        vectors = embedder.encode(req.texts)
    except Exception as e:
        raise HTTPException(500, f"向量化失败: {str(e)}")

    return {
        "vectors": [v.tolist() for v in vectors],
        "dimension": len(vectors[0]) if vectors.size > 0 else 0,
        "count": len(vectors),
    }


@router.post("/embedding/single")
async def single_embedding(text: str = Form(...)):
    """单条文本向量化"""
    try:
        embedder = get_embedder()
        vector = embedder.encode([text])[0]
        return {"vector": vector.tolist(), "dimension": len(vector)}
    except Exception as e:
        raise HTTPException(500, f"向量化失败: {str(e)}")


class VectorStoreRequest(BaseModel):
    """向量存储请求"""
    kb_id: int
    chunks: List[dict]  # [{"chunk_id": 1, "content": "...", "vector": [...]}, ...]


@router.post("/embedding/store")
async def embed_and_store(req: VectorStoreRequest):
    """嵌入 + 存入 Milvus 向量数据库（一步完成）"""
    try:
        from src.vector_store.milvus_store import get_milvus_store

        # 1. 提取文本并向量化
        texts = [c.get("content", "") for c in req.chunks]
        embedder = get_embedder()
        vectors = embedder.encode(texts)

        # 2. 组装数据存入 Milvus（全局单例，持久化）
        store = get_milvus_store()
        store.create_collection(req.kb_id)

        insert_data = []
        for i, chunk in enumerate(req.chunks):
            insert_data.append({
                "chunk_id": chunk.get("chunk_id", i),
                "content": chunk.get("content", "")[:1000],
                "vector": vectors[i],
            })

        store.insert(req.kb_id, insert_data)
        total = store.count(req.kb_id)

        # 更新检索索引
        from src.api.search import _refresh_searcher
        _refresh_searcher()

        return {
            "status": "ok",
            "stored": len(insert_data),
            "total_vectors": total,
            "dimension": EMBEDDING_DIM,
        }
    except Exception as e:
        raise HTTPException(500, f"向量存储失败: {str(e)}")


@router.get("/embedding/count/{kb_id}")
async def vector_count(kb_id: int):
    """查询知识库中已存储的向量数量"""
    try:
        from src.vector_store.milvus_store import get_milvus_store
        store = get_milvus_store()
        return {"kb_id": kb_id, "total_vectors": store.count(kb_id)}
    except Exception as e:
        raise HTTPException(500, str(e))


# ==================== CRUD 操作 ====================

@router.get("/embedding/chunks/{kb_id}")
async def list_chunks(kb_id: int):
    """列出指定KB的所有chunk（含内容预览）"""
    try:
        from src.vector_store.milvus_store import get_milvus_store
        store = get_milvus_store()
        chunks = store.get_chunks(kb_id)
        return {"kb_id": kb_id, "chunks": chunks, "total": len(chunks)}
    except Exception as e:
        raise HTTPException(500, str(e))


class ChunkUpdateRequest(BaseModel):
    kb_id: int
    chunk_id: int
    content: str


@router.put("/embedding/chunk")
async def update_chunk(req: ChunkUpdateRequest):
    """更新chunk内容（重新向量化）"""
    try:
        from src.vector_store.milvus_store import get_milvus_store
        store = get_milvus_store()
        store.update_chunk(req.kb_id, req.chunk_id, req.content)
        return {"status": "ok", "kb_id": req.kb_id, "chunk_id": req.chunk_id}
    except Exception as e:
        raise HTTPException(500, str(e))


@router.delete("/embedding/chunk/{kb_id}/{chunk_id}")
async def delete_chunk(kb_id: int, chunk_id: int):
    """删除指定chunk"""
    try:
        from src.vector_store.milvus_store import get_milvus_store
        store = get_milvus_store()
        store.delete_chunk(kb_id, chunk_id)
        return {"status": "ok", "kb_id": kb_id, "chunk_id": chunk_id}
    except Exception as e:
        raise HTTPException(500, str(e))


@router.delete("/embedding/drop/{kb_id}")
async def drop_collection(kb_id: int):
    """删除整个向量库"""
    try:
        from src.vector_store.milvus_store import get_milvus_store
        store = get_milvus_store()
        store.drop_collection(kb_id)
        return {"status": "ok", "kb_id": kb_id}
    except Exception as e:
        raise HTTPException(500, str(e))


@router.delete("/embedding/clear/{kb_id}")
async def clear_kb_vectors(kb_id: int):
    """重新向量化前清理指定KB（Java RagApiClient.clearVectors 调用此端点）"""
    try:
        from src.vector_store.milvus_store import get_milvus_store
        store = get_milvus_store()
        store.clear_kb(kb_id)
        return {"status": "ok", "kb_id": kb_id, "message": f"KB {kb_id} 向量已清理"}
    except Exception as e:
        raise HTTPException(500, str(e))


@router.get("/embedding/collections")
async def list_collections():
    """列出所有向量库及其统计信息"""
    try:
        from src.vector_store.milvus_store import get_milvus_store
        store = get_milvus_store()

        # 扫描已知KB（0-100范围内）
        collections = []
        for kb_id in range(0, 100):
            count = store.count(kb_id)
            if count > 0:
                collections.append({
                    "kb_id": kb_id,
                    "name": f"知识库_{kb_id}",
                    "vectors": count,
                    "storage": f"D:\\Milvus\\vectordb (SQLite)",
                })

        return {
            "collections": sorted(collections, key=lambda x: x["kb_id"]),
            "total_collections": len(collections),
            "total_vectors": sum(c["vectors"] for c in collections),
            "mode": "SQLite (D:\\Milvus\\vectordb)",
        }
    except Exception as e:
        raise HTTPException(500, str(e))


# ==================== 知识图谱 ====================

class GraphBuildRequest(BaseModel):
    kb_id: int
    chunks: List[dict]  # [{"chunk_id": 0, "content": "..."}, ...]


class EntityExtractRequest(BaseModel):
    text: str
    chunk_id: int = 0


@router.post("/graph/extract")
async def extract_entities(req: EntityExtractRequest):
    """单chunk实体+关系抽取"""
    try:
        from src.knowledge_enhancer.relation_extractor import RelationExtractor
        extractor = RelationExtractor()
        result = extractor.extract_from_chunk(req.text, req.chunk_id)
        return {
            "chunk_id": req.chunk_id,
            "entities": result["entities"],
            "relations": result["relations"],
            "entity_count": len(result["entities"]),
            "relation_count": len(result["relations"]),
        }
    except Exception as e:
        raise HTTPException(500, f"实体抽取失败: {str(e)}")


@router.post("/graph/build")
async def build_knowledge_graph(req: GraphBuildRequest):
    """构建KB全局知识图谱（从多个chunk）"""
    try:
        from src.knowledge_enhancer.relation_extractor import RelationExtractor
        extractor = RelationExtractor()
        graph = extractor.build_global_graph(req.chunks)
        return graph
    except Exception as e:
        raise HTTPException(500, f"图谱构建失败: {str(e)}")


@router.get("/graph/{kb_id}")
async def get_graph_from_stored(kb_id: int):
    """从已存储的向量库chunk构建图谱"""
    try:
        from src.vector_store.milvus_store import get_milvus_store
        from src.knowledge_enhancer.relation_extractor import RelationExtractor

        store = get_milvus_store()
        raw_chunks = store.get_chunks(kb_id)

        if not raw_chunks:
            return {"nodes": [], "edges": [], "stats": {"total_nodes": 0, "total_edges": 0, "node_types": []}}

        chunks_data = [{"chunk_id": i, "content": c.get("content", "")} for i, c in enumerate(raw_chunks)]
        extractor = RelationExtractor()
        graph = extractor.build_global_graph(chunks_data)
        return graph
    except Exception as e:
        raise HTTPException(500, f"图谱查询失败: {str(e)}")


# ==================== 抽取式摘要 ====================

class SummarizeRequest(BaseModel):
    text: str
    max_len: int = 80


@router.post("/graph/summarize")
async def extractive_summarize(req: SummarizeRequest):
    """抽取式摘要：基于实体密度的关键句提取"""
    try:
        from src.knowledge_enhancer.entity_extractor import EntityExtractor
        extractor = EntityExtractor()

        # 拆句
        sentences = _split_sentences(req.text)
        if not sentences:
            return {"summary": req.text[:req.max_len] + "..." if len(req.text) > req.max_len else req.text, "method": "truncate"}

        # 每句评分：实体密度 + 位置加权
        scored = []
        for i, sent in enumerate(sentences):
            ner = extractor.extract(sent)
            entity_count = len(ner.get("entities", []))
            # 位置加权：首句×1.3，末句×1.1
            pos_weight = 1.3 if i == 0 else (1.1 if i == len(sentences) - 1 else 1.0)
            score = entity_count * pos_weight + len(sent) * 0.001
            scored.append((sent, score))

        # 按分数排序，选 Top-2
        scored.sort(key=lambda x: x[1], reverse=True)
        top = [s for s, _ in scored[:2]]
        summary = "".join(top)
        if not summary.endswith("。") and not summary.endswith("！") and not summary.endswith("？"):
            summary += "。"

        if len(summary) > req.max_len:
            summary = summary[:req.max_len] + "..."

        return {"summary": summary, "method": "extractive", "sentence_count": len(sentences), "selected": min(2, len(sentences))}
    except Exception as e:
        raise HTTPException(500, f"摘要生成失败: {str(e)}")


def _split_sentences(text: str) -> list:
    """按中文标点拆句"""
    import re
    pattern = r'([^。！？!?\n]+[。！？!?]?)'
    parts = re.findall(pattern, text)
    return [p.strip() for p in parts if p.strip() and len(p.strip()) > 3]
