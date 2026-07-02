package com.virtualwife.admin.module.speech.controller;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.virtualwife.admin.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * TTS语音合成接口 - 供Android端调用
 * 使用Edge-TTS (免费微软语音合成)
 */
@Slf4j
@RestController
@RequestMapping("/speech/tts")
@RequiredArgsConstructor
public class TtsController {

    @Value("${virtualwife.tts.edge-tts-path:}")
    private String edgeTtsPath;

    @Value("${virtualwife.tts.output-dir:${java.io.tmpdir}/tts}")
    private String outputDir;

    /**
     * 生成TTS音频
     * POST /api/admin/speech/tts/generate
     * 使用手动读取请求体避免UTF-8编码问题
     */
    @PostMapping("/generate")
    public Result<Map<String, Object>> generateTts(HttpServletRequest httpRequest) {
        try {
            // 手动读取请求体
            byte[] bytes = httpRequest.getInputStream().readAllBytes();

            // 尝试UTF-8解码，如果出现乱码则使用GBK
            String body = new String(bytes, StandardCharsets.UTF_8);
            if (body.contains("�") || body.contains("?")) {
                // UTF-8解码失败，尝试GBK
                body = new String(bytes, java.nio.charset.Charset.forName("GBK"));
                log.info("TTS: 使用GBK编码解码");
            }
            log.info("TTS body: {}", body);
            JSONObject json = JSONUtil.parseObj(body);

            String text = json.getStr("text");
            String voiceId = json.getStr("voiceId", "zh-CN-XiaoyiNeural");
            log.info("TTS request: voiceId={}, text={}", voiceId, text != null ? text.substring(0, Math.min(30, text.length())) : "null");

            if (text == null || text.isBlank()) {
                return Result.badRequest("文本不能为空");
            }

            // 尝试使用edge-tts生成音频
            String audioBase64 = generateWithEdgeTts(text, voiceId);

            Map<String, Object> data = new HashMap<>();
            if (audioBase64 != null) {
                data.put("audio_base64", audioBase64);
                data.put("status", "ok");
            } else {
                data.put("status", "fallback");
                data.put("message", "TTS服务暂不可用，请使用本地TTS");
            }

            return Result.success(data);
        } catch (Exception e) {
            log.error("TTS生成失败: {}", e.getMessage());
            Map<String, Object> data = new HashMap<>();
            data.put("status", "error");
            data.put("error", e.getMessage());
            return Result.success(data);
        }
    }

    /**
     * 获取可用语音列表
     * GET /api/admin/speech/tts/voices
     */
    @GetMapping("/voices")
    public Result<List<Map<String, String>>> getVoices() {
        List<Map<String, String>> voices = new ArrayList<>();
        addVoice(voices, "zh-CN-XiaoyiNeural", "小艺 (女声)", "zh-CN");
        addVoice(voices, "zh-CN-YunxiNeural", "云希 (男声)", "zh-CN");
        addVoice(voices, "zh-CN-XiaoxiaoNeural", "小晓 (女声)", "zh-CN");
        addVoice(voices, "zh-CN-YunjianNeural", "云健 (男声)", "zh-CN");
        addVoice(voices, "zh-CN-XiaochenNeural", "小辰 (女声)", "zh-CN");
        addVoice(voices, "zh-CN-YunzeNeural", "云泽 (男声)", "zh-CN");
        return Result.success(voices);
    }

    private void addVoice(List<Map<String, String>> voices, String id, String name, String locale) {
        Map<String, String> v = new HashMap<>();
        v.put("id", id);
        v.put("name", name);
        v.put("locale", locale);
        voices.add(v);
    }

    /**
     * 使用edge-tts生成音频
     */
    private String generateWithEdgeTts(String text, String voiceId) {
        try {
            // 检查edge-tts是否可用
            ProcessBuilder checkPb = new ProcessBuilder("edge-tts", "--version");
            checkPb.redirectErrorStream(true);
            Process checkProcess = checkPb.start();
            int checkExit = checkProcess.waitFor();
            if (checkExit != 0) {
                log.warn("edge-tts未安装，退出码: {}", checkExit);
                return null;
            }
            log.info("edge-tts可用，开始生成音频");

            // 生成临时文件
            File outDir = new File(outputDir);
            if (!outDir.exists()) outDir.mkdirs();
            String fileName = "tts_" + System.currentTimeMillis() + ".mp3";
            File outputFile = new File(outDir, fileName);

            // 调用edge-tts
            ProcessBuilder pb = new ProcessBuilder(
                    "edge-tts",
                    "--voice", voiceId,
                    "--text", text,
                    "--write-media", outputFile.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            log.info("edge-tts退出码: {}, 输出: {}", exitCode, output.toString().trim());

            if (exitCode == 0 && outputFile.exists() && outputFile.length() > 0) {
                // 读取为Base64
                byte[] audioBytes = Files.readAllBytes(outputFile.toPath());
                String base64 = Base64.getEncoder().encodeToString(audioBytes);
                log.info("TTS生成成功，音频大小: {} bytes", audioBytes.length);
                outputFile.delete();
                return base64;
            }

            log.warn("TTS生成失败，退出码: {}, 文件存在: {}, 文件大小: {}",
                    exitCode, outputFile.exists(), outputFile.exists() ? outputFile.length() : 0);
            outputFile.delete();
            return null;
        } catch (Exception e) {
            log.error("edge-tts调用失败: {}", e.getMessage(), e);
            return null;
        }
    }

    @Data
    public static class TtsRequest {
        private String text;
        private String voiceId;
        private String type;
    }
}
