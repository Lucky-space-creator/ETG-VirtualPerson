package com.virtualwife.admin.module.scenic.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.virtualwife.admin.module.scenic.entity.ScenicSpot;
import com.virtualwife.admin.module.scenic.mapper.ScenicSpotMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScenicSpotService extends ServiceImpl<ScenicSpotMapper, ScenicSpot> {

    @Cacheable(value = "scenic", key = "'all'")
    public List<ScenicSpot> listEnabled() {
        return list(new LambdaQueryWrapper<ScenicSpot>()
                .eq(ScenicSpot::getStatus, 1)
                .orderByAsc(ScenicSpot::getId));
    }

    @CacheEvict(value = "scenic", allEntries = true)
    @Override
    public boolean save(ScenicSpot entity) {
        return super.save(entity);
    }

    @CacheEvict(value = "scenic", allEntries = true)
    @Override
    public boolean updateById(ScenicSpot entity) {
        return super.updateById(entity);
    }
}
