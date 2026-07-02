package com.virtualwife.admin.module.rag.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.virtualwife.admin.common.exception.BusinessException;
import com.virtualwife.admin.common.util.MinioUtil;
import com.virtualwife.admin.module.rag.entity.RagDocument;
import com.virtualwife.admin.module.rag.entity.RagChunk;
import com.virtualwife.admin.module.rag.entity.RagQaPair;
import com.virtualwife.admin.module.rag.entity.RagEvaluation;
import com.virtualwife.admin.module.rag.mapper.RagChunkMapper;
import com.virtualwife.admin.module.rag.mapper.RagDocumentMapper;
import com.virtualwife.admin.module.rag.mapper.RagQaPairMapper;
import com.virtualwife.admin.module.rag.mapper.RagEvaluationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagDocumentService extends ServiceImpl<RagDocumentMapper, RagDocument> {

    private final MinioUtil minioUtil;
    private final RagChunkMapper ragChunkMapper;
    private final RagQaPairMapper ragQaPairMapper;
    private final RagEvaluationMapper ragEvaluationMapper;

    /** 获取Chunk Mapper（供Controller直接用） */
    public RagChunkMapper getChunkMapper() {
        return ragChunkMapper;
    }

    public Page<RagDocument> pageDocs(int pageNum, int pageSize, Long kbId, Integer processStatus, String keyword) {
        LambdaQueryWrapper<RagDocument> wrapper = new LambdaQueryWrapper<>();
        if (kbId != null) wrapper.eq(RagDocument::getKbId, kbId);
        if (processStatus != null) wrapper.eq(RagDocument::getProcessStatus, processStatus);
        if (keyword != null && !keyword.isBlank()) wrapper.like(RagDocument::getDocName, keyword);
        wrapper.orderByDesc(RagDocument::getCreatedAt);
        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Transactional
    public RagDocument uploadDocument(MultipartFile file, Long kbId, String chunkStrategy, Integer chunkSize, Integer chunkOverlap) throws Exception {
        String originalName = file.getOriginalFilename();
        String docType = "";
        if (originalName != null && originalName.contains(".")) {
            docType = originalName.substring(originalName.lastIndexOf(".") + 1).toUpperCase();
        }

        // 计算SHA-256去重
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        String sha256 = bytesToHex(md.digest(file.getBytes()));

        // 上传到MinIO（失败则存本地）
        String filePath;
        try {
            filePath = minioUtil.uploadFile(file, "rag/documents");
        } catch (Exception e) {
            log.warn("MinIO上传失败，使用本地存储: {}", e.getMessage());
            File localDir = new File(System.getProperty("java.io.tmpdir"), "rag_uploads");
            localDir.mkdirs();
            File localFile = new File(localDir, UUID.randomUUID() + "_" + originalName);
            try (FileOutputStream fos = new FileOutputStream(localFile)) {
                fos.write(file.getBytes());
            }
            filePath = "local:" + localFile.getAbsolutePath();
        }

        RagDocument doc = new RagDocument();
        doc.setKbId(kbId);
        doc.setDocName(originalName);
        doc.setDocType(docType);
        doc.setFilePath(filePath);
        doc.setFileSize(file.getSize());
        doc.setSha256(sha256);
        doc.setProcessStatus(0);
        doc.setChunkStrategy(chunkStrategy);
        doc.setChunkSize(chunkSize != null ? chunkSize : 512);
        doc.setChunkOverlap(chunkOverlap != null ? chunkOverlap : 50);
        this.save(doc);
        return doc;
    }

    @Transactional
    public void triggerProcess(Long id) {
        RagDocument doc = this.getById(id);
        if (doc == null) throw new BusinessException(404, "文档不存在");
        if (doc.getProcessStatus() == 1 || doc.getProcessStatus() == 2 || doc.getProcessStatus() == 3) {
            throw new BusinessException("文档正在处理中");
        }
        doc.setProcessStatus(1); // 解析中
        this.updateById(doc);
        // TODO: 调用RAG嵌入服务触发完整处理流水线
    }

    @Transactional
    @Override
    public boolean removeById(java.io.Serializable id) {
        // 删除关联的chunk
        ragChunkMapper.delete(new LambdaQueryWrapper<RagChunk>().eq(RagChunk::getDocId, id));
        return super.removeById(id);
    }

    public Page<RagChunk> pageChunks(Long docId, int pageNum, int pageSize) {
        LambdaQueryWrapper<RagChunk> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RagChunk::getDocId, docId).orderByAsc(RagChunk::getChunkIndex);
        Page<RagChunk> page = new Page<>(pageNum, pageSize);
        return ragChunkMapper.selectPage(page, wrapper);
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    // ==================== QA对 / 评测 ====================

    public Page<RagQaPair> pageQaPairs(int pageNum, int pageSize, LambdaQueryWrapper<RagQaPair> wrapper) {
        return ragQaPairMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    public void saveQaPair(RagQaPair qa) { ragQaPairMapper.insert(qa); }

    public void updateQaPair(RagQaPair qa) { ragQaPairMapper.updateById(qa); }

    public void batchDeleteQaPairs(List<Long> ids) { ragQaPairMapper.deleteBatchIds(ids); }

    public List<RagQaPair> listQaPairs(Long kbId) {
        LambdaQueryWrapper<RagQaPair> w = new LambdaQueryWrapper<>();
        if (kbId != null) w.eq(RagQaPair::getKbId, kbId);
        return ragQaPairMapper.selectList(w);
    }

    public void saveEvaluation(RagEvaluation eval) { ragEvaluationMapper.insert(eval); }
}
