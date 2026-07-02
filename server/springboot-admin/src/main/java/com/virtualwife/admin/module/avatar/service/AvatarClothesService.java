package com.virtualwife.admin.module.avatar.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.virtualwife.admin.common.util.MinioUtil;
import com.virtualwife.admin.module.avatar.entity.AvatarClothes;
import com.virtualwife.admin.module.avatar.mapper.AvatarClothesMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 数字人衣服服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AvatarClothesService extends ServiceImpl<AvatarClothesMapper, AvatarClothes> {

    private final MinioUtil minioUtil;

    /**
     * 获取指定数字人的衣服列表
     */
    public List<AvatarClothes> getClothesByAvatarId(Long avatarId) {
        List<AvatarClothes> clothes = list(
                new LambdaQueryWrapper<AvatarClothes>()
                        .eq(AvatarClothes::getAvatarId, avatarId)
                        .orderByAsc(AvatarClothes::getSortOrder)
        );
        // 填充显示URL
        clothes.forEach(this::fillDisplayUrls);
        return clothes;
    }

    /**
     * 获取默认衣服
     */
    public AvatarClothes getDefaultClothes(Long avatarId) {
        AvatarClothes clothes = getOne(
                new LambdaQueryWrapper<AvatarClothes>()
                        .eq(AvatarClothes::getAvatarId, avatarId)
                        .eq(AvatarClothes::getIsDefault, 1)
                        .last("LIMIT 1")
        );
        if (clothes == null) {
            // 如果没有默认衣服，返回第一件
            clothes = getOne(
                    new LambdaQueryWrapper<AvatarClothes>()
                            .eq(AvatarClothes::getAvatarId, avatarId)
                            .orderByAsc(AvatarClothes::getSortOrder)
                            .last("LIMIT 1")
            );
        }
        if (clothes != null) {
            fillDisplayUrls(clothes);
        }
        return clothes;
    }

    /**
     * 设置默认衣服
     */
    public void setDefault(Long clothesId) {
        AvatarClothes clothes = getById(clothesId);
        if (clothes == null) return;

        // 取消同数字人下的其他默认衣服
        update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AvatarClothes>()
                .eq(AvatarClothes::getAvatarId, clothes.getAvatarId())
                .eq(AvatarClothes::getIsDefault, 1)
                .set(AvatarClothes::getIsDefault, 0)
        );

        // 设置当前衣服为默认
        clothes.setIsDefault(1);
        updateById(clothes);
    }

    /**
     * 填充显示URL
     */
    private void fillDisplayUrls(AvatarClothes clothes) {
        if (clothes == null) return;
        clothes.setThumbnailDisplayUrl(minioUtil.getPresignedUrl(clothes.getThumbnailUrl()));
        clothes.setVrmDisplayUrl(minioUtil.getPresignedUrl(clothes.getVrmModelUrl()));
    }
}
