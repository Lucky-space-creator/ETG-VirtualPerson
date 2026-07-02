from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import Optional, List, Dict, Any
import json, time

router = APIRouter()

# 全局检索器（按景区隔离）
_searchers = {}

def _get_searcher(scenic_spot: str = "default", force=False):
    """获取指定景区的检索器"""
    global _searchers
    if scenic_spot not in _searchers or force:
        _rebuild_searcher(scenic_spot)
    return _searchers.get(scenic_spot)

def _refresh_searcher(scenic_spot: str = None):
    """刷新指定景区或所有景区的检索器"""
    if scenic_spot:
        _rebuild_searcher(scenic_spot)
    else:
        for spot in list(_searchers.keys()):
            _rebuild_searcher(spot)

def _rebuild_searcher(scenic_spot: str = "default"):
    """重建指定景区的检索器"""
    global _searchers
    from src.retrieval.hybrid_search import HybridSearcher
    from src.doc_store import get_doc_store
    from src.vector_store.milvus_store import get_milvus_store

    searcher = HybridSearcher()
    all_docs = []

    # 从DocStore加载（按景区过滤）
    doc_store = get_doc_store()
    for kb_id in range(100):
        docs = doc_store.get_chunks(str(kb_id))
        # 根据kb_id前缀判断是否属于该景区
        for doc in docs:
            if isinstance(doc, dict):
                content = doc.get("content", "")
                doc_spot = doc.get("scenic_spot", "default")
                if doc_spot == scenic_spot and content:
                    all_docs.append(content)
            elif isinstance(doc, str) and doc:
                all_docs.append(doc)

    # 从SQLite向量库加载
    vec_store = get_milvus_store()
    for kb_id in range(100):
        chunks = vec_store.get_chunks(kb_id)
        for ch in chunks:
            content = ch.get("content", "")
            if content and content not in all_docs:
                all_docs.append(content)

    if all_docs:
        searcher.index(all_docs)

    _searchers[scenic_spot] = searcher


class SearchRequest(BaseModel):
    query: str
    kb_id: Optional[int] = 1
    scenic_spot: Optional[str] = "default"
    top_k: int = 5
    entity_filter: Optional[Dict[str, Any]] = None


class IndexRequest(BaseModel):
    kb_id: str = "1"
    scenic_spot: str = "default"
    chunks: List[str]


@router.post("/search/index")
async def index_chunks(req: IndexRequest):
    """预索引文档块（搜索前调用）"""
    from src.doc_store import get_doc_store
    store = get_doc_store()
    # 将景区信息附加到chunks
    enriched_chunks = []
    for chunk in req.chunks:
        enriched_chunks.append({
            "content": chunk,
            "scenic_spot": req.scenic_spot,
            "kb_id": req.kb_id
        })
    store.add_chunks(req.kb_id, enriched_chunks)
    _refresh_searcher(req.scenic_spot)
    return {"status": "ok", "indexed": len(req.chunks), "scenic_spot": req.scenic_spot}


@router.post("/search/vector")
async def vector_search(req: SearchRequest):
    """纯向量语义检索"""
    start = time.time()
    try:
        searcher = _get_searcher(req.scenic_spot or "default")
        if not searcher:
            return {"chunks": [], "total_found": 0, "search_type": "vector", "elapsed_ms": 0}
        chunks = searcher.vector_search(req.query, str(req.kb_id or 1), req.top_k)
        elapsed = (time.time() - start) * 1000
        return {"chunks": chunks, "total_found": len(chunks), "search_type": "vector", "elapsed_ms": round(elapsed, 1)}
    except Exception as e:
        raise HTTPException(500, f"向量检索失败: {str(e)}")


@router.post("/search/hybrid")
async def hybrid_search(req: SearchRequest):
    """混合检索：向量 + BM25 → RRF 融合"""
    start = time.time()
    try:
        searcher = _get_searcher(req.scenic_spot or "default")
        if not searcher:
            return {"chunks": [], "total_found": 0, "search_type": "hybrid", "elapsed_ms": 0}
        chunks = searcher.hybrid_search(req.query, str(req.kb_id or 1), req.top_k)
        elapsed = (time.time() - start) * 1000
        return {"chunks": chunks, "total_found": len(chunks), "search_type": "hybrid", "elapsed_ms": round(elapsed, 1)}
    except Exception as e:
        raise HTTPException(500, f"混合检索失败: {str(e)}")


@router.get("/search/spots")
async def list_scenic_spots():
    """获取所有景区列表"""
    from src.doc_store import get_doc_store
    doc_store = get_doc_store()

    # 从知识库中提取景区列表
    spots = [
        {"id": "lingshan", "name": "灵山胜境", "description": "无锡灵山大佛景区"},
        {"id": "zhoubian", "name": "周庄古镇", "description": "江南水乡古镇"},
        {"id": "xihu", "name": "西湖风景区", "description": "杭州西湖"},
        {"id": "gugong", "name": "故宫博物院", "description": "北京故宫"},
        {"id": "default", "name": "通用知识库", "description": "通用旅游知识"}
    ]

    return {"spots": spots, "total": len(spots)}


