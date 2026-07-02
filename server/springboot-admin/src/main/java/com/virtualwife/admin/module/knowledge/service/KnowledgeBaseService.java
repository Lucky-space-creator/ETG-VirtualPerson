package com.virtualwife.admin.module.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.virtualwife.admin.module.knowledge.entity.KnowledgeBase;
import com.virtualwife.admin.module.knowledge.entity.KnowledgeItem;
import com.virtualwife.admin.module.knowledge.mapper.KnowledgeBaseMapper;
import com.virtualwife.admin.module.knowledge.mapper.KnowledgeItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseService extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase> {

    private final KnowledgeItemMapper knowledgeItemMapper;

    @Cacheable(value = "knowledge", key = "'page:' + #pageNum + ':' + #pageSize + ':' + #keyword")
    public Page<KnowledgeBase> pageKb(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) wrapper.like(KnowledgeBase::getKbName, keyword);
        wrapper.orderByDesc(KnowledgeBase::getCreateTime);
        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    @CacheEvict(value = "knowledge", allEntries = true)
    @Transactional
    @Override
    public boolean removeById(java.io.Serializable id) {
        knowledgeItemMapper.delete(new LambdaQueryWrapper<KnowledgeItem>().eq(KnowledgeItem::getKbId, id));
        return super.removeById(id);
    }

    @CacheEvict(value = "knowledge", allEntries = true)
    @Override
    public boolean save(KnowledgeBase entity) {
        return super.save(entity);
    }

    @CacheEvict(value = "knowledge", allEntries = true)
    @Override
    public boolean updateById(KnowledgeBase entity) {
        return super.updateById(entity);
    }
}
