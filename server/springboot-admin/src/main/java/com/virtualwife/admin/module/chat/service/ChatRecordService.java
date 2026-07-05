package com.virtualwife.admin.module.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.virtualwife.admin.module.chat.entity.ChatRecord;
import com.virtualwife.admin.module.chat.mapper.ChatRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatRecordService extends ServiceImpl<ChatRecordMapper, ChatRecord> {

    @Cacheable(value = "chatRecord", key = "'page:' + #pageNum + ':' + #pageSize")
    public Page<ChatRecord> pageRecords(int pageNum, int pageSize, Long userId, String avatarName,
                                        String messageType, String emotion, String keyword, Long scenicSpotId) {
        LambdaQueryWrapper<ChatRecord> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) wrapper.eq(ChatRecord::getUserId, userId);
        if (scenicSpotId != null) wrapper.eq(ChatRecord::getScenicSpotId, scenicSpotId);
        if (avatarName != null && !avatarName.isBlank()) wrapper.eq(ChatRecord::getAvatarName, avatarName);
        if (messageType != null && !messageType.isBlank()) wrapper.eq(ChatRecord::getMessageType, messageType);
        if (emotion != null && !emotion.isBlank()) wrapper.eq(ChatRecord::getEmotion, emotion);
        if (keyword != null && !keyword.isBlank()) wrapper.like(ChatRecord::getContent, keyword);
        wrapper.orderByDesc(ChatRecord::getCreateTime);
        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    public List<ChatRecord> getSessionRecords(String sessionId) {
        return this.list(new LambdaQueryWrapper<ChatRecord>()
                .eq(ChatRecord::getSessionId, sessionId)
                .orderByAsc(ChatRecord::getCreateTime));
    }

    public List<ChatRecord> getExportRecords(Long userId, String avatarName, String messageType,
                                              String emotion, String keyword) {
        LambdaQueryWrapper<ChatRecord> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) wrapper.eq(ChatRecord::getUserId, userId);
        if (avatarName != null && !avatarName.isBlank()) wrapper.eq(ChatRecord::getAvatarName, avatarName);
        if (messageType != null && !messageType.isBlank()) wrapper.eq(ChatRecord::getMessageType, messageType);
        if (emotion != null && !emotion.isBlank()) wrapper.eq(ChatRecord::getEmotion, emotion);
        if (keyword != null && !keyword.isBlank()) wrapper.like(ChatRecord::getContent, keyword);
        wrapper.orderByDesc(ChatRecord::getCreateTime);
        return this.list(wrapper);
    }

    @CacheEvict(value = "chatRecord", allEntries = true)
    public void evictCache() {}
}
