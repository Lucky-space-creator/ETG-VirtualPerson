package com.virtualwife.admin.module.chat.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.virtualwife.admin.common.result.Result;
import com.virtualwife.admin.module.chat.entity.ChatRecord;
import com.virtualwife.admin.module.chat.service.ChatRecordService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatRecordController {

    private final ChatRecordService chatRecordService;

    @GetMapping("/page")
    public Result<Page<ChatRecord>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String avatarName,
            @RequestParam(required = false) String messageType,
            @RequestParam(required = false) String emotion,
            @RequestParam(required = false) Long scenicSpotId,
            @RequestParam(required = false) String keyword) {
        return Result.success(chatRecordService.pageRecords(pageNum, pageSize, userId, avatarName, messageType, emotion, keyword, scenicSpotId));
    }

    @GetMapping("/{id}")
    public Result<ChatRecord> getById(@PathVariable Long id) {
        return Result.success(chatRecordService.getById(id));
    }

    @GetMapping("/session/{sessionId}")
    public Result<List<ChatRecord>> getSession(@PathVariable String sessionId) {
        return Result.success(chatRecordService.getSessionRecords(sessionId));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        chatRecordService.removeById(id);
        return Result.success();
    }

    @DeleteMapping("/batch")
    public Result<?> batchDelete(@RequestBody List<Long> ids) {
        chatRecordService.removeByIds(ids);
        return Result.success();
    }

    @GetMapping("/export")
    public void export(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String avatarName,
            @RequestParam(required = false) String messageType,
            @RequestParam(required = false) String emotion,
            @RequestParam(required = false) String keyword,
            HttpServletResponse response) throws Exception {
        List<ChatRecord> records = chatRecordService.getExportRecords(userId, avatarName, messageType, emotion, keyword);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("聊天记录导出", StandardCharsets.UTF_8).replace("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), ChatRecord.class).sheet("聊天记录").doWrite(records);
    }
}
