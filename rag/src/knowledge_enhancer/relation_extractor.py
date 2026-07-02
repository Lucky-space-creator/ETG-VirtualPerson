"""
关系抽取器 — 基于同现（co-occurrence）+ 规则推导实体间关系
"""

from typing import List, Dict
from collections import defaultdict
from .entity_extractor import EntityExtractor


class RelationExtractor:
    """从实体同现 + 上下文规则推导关系"""

    # 关系触发词规则
    RELATION_PATTERNS = {
        "位于": {"source_type": "building", "target_type": "location"},
        "地处": {"source_type": "building", "target_type": "location"},
        "修建于": {"source_type": "building", "target_type": "time"},
        "建于": {"source_type": "building", "target_type": "time"},
        "始建于": {"source_type": "building", "target_type": "time"},
        "落成于": {"source_type": "building", "target_type": "time"},
        "重修于": {"source_type": "event", "target_type": "time"},
        "扩建于": {"source_type": "event", "target_type": "time"},
        "主持建造": {"source_type": "person", "target_type": "building"},
        "修建": {"source_type": "person", "target_type": "building"},
        "设计": {"source_type": "person", "target_type": "building"},
        "供奉": {"source_type": "building", "target_type": "person"},
    }

    def __init__(self):
        self.entity_extractor = EntityExtractor()

    def extract_from_chunk(self, text: str, chunk_id: int = 0) -> Dict:
        """从单个chunk提取实体+关系"""
        result = self.entity_extractor.extract(text)
        entities = result["entities"]

        relations = []

        # 1. 规则关系：基于触发词
        for trigger, types in self.RELATION_PATTERNS.items():
            if trigger not in text:
                continue
            srcs = [e for e in entities if e["type"] == types["source_type"]]
            tgts = [e for e in entities if e["type"] == types["target_type"]]
            for s in srcs:
                for t in tgts:
                    if s["name"] != t["name"]:
                        relations.append({
                            "source": s["name"],
                            "target": t["name"],
                            "relation": trigger,
                            "sourceType": s["type"],
                            "targetType": t["type"],
                            "chunkId": chunk_id,
                        })

        # 2. 同现关系：同一chunk中不同类型实体互相连接
        for i, e1 in enumerate(entities):
            for j, e2 in enumerate(entities):
                if i >= j or e1["type"] == e2["type"] or e1["name"] == e2["name"]:
                    continue
                # 避免重复（排序保证 source < target）
                if (e1["name"], e2["name"]) < (e2["name"], e1["name"]):
                    continue
                relations.append({
                    "source": e1["name"],
                    "target": e2["name"],
                    "relation": "co_occur",
                    "sourceType": e1["type"],
                    "targetType": e2["type"],
                    "chunkId": chunk_id,
                })

        return {
            "chunk_id": chunk_id,
            "entities": entities,
            "relations": relations,
        }

    def build_global_graph(self, chunks_data: List[Dict]) -> Dict:
        """从多个chunk构建全局知识图谱

        Args:
            chunks_data: [{"chunk_id": 0, "content": "..."}, ...]

        Returns:
            {"nodes": [...], "edges": [...], "stats": {...}}
        """
        nodes_map = {}   # {name: {type, count, chunkIds}}
        edges_map = {}   # {(source,target,relation): {count, chunkIds}}

        for item in chunks_data:
            chunk_result = self.extract_from_chunk(
                item.get("content", ""),
                item.get("chunk_id", 0),
            )

            # 聚合节点
            for e in chunk_result["entities"]:
                key = e["name"]
                if key not in nodes_map:
                    nodes_map[key] = {
                        "name": key,
                        "type": e["type"],
                        "count": 0,
                        "chunkIds": set(),
                    }
                nodes_map[key]["count"] += 1
                nodes_map[key]["chunkIds"].add(item.get("chunk_id", 0))

            # 聚合边
            for r in chunk_result["relations"]:
                key = (r["source"], r["target"], r["relation"])
                if key not in edges_map:
                    edges_map[key] = {
                        "source": r["source"],
                        "target": r["target"],
                        "relation": r["relation"],
                        "sourceType": r.get("sourceType", ""),
                        "targetType": r.get("targetType", ""),
                        "count": 0,
                        "chunkIds": set(),
                    }
                edges_map[key]["count"] += 1
                edges_map[key]["chunkIds"].add(item.get("chunk_id", 0))

        # 转换set为list便于JSON序列化
        nodes = []
        for n in nodes_map.values():
            n["chunkIds"] = sorted(list(n["chunkIds"]))
            n["id"] = n["name"]  # ECharts 用 id 字段
            nodes.append(n)

        edges = []
        for e in edges_map.values():
            e["chunkIds"] = sorted(list(e["chunkIds"]))
            e["weight"] = e["count"]
            edges.append(e)

        return {
            "nodes": sorted(nodes, key=lambda x: x["count"], reverse=True),
            "edges": sorted(edges, key=lambda x: x["count"], reverse=True),
            "stats": {
                "total_nodes": len(nodes),
                "total_edges": len(edges),
                "node_types": list(set(n["type"] for n in nodes)),
            },
        }