class RerankBody(BaseModel):
    query: str
    documents: list = []
    top_k: int = 5


class RewriteBody(BaseModel):
    query: str
    method: str = "hyde"
    history: Optional[list] = None


@router.post("/search/rerank")
async def rerank_documents(req: RerankBody):
    """Cross-Encoder 重排序"""
    try:
        from src.retrieval.reranker import Reranker
        reranker = Reranker()
        ranked = reranker.rerank(req.query, req.documents, req.top_k)
        return {"documents": ranked, "count": len(ranked)}
    except Exception as e:
        raise HTTPException(500, f"重排序失败: {str(e)}")


@router.post("/search/query-rewrite")
async def query_rewrite(req: RewriteBody):
    """查询改写：HyDE / 多查询 / 上下文补全"""
    try:
        from src.retrieval.query_rewriter import QueryRewriter
        rewriter = QueryRewriter()
        rewrites = rewriter.rewrite(req.query, method=req.method, history=req.history)
        return {"original": req.query, "rewrites": rewrites, "method": req.method}
    except Exception as e:
        raise HTTPException(500, f"查询改写失败: {str(e)}")


@router.post("/search/dedup")
async def dedup_documents(documents: List[dict], threshold: float = 0.95):
    """内容去重"""
    try:
        from src.retrieval.post_processor import deduplicate
        result = deduplicate(documents, threshold)
        return {"documents": result, "original_count": len(documents), "dedup_count": len(result)}
    except Exception as e:
        raise HTTPException(500, f"去重失败: {str(e)}")


@router.get("/tools")
async def get_tools():
    """获取可用工具列表"""
    from src.doc_store import get_doc_store
    store = get_doc_store()
    return {
        "tools": [
            {"name": "kb_search", "description": "知识库语义检索(BGE向量+BM25→RRF融合)", "params": ["query", "kb_id", "top_k"]},
            {"name": "kb_keyword", "description": "知识库关键词检索(BM25)", "params": ["keywords", "kb_id", "top_k"]},
            {"name": "graph_traverse", "description": "知识图谱遍历(实体→关联实体→扩展查询)", "params": ["entity", "kb_id"]},
            {"name": "web_search", "description": "联网搜索补充", "params": ["query", "max_results"]},
            {"name": "location_lookup", "description": "地图位置查询", "params": ["lat", "lng", "radius"]},
            {"name": "memory_query", "description": "对话记忆查询", "params": ["session_id", "query"]},
        ],
        "indexed_kbs": store.kb_count(),
        "total_chunks": store.total_chunks(),
    }


# ==================== 图谱缓存 ====================
_graph_cache = {}  # {kb_id: {graph, timestamp}}


def _get_cached_graph(kb_id: int):
    """获取缓存的KB知识图谱（5分钟有效）"""
    import time as _time
    now = _time.time()
    entry = _graph_cache.get(kb_id)
    if entry and now - entry["ts"] < 300:
        return entry["graph"]
    return None


def _cache_graph(kb_id: int, graph: dict):
    import time as _time
    _graph_cache[kb_id] = {"graph": graph, "ts": _time.time()}


# ==================== Agentic RAG ====================

class DebugSearchBody(BaseModel):
    kb_id: int = 1
    query: str
    top_k: int = 5


@router.post("/search/debug")
async def debug_vector_search(req: DebugSearchBody):
    """调试向量搜索（独立前端调用）"""
    try:
        from src.vector_store.milvus_store import get_milvus_store
        from src.embedder.bge_embedder import get_embedder
        embedder = get_embedder()
        query_vec = embedder.encode([req.query])[0]
        store = get_milvus_store()
        hits = store.search(req.kb_id, query_vec, req.top_k)
        return {"query": req.query, "kb_id": req.kb_id, "hits": hits, "total": store.count(req.kb_id)}
    except Exception as e:
        raise HTTPException(500, str(e))


class AgenticRequest(BaseModel):
    query: str
    kb_id: int = 1
    max_iterations: int = 5
    quality_threshold: float = 0.8
    use_graph: bool = True  # 是否启用知识图谱增强


