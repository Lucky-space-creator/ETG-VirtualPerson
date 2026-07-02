package com.virtualwife.admin.module.route.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.virtualwife.admin.module.route.entity.Spot;
import com.virtualwife.admin.module.route.mapper.SpotMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class SpotService extends ServiceImpl<SpotMapper, Spot> {

    @Cacheable(value = "spot", key = "#routeId + ':' + #pageNum + ':' + #pageSize")
    public Page<Spot> pageSpots(Long routeId, int pageNum, int pageSize) {
        LambdaQueryWrapper<Spot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Spot::getRouteId, routeId).orderByAsc(Spot::getSpotOrder);
        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    @CacheEvict(value = "spot", allEntries = true)
    @Override
    public boolean save(Spot entity) {
        return super.save(entity);
    }

    @CacheEvict(value = "spot", allEntries = true)
    @Override
    public boolean updateById(Spot entity) {
        return super.updateById(entity);
    }

    @CacheEvict(value = "spot", allEntries = true)
    @Override
    public boolean removeById(java.io.Serializable id) {
        return super.removeById(id);
    }
}
