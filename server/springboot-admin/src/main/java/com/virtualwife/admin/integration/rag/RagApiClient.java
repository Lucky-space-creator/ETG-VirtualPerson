package com.virtualwife.admin.integration.rag;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG嵌入服务客户端
 * 调用 FastAPI RAG服务 (:5001) 的文档处理、切割、向量化、检索等接口
 */
@Slf4j
@Component
public class RagApiClient {

    @Value("${virtualwife.rag.api-url}")
    private String ragApiUrl;

    @Value("${virtualwife.django.api-url:}")
    private String djangoApiUrl;

    // ==================== 文档处理 ====================

    /**
     * 解析文档内容（返回 text + page_count 等元数据）
     */
    public Map<String, Object> parseDocumentWithMeta(byte[] fileBytes, String fileName) throws Exception {
        File tempFile = FileUtil.writeBytes(fileBytes, 
            new File(System.getProperty("java.io.tmpdir"), "rag_parse_" + System.currentTimeMillis() + "_" + fileName));
        try {
            HttpResponse resp = HttpRequest.post(ragApiUrl + "/api/rag/parse")
                    .form("file", tempFile)
                    .timeout(120000)
                    .execute();
            if (resp.isOk()) {
                return JSONUtil.parseObj(resp.body()).toBean(Map.class);
            }
            log.error("RAG解析失败: status={}, body={}", resp.getStatus(), resp.body());
            throw new RuntimeException("RAG文档解析失败: " + resp.getStatus());
        } finally {
            tempFile.delete();
        }
    }

    /**
     * 解析文档内容（仅返回文本，兼容旧代码）
     */
    public String parseDocument(byte[] fileBytes, String fileName) throws Exception {
        Map<String, Object> result = parseDocumentWithMeta(fileBytes, fileName);
        return (String) result.getOrDefault("text", "");
    }

    /**
     * 执行文本切割（使用chunk/preview端点）
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> chunkDocument(String text, String strategy, int chunkSize, int chunkOverlap) {
        Map<String, Object> body = new HashMap<>();
        body.put("text", text);
        body.put("chunk_strategy", strategy);
        body.put("chunk_size", chunkSize);
        body.put("chunk_overlap", chunkOverlap);

        HttpResponse resp = HttpRequest.post(ragApiUrl + "/api/rag/chunk/preview")
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(body))
                .timeout(120000)
                .execute();

        if (resp.isOk()) {
            JSONObject result = JSONUtil.parseObj(resp.body());
            // 适配多种返回格式
            JSONArray chunks = result.getJSONArray("chunks");
            if (chunks == null) chunks = result.getJSONArray("data");
            if (chunks == null) {
                // 如果返回的是简单列表
                List<Map<String, Object>> list = new ArrayList<>();
                String text2 = result.getStr("chunks", result.getStr("text", ""));
                return (List) JSONUtil.toList("[" + text2 + "]", Map.class);
            }
            try {
                return (List) JSONUtil.toList(chunks, Map.class);
            } catch (Exception e) {
                // 如果chunks是字符串数组，包装为content
                List<Map<String, Object>> wrapped = new ArrayList<>();
                for (int i = 0; i < chunks.size(); i++) {
                    String chunkText = chunks.getStr(i);
                    Map<String, Object> m = new HashMap<>();
                    m.put("content", chunkText != null ? chunkText : chunks.get(i).toString());
                    m.put("token_count", chunkText != null ? chunkText.length() : 0);
                    wrapped.add(m);
                }
                return wrapped;
            }
        }
        log.error("RAG切割失败: status={}, body={}", resp.getStatus(), resp.body());
        throw new RuntimeException("RAG文本切割失败: " + resp.getStatus());
    }

    /**
     * 切割预览
     */
    public Map<String, Object> chunkPreview(String text, String strategy, int chunkSize, int chunkOverlap) {
        Map<String, Object> body = new HashMap<>();
        body.put("text", text);
        body.put("chunk_strategy", strategy);
        body.put("chunk_size", chunkSize);
        body.put("chunk_overlap", chunkOverlap);

        HttpResponse resp = HttpRequest.post(ragApiUrl + "/api/rag/chunk/preview")
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(body))
                .timeout(120000)
                .execute();

