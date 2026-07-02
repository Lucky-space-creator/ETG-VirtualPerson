from typing import List, Dict, Optional


class QueryRewriter:
    """查询改写器：HyDE / 多查询分解 / 上下文补全"""

    def rewrite(
        self,
        query: str,
        method: str = "hyde",
        history: Optional[List[Dict]] = None,
    ) -> List[str]:
        """根据指定方法改写查询"""
        if method == "hyde":
            return self._hyde_rewrite(query)
        elif method == "multi_query":
            return self._multi_query_rewrite(query)
        elif method == "context_augment":
            return self._context_augment_rewrite(query, history)
        else:
            return [query]

    @staticmethod
    def _hyde_rewrite(query: str) -> List[str]:
        """HyDE：假设文档嵌入"""
        rewrites = [query]

        # 短查询：补充上下文词汇
        if len(query) < 15:
            rewrites.append(f"关于{query}的详细资料和介绍")

        # 含对比关键词：拆解
        if any(kw in query for kw in ["对比", "区别", "不同"]):
            parts = query.replace("对比", " ").replace("区别", " ").replace("不同", " ").split()
            for part in parts:
                if len(part) >= 2:
                    rewrites.append(part)

        return rewrites

    @staticmethod
    def _multi_query_rewrite(query: str) -> List[str]:
        """多查询分解"""
        rewrites = [query]

        # 根据语义特征拆分
        separators = ["和", "与", "及", "以及", "还有"]
        for sep in separators:
            if sep in query:
                parts = query.split(sep, 1)
                if len(parts) == 2 and len(parts[0]) >= 3 and len(parts[1]) >= 3:
                    rewrites.append(parts[0].strip())
                    rewrites.append(parts[1].strip())
                break

        return rewrites

    @staticmethod
    def _context_augment_rewrite(
        query: str,
        history: Optional[List[Dict]] = None,
    ) -> List[str]:
        """上下文补全：利用对话历史补充查询"""
        if not history:
            return [query]

        # 取最近一轮对话作为上下文
        context_parts = []
        for msg in history[-2:]:
            content = msg.get("content", "")
            if content and len(content) < 200:
                context_parts.append(content)

        if context_parts:
            augmented = " ".join(context_parts + [query])
            return [augmented, query]

        return [query]
