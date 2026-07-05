package com.virtualwife.admin.module.scenic.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.virtualwife.admin.module.scenic.entity.ScenicSpot;
import com.virtualwife.admin.module.scenic.mapper.ScenicSpotMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScenicSpotService extends ServiceImpl<ScenicSpotMapper, ScenicSpot> {

    public List<ScenicSpot> listEnabled() {
        return list(new LambdaQueryWrapper<ScenicSpot>()
                .eq(ScenicSpot::getStatus, 1)
                .orderByAsc(ScenicSpot::getId));
    }
}