        if (resp.isOk()) {
            return JSONUtil.parseObj(resp.body()).toBean(Map.class);
        }
        throw new RuntimeException("RAG切割预览失败");
    }

    // ==================== 向量化 ====================

    /**
     * 批量向量化
     */
    public List<List<Double>> batchEmbed(List<String> texts) {
        Map<String, Object> body = new HashMap<>();
        body.put("texts", texts);

        HttpResponse resp = HttpRequest.post(ragApiUrl + "/api/rag/embedding/batch")
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(body))
                .timeout(60000)
                .execute();

        if (resp.isOk()) {
            JSONObject resultObj = JSONUtil.parseObj(resp.body());
            JSONArray vectors = resultObj.getJSONArray("vectors");
            @SuppressWarnings("unchecked")
            List<List<Double>> vecList = (List) JSONUtil.toList(vectors, List.class);
            return vecList;
        }
        throw new RuntimeException("RAG向量化失败");
    }

    /**
     * 嵌入 + 存入 Milvus 向量数据库（一步完成）
     */
    public void storeVectors(int kbId, List<Map<String, Object>> chunks) {
        Map<String, Object> body = new HashMap<>();
        body.put("kb_id", kbId);
        body.put("chunks", chunks);

        HttpResponse resp = HttpRequest.post(ragApiUrl + "/api/rag/embedding/store")
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(body))
                .timeout(300000)  // 5分钟，大文档BGE嵌入需时
                .execute();

        if (resp.isOk()) {
            JSONObject result = JSONUtil.parseObj(resp.body());
            log.info("向量已存入Milvus: kbId={}, stored={}, total={}",
                    kbId, result.getInt("stored"), result.getInt("total_vectors"));
        } else {
            log.error("向量存储失败: status={}, body={}", resp.getStatus(), resp.body());
            throw new RuntimeException("向量存储失败: " + resp.getStatus());
        }
    }

    /**
     * 清理指定KB的所有向量（重新嵌入前调用）
     */
    public void clearVectors(int kbId) {
        HttpResponse resp = HttpRequest.delete(ragApiUrl + "/api/rag/embedding/clear/" + kbId)
                .timeout(10000)
                .execute();
        if (resp.isOk()) {
            log.info("向量已清理: kbId={}", kbId);
        } else {
            log.warn("向量清理失败: kbId={}, status={}", kbId, resp.getStatus());
        }
    }

    /**
     * 查询Milvus中向量数量
     */
    public int getVectorCount(int kbId) {
        HttpResponse resp = HttpRequest.get(ragApiUrl + "/api/rag/embedding/count/" + kbId)
                .timeout(10000)
                .execute();
        if (resp.isOk()) {
            return JSONUtil.parseObj(resp.body()).getInt("total_vectors", 0);
        }
        return 0;
    }

    /**
     * 将chunk索引到RAG检索引擎
     */
    public void indexChunks(String kbId, List<String> chunks) {
        Map<String, Object> body = new HashMap<>();
        body.put("kb_id", kbId);
        body.put("chunks", chunks);

        HttpResponse resp = HttpRequest.post(ragApiUrl + "/api/rag/search/index")
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(body))
                .timeout(30000)
                .execute();

        if (!resp.isOk()) {
            throw new RuntimeException("RAG索引失败: " + resp.getStatus());
        }
    }

    // ==================== 检索 ====================

    /**
     * 混合检索
     */
    public Map<String, Object> hybridSearch(String query, int kbId, int topK) {
        Map<String, Object> body = new HashMap<>();
        body.put("query", query);
        body.put("kb_id", kbId);
        body.put("top_k", topK);

        HttpResponse resp = HttpRequest.post(ragApiUrl + "/api/rag/search/hybrid")
                .body(JSONUtil.toJsonStr(body))
                .timeout(30000)
                .execute();

        if (resp.isOk()) {
            return JSONUtil.parseObj(resp.body()).toBean(Map.class);
        }
        throw new RuntimeException("RAG检索失败");
    }

    /**
     * 重排序
     */
    public List<Map<String, Object>> rerank(String query, List<Map<String, Object>> documents, int topK) {
        Map<String, Object> body = new HashMap<>();
        body.put("query", query);
        body.put("documents", documents);
        body.put("top_k", topK);

        HttpResponse resp = HttpRequest.post(ragApiUrl + "/api/rag/search/rerank")
                .body(JSONUtil.toJsonStr(body))
                .timeout(30000)
                .execute();

        if (resp.isOk()) {
            JSONObject result = JSONUtil.parseObj(resp.body());
            JSONArray docs = result.getJSONArray("documents");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> ret = (List) JSONUtil.toList(docs, Map.class);
            return ret;
        }
        throw new RuntimeException("RAG重排序失败");
    }

    // ==================== 知识图谱 ====================

    /**
     * LLM 摘要（优先 Django OpenAI/Ollama，降级 RAG extractive）
     */
    public String llmSummary(String text, int maxLen) {
        // 1. 尝试 Django LLM API
        if (djangoApiUrl != null && !djangoApiUrl.isBlank()) {
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("text", text);
                body.put("max_len", maxLen);
                HttpResponse resp = HttpRequest.post(djangoApiUrl + "/chatbot/llm/summarize")
                        .header("Content-Type", "application/json")
                        .body(JSONUtil.toJsonStr(body))
                        .timeout(35000)
                        .execute();
                if (resp.isOk()) {
                    JSONObject d = JSONUtil.parseObj(resp.body());
                    String summary = d.getJSONObject("data").getStr("summary");
                    if (summary != null && !summary.isBlank()) return summary;
                }
            } catch (Exception e) {
                log.debug("Django LLM 摘要失败，降级 RAG: {}", e.getMessage());
            }
        }

        // 2. 降级 RAG extractive
        return extractiveSummary(text, maxLen);
    }

    /**
     * 抽取式摘要（基于实体密度，RAG本地）
     */
    public String extractiveSummary(String text, int maxLen) {
        Map<String, Object> body = new HashMap<>();
        body.put("text", text);
        body.put("max_len", maxLen);

        HttpResponse resp = HttpRequest.post(ragApiUrl + "/api/rag/graph/summarize")
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(body))
                .timeout(10000)
                .execute();

        if (resp.isOk()) {
            return JSONUtil.parseObj(resp.body()).getStr("summary", "");
        }
        // 降级：前80字
        return text.length() > 80 ? text.substring(0, 80) + "..." : text;
    }

    /**
     * 单chunk实体+关系抽取
     */
    public Map<String, Object> extractEntities(String text, int chunkId) {
        Map<String, Object> body = new HashMap<>();
        body.put("text", text);
        body.put("chunk_id", chunkId);

        HttpResponse resp = HttpRequest.post(ragApiUrl + "/api/rag/graph/extract")
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(body))
                .timeout(30000)
                .execute();

        if (resp.isOk()) {
            return JSONUtil.parseObj(resp.body()).toBean(Map.class);
        }
        log.warn("实体抽取失败: status={}", resp.getStatus());
        return Map.of("entities", List.of(), "relations", List.of());
    }

    /**
     * 构建KB全局知识图谱
     */
    public Map<String, Object> buildGraph(int kbId, List<Map<String, Object>> chunks) {
        Map<String, Object> body = new HashMap<>();
        body.put("kb_id", kbId);
        body.put("chunks", chunks);

        HttpResponse resp = HttpRequest.post(ragApiUrl + "/api/rag/graph/build")
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(body))
                .timeout(60000)
                .execute();

        if (resp.isOk()) {
            return JSONUtil.parseObj(resp.body()).toBean(Map.class);
        }
        throw new RuntimeException("图谱构建失败: " + resp.getStatus());
    }
}
