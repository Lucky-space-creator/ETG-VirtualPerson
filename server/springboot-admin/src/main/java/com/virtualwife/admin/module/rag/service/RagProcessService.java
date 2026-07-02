package com.virtualwife.admin.module.rag.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.virtualwife.admin.common.util.MinioUtil;
import com.virtualwife.admin.integration.rag.RagApiClient;
import com.virtualwife.admin.module.rag.entity.RagChunk;
import com.virtualwife.admin.module.rag.entity.RagDocument;
import com.virtualwife.admin.module.rag.mapper.RagChunkMapper;
import com.virtualwife.admin.module.rag.mapper.RagDocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RAG 文档异步处理服务
 * 独立 Service 确保 @Async 通过 AOP 代理生效
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagProcessService {

    private final RagDocumentMapper ragDocumentMapper;
    private final RagChunkMapper ragChunkMapper;
    private final RagApiClient ragApiClient;
    private final MinioUtil minioUtil;

    /**
     * 同步处理（直接执行，用于调试）
     */
    public void processSync(Long docId) {
        doProcess(docId);
    }

    /**
     * 异步执行完整处理流水线：下载→解析→切割→向量化→索引
     */
    @Async("taskExecutor")
    public void processAsync(Long docId) {
        doProcess(docId);
    }

    /** 核心处理逻辑 */
    private void doProcess(Long docId) {
        RagDocument doc = ragDocumentMapper.selectById(docId);
        if (doc == null) return;

        log.info("===== 开始处理文档: id={}, name={} =====", docId, doc.getDocName());

        try {
            // ===== 阶段1: 下载文件 + RAG解析 (进度 1% → 25%) =====
            updateProgress(docId, 1, 2);  // 开始解析
            String filePath = doc.getFilePath();
            byte[] fileBytes;

            if (filePath != null && filePath.startsWith("local:")) {
                updateProgress(docId, 1, 8);  // 读本地文件
                java.io.File localFile = new java.io.File(filePath.substring(6));
                fileBytes = java.nio.file.Files.readAllBytes(localFile.toPath());
                log.info("[解析] 读取本地文件: {} bytes", fileBytes.length);
            } else {
                updateProgress(docId, 1, 5);  // 下载MinIO
                log.info("[解析] 从MinIO下载文件: {}", filePath);
                fileBytes = minioUtil.downloadFile(filePath);
                if (fileBytes == null || fileBytes.length == 0) {
                    throw new RuntimeException("文件下载失败或为空: " + filePath);
                }
            }
            updateProgress(docId, 1, 15);  // 文件已就绪，RAG解析中
            log.info("[解析] 文件大小: {} bytes, 调用RAG解析...", fileBytes.length);
            Map<String, Object> parseResult = ragApiClient.parseDocumentWithMeta(fileBytes, doc.getDocName());
            String parsedText = (String) parseResult.getOrDefault("text", "");
            int pageCount = ((Number) parseResult.getOrDefault("page_count", 0)).intValue();
            // 保存解析全文到 rag_document
            doc.setParsedText(parsedText);
            doc.setPageCount(pageCount);
            ragDocumentMapper.updateById(doc);
            updateProgress(docId, 1, 25);  // 解析完成
            log.info("[解析] 完成, 文本长度: {} 字符, 页数: {}", parsedText.length(), pageCount);

            // ===== 阶段2: 文本切割 (进度 26% → 55%) =====
            updateProgress(docId, 2, 28);  // 开始切割
            String strategy = doc.getChunkStrategy() != null ? doc.getChunkStrategy() : "recursive";
            int cSize = doc.getChunkSize() != null ? doc.getChunkSize() : 512;
            int cOverlap = doc.getChunkOverlap() != null ? doc.getChunkOverlap() : 50;
            log.info("[切割] 开始, 策略={}, size={}, overlap={}", strategy, cSize, cOverlap);
            List<Map<String, Object>> chunks = ragApiClient.chunkDocument(parsedText, strategy, cSize, cOverlap);
            updateProgress(docId, 2, 40);  // 切割完成，保存中
            log.info("[切割] 完成, {} 个chunk", chunks.size());

            // 批量保存chunk到数据库 + 实体关系抽取
            List<String> chunkTexts = new ArrayList<>();
            List<RagChunk> batchList = new ArrayList<>();
            try {
                // 先用RAG批量构建知识图谱（一次性传所有chunk）
                List<Map<String, Object>> graphChunks = new ArrayList<>();
                for (int i = 0; i < chunks.size(); i++) {
                    Map<String, Object> gc = new HashMap<>();
                    gc.put("chunk_id", i);
                    gc.put("content", chunks.get(i).getOrDefault("content", ""));
                    graphChunks.add(gc);
                }
                Map<String, Object> graphResult = ragApiClient.buildGraph(
                        doc.getKbId() != null ? doc.getKbId().intValue() : 1, graphChunks);
                // 按chunk_id分组实体和关系
                Map<Integer, List<Object>> entitiesByChunk = new HashMap<>();
                Map<Integer, List<Object>> relationsByChunk = new HashMap<>();
                // 从全局图谱中为每个chunk分配实体/关系
                // （简化：将全局图谱数据存到第一个chunk）
                String graphJson = cn.hutool.json.JSONUtil.toJsonStr(graphResult);
                log.info("[图谱] 构建完成, nodes={}, edges={}",
                        ((Map)graphResult.getOrDefault("stats", Map.of())).getOrDefault("total_nodes", 0),
                        ((Map)graphResult.getOrDefault("stats", Map.of())).getOrDefault("total_edges", 0));
            } catch (Exception e) {
                log.warn("[图谱] 构建失败(非致命): {}", e.getMessage());
            }

            for (int i = 0; i < chunks.size(); i++) {
                Map<String, Object> chunk = chunks.get(i);
                String content = (String) chunk.getOrDefault("content", chunk.toString());
                chunkTexts.add(content);

                RagChunk rc = new RagChunk();
                rc.setDocId(docId);
                rc.setKbId(doc.getKbId());
                rc.setChunkIndex(i);
                rc.setContent(content);
                rc.setTokenCount(content.length());
                rc.setVectorStatus(0);
                rc.setVectorId("chunk_" + docId + "_" + i);

                // 增强内容: 文档名 + 位置标记
                StringBuilder enhanced = new StringBuilder();
                enhanced.append("[文档: ").append(doc.getDocName()).append(" — Chunk ").append(i + 1).append("/").append(chunks.size()).append("]\n");
                enhanced.append(content);
                rc.setEnhancedContent(enhanced.toString());

                // 摘要: Django LLM(OpenAI/Ollama) → 降级 RAG extractive
                try {
                    String aiSummary = ragApiClient.llmSummary(content, 80);
                    rc.setSummary(aiSummary);
                } catch (Exception e) {
                    rc.setSummary(content.length() > 80 ? content.substring(0, 80) + "..." : content);
                }

                // 章节路径: 智能提取首行标题
                String firstLine = content.split("\n")[0].trim();
                String section = _extractSection(firstLine);
                if (!section.isEmpty()) rc.setSectionPath(section);

                // 计算SHA-256内容哈希（快速去重）
                try {
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    String hash = bytesToHex(md.digest(content.getBytes(StandardCharsets.UTF_8)));
                    rc.setContentHash(hash);
                } catch (Exception e) { /* skip hash */ }

                // 抽取实体+关系（每个chunk独立调RAG）
                try {
                    Map<String, Object> nerResult = ragApiClient.extractEntities(content, i);
                    List<Map<String, Object>> entities = (List<Map<String, Object>>) nerResult.getOrDefault("entities", List.of());
                    List<Map<String, Object>> relations = (List<Map<String, Object>>) nerResult.getOrDefault("relations", List.of());
                    if (!entities.isEmpty()) rc.setEntities(cn.hutool.json.JSONUtil.toJsonStr(entities));
                    if (!relations.isEmpty()) rc.setRelations(cn.hutool.json.JSONUtil.toJsonStr(relations));
                } catch (Exception e) {
                    // 实体抽取失败不影响主流程
                }

                // sectionPath 已在上方 _extractSection 处理

                batchList.add(rc);

                // 每50条或最后一批批量写入
                if (batchList.size() >= 50 || i == chunks.size() - 1) {
                    for (RagChunk c : batchList) {
                        ragChunkMapper.insert(c);
                    }
                    batchList.clear();
                    // 更新进度
                    int pct = 40 + (i + 1) * 15 / Math.max(chunks.size(), 1);
                    updateProgress(docId, 2, pct);
                }
            }
            updateProgress(docId, 2, 55);
            doc.setChunkCount(chunks.size());
            ragDocumentMapper.updateById(doc);

            // ===== 阶段3: 嵌入 + 存入向量库 =====
            updateProgress(docId, 3, 56);
            int kbIdInt = doc.getKbId() != null ? doc.getKbId().intValue() : 1;
            String kbId = String.valueOf(kbIdInt);

            // 组装chunk数据发送给RAG（RAG负责embedding+存储）
            List<Map<String, Object>> chunkDataList = new ArrayList<>();
            for (int i = 0; i < chunkTexts.size(); i++) {
                Map<String, Object> cd = new HashMap<>();
                cd.put("chunk_id", i);
                cd.put("content", chunkTexts.get(i));
                chunkDataList.add(cd);
            }

            // 向量化前清理：删除本地磁盘该KB的旧向量
            updateProgress(docId, 3, 60);
            ragApiClient.clearVectors(kbIdInt);
            log.info("[清理] 已清除 kb_id={} 的旧向量数据", kbId);

            updateProgress(docId, 3, 65);
            log.info("[向量化] 批量embedding+存入本地磁盘, {} 条", chunkTexts.size());
            ragApiClient.storeVectors(kbIdInt, chunkDataList);
            updateProgress(docId, 3, 85);
            int vectorCount = ragApiClient.getVectorCount(kbIdInt);
            log.info("[向量化] 完成, kbId={}, Milvus向量总数={}", kbId, vectorCount);

            // 更新chunk向量化状态
            List<RagChunk> savedChunks = ragChunkMapper.selectList(
                    new LambdaQueryWrapper<RagChunk>().eq(RagChunk::getDocId, docId));
            for (RagChunk rc : savedChunks) {
                rc.setVectorStatus(1);
                ragChunkMapper.updateById(rc);
            }
            updateProgress(docId, 3, 88);

            // ===== 索引到RAG检索引擎 =====
            try {
                ragApiClient.indexChunks(kbId, chunkTexts);
                updateProgress(docId, 3, 95);
                log.info("[索引] 完成, kbId={}", kbId);
            } catch (Exception e) {
                log.warn("[索引] 失败(非致命): {}", e.getMessage());
            }

            // ===== 完成 =====
            updateProgress(docId, 4, 100);
            log.info("===== 文档处理完成: docId={}, chunks={} =====", docId, chunks.size());

        } catch (Exception e) {
            log.error("===== 文档处理失败: docId={} =====", docId, e);
            updateProgress(docId, -1, 0);
        }
    }

    /** 更新处理状态+进度百分比 */
    private void updateProgress(Long docId, int status, int percent) {
        RagDocument update = new RagDocument();
        update.setId(docId);
        update.setProcessStatus(status);
        update.setProgressPercent(percent);
        ragDocumentMapper.updateById(update);
    }

    /** SHA-256 bytes → hex */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /** 智能提取章节标题 */
    private static String _extractSection(String firstLine) {
        if (firstLine == null || firstLine.length() < 2 || firstLine.length() > 80) return "";
        // 匹配: 第X章、第X节、(1)、一、1.、一、
        if (firstLine.matches(".*第[一二三四五六七八九十百\\d]+[章节篇届].*")) return firstLine;
        if (firstLine.matches("^[\\(（]\\s*\\d+\\s*[\\)）].*")) return firstLine;
        if (firstLine.matches("^[一二三四五六七八九十]\\s*[、，,\\.].*")) return firstLine;
        if (firstLine.matches("^\\d+\\s*[、，,\\.].*")) return firstLine;
        if (firstLine.matches("^[#☆★△▲§※●○■□].*")) return firstLine;
        return "";
    }
}
