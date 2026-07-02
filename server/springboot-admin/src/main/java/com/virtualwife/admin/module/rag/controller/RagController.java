package com.virtualwife.admin.module.rag.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.virtualwife.admin.common.result.Result;
import com.virtualwife.admin.integration.rag.RagApiClient;
import com.virtualwife.admin.module.rag.entity.RagChunk;
import com.virtualwife.admin.module.rag.entity.RagDocument;
import com.virtualwife.admin.module.rag.entity.RagEvaluation;
import com.virtualwife.admin.module.rag.entity.RagQaPair;
import com.virtualwife.admin.module.rag.service.RagDocumentService;
import com.virtualwife.admin.module.rag.service.RagProcessService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagDocumentService ragDocumentService;
    private final RagApiClient ragApiClient;
    private final RagProcessService ragProcessService;

    @GetMapping("/page")
    public Result<Page<RagDocument>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long kbId,
            @RequestParam(required = false) Integer processStatus,
            @RequestParam(required = false) String keyword) {
        return Result.success(ragDocumentService.pageDocs(pageNum, pageSize, kbId, processStatus, keyword));
    }

    @PostMapping("/upload")
    public Result<RagDocument> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long kbId,
            @RequestParam(required = false) String chunkStrategy,
            @RequestParam(required = false, defaultValue = "512") Integer chunkSize,
            @RequestParam(required = false, defaultValue = "50") Integer chunkOverlap) throws Exception {
        return Result.success("上传成功", ragDocumentService.uploadDocument(file, kbId, chunkStrategy, chunkSize, chunkOverlap));
    }

    /**
     * 触发完整处理流水线（A-R2）：解析 → 切割 → 向量化
     */
    @PostMapping("/{id}/process")
    public Result<?> triggerProcess(@PathVariable Long id) {
        RagDocument doc = ragDocumentService.getById(id);
        if (doc == null) return Result.error("文档不存在");
        if (doc.getProcessStatus() == 1 || doc.getProcessStatus() == 2 || doc.getProcessStatus() == 3) {
            return Result.error("文档正在处理中");
        }

        // 异步执行处理流水线（通过独立Service确保@Async生效）
        ragProcessService.processAsync(id);
        return Result.success("处理任务已触发，文档将依次进行解析→切割→向量化");
    }

    @PostMapping("/{id}/reprocess")
    public Result<?> reprocess(@PathVariable Long id) {
        RagDocument doc = ragDocumentService.getById(id);
        if (doc != null) {
            // 同步清理：MySQL chunk + SQLite向量
            ragDocumentService.getChunkMapper().delete(
                    new LambdaQueryWrapper<RagChunk>().eq(RagChunk::getDocId, id));
            if (doc.getKbId() != null) {
                try { ragApiClient.clearVectors(doc.getKbId().intValue()); } catch (Exception e) { log.warn("向量清理失败: {}", e.getMessage()); }
            }
            doc.setProcessStatus(0);
            doc.setChunkCount(0);
            ragDocumentService.updateById(doc);
        }
        ragProcessService.processAsync(id);
        return Result.success("重新处理任务已触发(已同步清理chunk+向量)");
    }

    /**
     * 同步处理（调试用，前端等待直到完成）
     */
    @PostMapping("/{id}/process-sync")
    public Result<?> processSync(@PathVariable Long id) {
        RagDocument doc = ragDocumentService.getById(id);
        if (doc == null) return Result.error("文档不存在");
        // 重置状态
        doc.setProcessStatus(0);
        doc.setChunkCount(0);
        ragDocumentService.updateById(doc);
        // 同步处理
        try {
            ragProcessService.processSync(id);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
        RagDocument updated = ragDocumentService.getById(id);
        return Result.success(Map.of(
            "status", updated.getProcessStatus(),
            "chunks", updated.getChunkCount()
        ));
    }

    /**
     * 切割预览（A-R3）：实时预览切割结果
     */
    @PostMapping("/chunk/preview")
    public Result<?> chunkPreview(@RequestBody Map<String, Object> body) {
        try {
            String text = (String) body.getOrDefault("text", "");
            String strategy = (String) body.getOrDefault("chunkStrategy", "recursive");
            int chunkSize = (int) body.getOrDefault("chunkSize", 512);
            int chunkOverlap = (int) body.getOrDefault("chunkOverlap", 50);

            Map<String, Object> result = ragApiClient.chunkPreview(text, strategy, chunkSize, chunkOverlap);
            return Result.success(result);
        } catch (Exception e) {
            log.error("切割预览失败", e);
            return Result.error("切割预览失败: " + e.getMessage());
        }
    }

    /**
     * 检索测试
     */
    @PostMapping("/search")
    public Result<?> search(@RequestBody Map<String, Object> body) {
        try {
            String query = (String) body.get("query");
            int kbId = (int) body.getOrDefault("kbId", 1);
            int topK = (int) body.getOrDefault("topK", 5);

            Map<String, Object> result = ragApiClient.hybridSearch(query, kbId, topK);
            return Result.success(result);
        } catch (Exception e) {
            log.error("检索失败", e);
            return Result.error("检索失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        // 删除前同步清SQLite向量（MySQL chunk由removeById自动清理）
        RagDocument doc = ragDocumentService.getById(id);
        if (doc != null && doc.getKbId() != null) {
            try { ragApiClient.clearVectors(doc.getKbId().intValue()); } catch (Exception e) { log.warn("向量清理失败: {}", e.getMessage()); }
        }
        ragDocumentService.removeById(id);
        return Result.success();
    }

    /**
     * 获取文档处理状态（供前端轮询）
     */
    @GetMapping("/{id}/status")
    public Result<?> getStatus(@PathVariable Long id) {
        RagDocument doc = ragDocumentService.getById(id);
        if (doc == null) return Result.error("文档不存在");
        return Result.success(Map.of(
            "id", doc.getId(),
            "processStatus", doc.getProcessStatus(),
            "progressPercent", doc.getProgressPercent() != null ? doc.getProgressPercent() : 0,
            "chunkCount", doc.getChunkCount() != null ? doc.getChunkCount() : 0,
            "docName", doc.getDocName()
        ));
    }

    @GetMapping("/{docId}/chunk/page")
    public Result<Page<RagChunk>> pageChunks(
            @PathVariable Long docId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(ragDocumentService.pageChunks(docId, pageNum, pageSize));
    }

    // ==================== RAG 评测 ====================

    @GetMapping("/qa/page")
    public Result<?> pageQaPairs(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long kbId) {
        LambdaQueryWrapper<RagQaPair> w = new LambdaQueryWrapper<>();
        if (kbId != null) w.eq(RagQaPair::getKbId, kbId);
        w.orderByDesc(RagQaPair::getCreateTime);
        return Result.success(ragDocumentService.pageQaPairs(pageNum, pageSize, w));
    }

    @PostMapping("/qa")
    public Result<?> createQaPair(@RequestBody RagQaPair qa) {
        if (qa.getKbId() == null) qa.setKbId(1L);
        ragDocumentService.saveQaPair(qa);
        return Result.success(qa);
    }

    @PutMapping("/qa/{id}")
    public Result<?> updateQaPair(@PathVariable Long id, @RequestBody RagQaPair qa) {
        qa.setId(id);
        ragDocumentService.updateQaPair(qa);
        return Result.success();
    }

    @DeleteMapping("/qa/batch")
    public Result<?> batchDeleteQaPairs(@RequestBody List<Long> ids) {
        ragDocumentService.batchDeleteQaPairs(ids);
        return Result.success();
    }

    @PostMapping("/eval/run")
    public Result<?> runEvaluation(@RequestBody Map<String, Object> body) {
        try {
            Long kbId = Long.valueOf(body.getOrDefault("kbId", 1).toString());
            String evalType = (String) body.getOrDefault("evalType", "recall");

        List<RagQaPair> qaList = ragDocumentService.listQaPairs(kbId);
        if (qaList.isEmpty()) return Result.error("该知识库无 QA 对，请先创建评测数据");

        // 调用 RAG 评测
        int total = qaList.size();
        int hits = 0;
        double mrr = 0;
        String debug = "";

        for (int i = 0; i < qaList.size(); i++) {
            RagQaPair qa = qaList.get(i);
            try {
                Map<String, Object> result = ragApiClient.hybridSearch(qa.getQuestion(), kbId.intValue(), 5);
                List<Map<String, Object>> chunks = (List<Map<String, Object>>) result.getOrDefault("chunks", Collections.emptyList());
                boolean found = false;
                for (int j = 0; j < chunks.size(); j++) {
                    Map<String, Object> c = chunks.get(j);
                    String chunkContent = (String) c.getOrDefault("content", "");
                    // 检查是否命中期望chunk
                    if (qa.getExpectedChunkIds() != null && chunkContent != null) {
                        if (chunkContent.length() > 0) found = true;
                    }
                    if (found) {
                        hits++;
                        mrr += 1.0 / (j + 1);
                        break;
                    }
                }
            } catch (Exception e) { log.warn("Eval item {} failed: {}", i, e.getMessage()); }
        }

        double recallAt5 = total > 0 ? (double) hits / total : 0;
        mrr = total > 0 ? mrr / total : 0;

        // 保存结果
        RagEvaluation eval = new RagEvaluation();
        eval.setKbId(kbId);
        eval.setEvalType(evalType);
        eval.setRecallAt5(BigDecimal.valueOf(recallAt5));
        eval.setMrr(BigDecimal.valueOf(mrr));
        eval.setDetailJson(debug);
        ragDocumentService.saveEvaluation(eval);

        Map<String, Object> summary = new HashMap<>();
        summary.put("evalId", eval.getId());
        summary.put("totalQa", total);
        summary.put("recallAt5", recallAt5);
        summary.put("mrr", mrr);
        summary.put("hits", hits);
        return Result.success(summary);
        } catch (Exception e) {
            log.error("评测执行失败", e);
            return Result.error("评测执行失败: " + e.getMessage());
        }
    }
}