@router.post("/search/agentic")
async def agentic_search(req: AgenticRequest):
    """Agentic RAG: Plan → Act → Observe → Reflect 完整闭环（含图谱增强）"""
    start = time.time()
    trace = []

    try:
        # === Plan: 查询改写 + 图谱实体扩展 ===
        trace.append({"stage": "plan", "status": "start"})
        from src.retrieval.query_rewriter import QueryRewriter
        rewriter = QueryRewriter()
        sub_queries = rewriter.rewrite(req.query, method="multi_query")
        trace.append({"stage": "plan", "queries": sub_queries, "count": len(sub_queries)})

        # 图谱增强：抽取查询实体 → 找关联实体 → 扩展子查询
        graph_entities = []
        if req.use_graph:
            try:
                from src.knowledge_enhancer.relation_extractor import RelationExtractor
                extractor = RelationExtractor()
                query_ner = extractor.extract_from_chunk(req.query, chunk_id=-1)
                graph_entities = query_ner.get("entities", [])
                if graph_entities:
                    # 优先用缓存图谱，未命中则重建
                    graph = _get_cached_graph(req.kb_id)
                    if not graph:
                        from src.vector_store.milvus_store import get_milvus_store
                        store = get_milvus_store()
                        stored = store.get_chunks(req.kb_id)
                        if stored:
                            chunks_data = [{"chunk_id": i, "content": c.get("content","")} for i,c in enumerate(stored)]
                            graph = extractor.build_global_graph(chunks_data)
                            _cache_graph(req.kb_id, graph)
                    if graph:
                        query_entity_names = {e["name"] for e in graph_entities}
                        related = set()
                        for edge in graph.get("edges", []):
                            if edge["source"] in query_entity_names and edge["target"] not in query_entity_names:
                                related.add(edge["target"])
                            elif edge["target"] in query_entity_names and edge["source"] not in query_entity_names:
                                related.add(edge["source"])
                        if related:
                            expanded = list(sub_queries)
                            for entity in list(related)[:3]:
                                expanded.append(f"{req.query} {entity}")
                            sub_queries = expanded
                            trace.append({"stage": "plan", "graph_expansion": {
                                "query_entities": list(query_entity_names),
                                "related_entities": list(related)[:10],
                                "expanded_queries": len(expanded)
                            }})
            except Exception as e:
                trace.append({"stage": "plan", "graph_expansion": "failed", "error": str(e)})

        # === Act + Observe + Reflect 循环 ===
        searcher = _get_searcher()
        from src.retrieval.post_processor import deduplicate
        all_results = []
        iteration = 0

        for iteration in range(req.max_iterations):
            trace.append({"stage": "act", "iteration": iteration + 1, "status": "start"})

            # Act: 对每个子查询执行混合检索
            round_results = []
            for sq in sub_queries:
                chunks = searcher.hybrid_search(sq, str(req.kb_id), top_k=10)
                if chunks:
                    round_results.extend(chunks)

            # Act: 图谱加权（相关实体出现更多的chunk加分）
            if req.use_graph and graph_entities:
                entity_names = {e["name"] for e in graph_entities}
                for r in round_results:
                    content = r.get("content", "") if isinstance(r, dict) else str(r)
                    hit_count = sum(1 for en in entity_names if en in content)
                    if isinstance(r, dict):
                        r["score"] = r.get("score", 0) * (1 + 0.1 * hit_count)

            # Act: 去重
            round_results = deduplicate(round_results)

            trace.append({"stage": "act", "iteration": iteration + 1,
                          "results": len(round_results), "status": "done"})
            all_results.extend(round_results)

            # Observe: 计算质量分数
            quality = _evaluate_quality(round_results)
            trace.append({"stage": "observe", "iteration": iteration + 1,
                          "quality_score": round(quality, 4),
                          "threshold": req.quality_threshold})

            # Reflect: 达标则退出
            if quality >= req.quality_threshold or iteration + 1 >= req.max_iterations:
                trace.append({"stage": "reflect", "decision": "stop"})
                break

            trace.append({"stage": "reflect", "decision": "retry"})
            sub_queries.append(req.query + " 详细介绍")

        # === 最终去重 ===
        final = deduplicate(all_results)
        top_k = getattr(req, 'top_k', 5)

        elapsed = (time.time() - start) * 1000
        return {
            "query": req.query,
            "kb_id": req.kb_id,
            "results": final[:top_k],
            "total_found": len(final),
            "iterations": min(iteration + 1, req.max_iterations),
            "trace": trace,
            "elapsed_ms": round(elapsed, 1),
        }
    except Exception as e:
        import traceback as tb
        tb.print_exc()
        raise HTTPException(500, f"Agentic error: {str(e)}")


def _evaluate_quality(chunks: list) -> float:
    """评估检索质量（平均分 + 多样性）"""
    if not chunks:
        return 0.0

    scores = []
    seen = set()
    for c in chunks:
        if isinstance(c, dict):
            scores.append(abs(c.get("score", c.get("rerank_score", 0))))
            seen.add(c.get("content", "")[:100])
        elif isinstance(c, str):
            scores.append(0.1)
            seen.add(c[:100])

    avg_score = sum(scores) / max(len(scores), 1)
    diversity = len(seen) / max(len(chunks), 1)
    return round(0.6 * avg_score + 0.4 * diversity, 4)
