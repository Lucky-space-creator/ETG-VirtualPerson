package com.virtualwife.admin.module.avatar.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.virtualwife.admin.module.avatar.entity.AvatarConfig;
import com.virtualwife.admin.module.avatar.mapper.AvatarConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AvatarConfigService extends ServiceImpl<AvatarConfigMapper, AvatarConfig> {

    public Page<AvatarConfig> pageAvatars(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<AvatarConfig> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) wrapper.like(AvatarConfig::getAvatarName, keyword);
        wrapper.orderByAsc(AvatarConfig::getSortOrder);
        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Cacheable(value = "avatarConfig", key = "#id")
    @Override
    public AvatarConfig getById(java.io.Serializable id) {
        return super.getById(id);
    }

    @CacheEvict(value = "avatarConfig", allEntries = true)
    @Override
    public boolean save(AvatarConfig entity) {
        return super.save(entity);
    }

    @CacheEvict(value = "avatarConfig", allEntries = true)
    @Override
    public boolean updateById(AvatarConfig entity) {
        return super.updateById(entity);
    }

    @CacheEvict(value = "avatarConfig", allEntries = true)
    @Override
    public boolean removeById(java.io.Serializable id) {
        return super.removeById(id);
    }

    /**
     * 设为默认形象（清除其他默认）
     */
    @CacheEvict(value = "avatarConfig", allEntries = true)
    public void setDefault(Long id) {
        // 清除所有默认
        LambdaUpdateWrapper<AvatarConfig> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(AvatarConfig::getIsDefault, 0);
        this.update(updateWrapper);
        // 设置当前为默认
        AvatarConfig avatar = new AvatarConfig();
        avatar.setId(id);
        avatar.setIsDefault(1);
        this.updateById(avatar);
    }

    /**
     * 获取默认形象（带缓存）
     */
    @Cacheable(value = "avatarConfig", key = "'default'")
    public AvatarConfig getDefault() {
        AvatarConfig avatar = this.getOne(
                new LambdaQueryWrapper<AvatarConfig>()
                        .eq(AvatarConfig::getIsDefault, 1)
                        .last("LIMIT 1")
        );
        // 如果没有默认，取第一个
        if (avatar == null) {
            avatar = this.getOne(
                    new LambdaQueryWrapper<AvatarConfig>()
                            .orderByAsc(AvatarConfig::getSortOrder)
                            .last("LIMIT 1")
            );
        }
        return avatar;
    }
}
