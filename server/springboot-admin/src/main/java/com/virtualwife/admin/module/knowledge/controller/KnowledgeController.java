package com.virtualwife.admin.module.knowledge.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.virtualwife.admin.common.result.Result;
import com.virtualwife.admin.integration.rag.RagApiClient;
import com.virtualwife.admin.module.knowledge.entity.KnowledgeBase;
import com.virtualwife.admin.module.knowledge.entity.KnowledgeItem;
import com.virtualwife.admin.module.knowledge.service.KnowledgeBaseService;
import com.virtualwife.admin.module.knowledge.service.KnowledgeItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/kb")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeBaseService kbService;
    private final KnowledgeItemService itemService;
    private final RagApiClient ragApiClient;

    // ==================== 知识库 ====================

    @GetMapping("/page")
    public Result<Page<KnowledgeBase>> pageKb(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long scenicSpotId) {
        return Result.success(kbService.pageKb(pageNum, pageSize, keyword, scenicSpotId));
    }

    @PostMapping
    public Result<?> createKb(@RequestBody KnowledgeBase kb) {
        kbService.save(kb);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<KnowledgeBase> getKb(@PathVariable Long id) {
        return Result.success(kbService.getById(id));
    }

    @PutMapping("/{id}")
    public Result<?> updateKb(@PathVariable Long id, @RequestBody KnowledgeBase kb) {
        kb.setId(id);
        kbService.updateById(kb);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteKb(@PathVariable Long id) {
        kbService.removeById(id);
        return Result.success();
    }

    // ==================== 知识条目 ====================

    @GetMapping("/{kbId}/item/page")
    public Result<Page<KnowledgeItem>> pageItems(
            @PathVariable Long kbId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.success(itemService.pageItems(kbId, pageNum, pageSize, keyword));
    }

    @PostMapping("/{kbId}/item")
    public Result<?> createItem(@PathVariable Long kbId, @RequestBody KnowledgeItem item) {
        item.setKbId(kbId);
        item.setVectorStatus(0);
        itemService.save(item);
        return Result.success();
    }

    @PostMapping("/{kbId}/item/batch-import")
    public Result<?> batchImportItems(@PathVariable Long kbId, @RequestBody List<KnowledgeItem> items) {
        itemService.batchImport(kbId, items);
        return Result.success("导入成功");
    }

    @PutMapping("/{kbId}/item/{id}")
    public Result<?> updateItem(@PathVariable Long kbId, @PathVariable Long id, @RequestBody KnowledgeItem item) {
        item.setId(id);
        item.setKbId(kbId);
        itemService.updateById(item);
        return Result.success();
    }

    @DeleteMapping("/{kbId}/item/{id}")
    public Result<?> deleteItem(@PathVariable Long kbId, @PathVariable Long id) {
        itemService.removeById(id);
        return Result.success();
    }

    /**
     * 触发向量化（A-K3）
     * 将知识库中未向量化的条目批量发送到RAG服务进行向量化
     */
    @PostMapping("/{kbId}/vectorize")
    public Result<?> triggerVectorize(@PathVariable Long kbId) {
        try {
            List<KnowledgeItem> items = itemService.getBaseMapper().selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KnowledgeItem>()
                            .eq(KnowledgeItem::getKbId, kbId)
                            .eq(KnowledgeItem::getVectorStatus, 0)
            );

            if (items.isEmpty()) {
                return Result.success("所有条目已向量化");
            }

            // 批量提取文本并发送到RAG
            List<String> texts = items.stream()
                    .map(item -> (item.getTitle() != null ? item.getTitle() + " " : "") +
                                (item.getContent() != null ? item.getContent() : ""))
                    .collect(Collectors.toList());

            ragApiClient.batchEmbed(texts);

            // 更新向量化状态
            for (KnowledgeItem item : items) {
                item.setVectorStatus(1);
            }
            itemService.updateBatchById(items);

            return Result.success("已触发向量化，共处理 " + items.size() + " 条");
        } catch (Exception e) {
            log.error("向量化失败", e);
            return Result.error("向量化失败: " + e.getMessage());
        }
    }
}
