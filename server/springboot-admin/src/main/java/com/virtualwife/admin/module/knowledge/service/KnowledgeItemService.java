package com.virtualwife.admin.module.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.virtualwife.admin.module.knowledge.entity.KnowledgeItem;
import com.virtualwife.admin.module.knowledge.mapper.KnowledgeItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeItemService extends ServiceImpl<KnowledgeItemMapper, KnowledgeItem> {

    @Cacheable(value = "knowledgeItem", key = "#kbId + ':' + #pageNum + ':' + #pageSize")
    public Page<KnowledgeItem> pageItems(Long kbId, int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<KnowledgeItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeItem::getKbId, kbId);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(KnowledgeItem::getTitle, keyword).or().like(KnowledgeItem::getContent, keyword));
        }
        wrapper.orderByDesc(KnowledgeItem::getCreateTime);
        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    @CacheEvict(value = "knowledgeItem", allEntries = true)
    public void batchImport(Long kbId, List<KnowledgeItem> items) {
        for (KnowledgeItem item : items) {
            item.setKbId(kbId);
            item.setVectorStatus(0);
        }
        this.saveBatch(items);
    }

    @CacheEvict(value = "knowledgeItem", allEntries = true)
    @Override
    public boolean save(KnowledgeItem entity) {
        return super.save(entity);
    }

    @CacheEvict(value = "knowledgeItem", allEntries = true)
    @Override
    public boolean updateById(KnowledgeItem entity) {
        return super.updateById(entity);
    }

    @CacheEvict(value = "knowledgeItem", allEntries = true)
    @Override
    public boolean removeById(java.io.Serializable id) {
        return super.removeById(id);
    }
}
