package com.virtualwife.admin.module.route.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.virtualwife.admin.module.route.entity.Route;
import com.virtualwife.admin.module.route.entity.Spot;
import com.virtualwife.admin.module.route.mapper.RouteMapper;
import com.virtualwife.admin.module.route.mapper.SpotMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RouteService extends ServiceImpl<RouteMapper, Route> {

    private final SpotMapper spotMapper;

    @Cacheable(value = "route", key = "'page:' + #pageNum + ':' + #pageSize + ':' + #keyword")
    public Page<Route> pageRoutes(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<Route> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) wrapper.like(Route::getRouteName, keyword);
        wrapper.orderByAsc(Route::getSortOrder);
        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    @CacheEvict(value = "route", allEntries = true)
    @Transactional
    @Override
    public boolean removeById(java.io.Serializable id) {
        spotMapper.delete(new LambdaQueryWrapper<Spot>().eq(Spot::getRouteId, id));
        return super.removeById(id);
    }

    @CacheEvict(value = "route", allEntries = true)
    @Override
    public boolean save(Route entity) {
        return super.save(entity);
    }

    @CacheEvict(value = "route", allEntries = true)
    @Override
    public boolean updateById(Route entity) {
        return super.updateById(entity);
    }
}
