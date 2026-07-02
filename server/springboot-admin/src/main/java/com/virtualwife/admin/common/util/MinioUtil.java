package com.virtualwife.admin.common.util;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioUtil {

    private final MinioClient minioClient;
    private final com.virtualwife.admin.common.config.MinioConfig minioConfig;

    /**
     * 上传文件到MinIO
     */
    public String uploadFile(MultipartFile file, String folder) throws Exception {
        String bucket = minioConfig.getBucket();
        ensureBucket(bucket);

        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        String objectName = folder + "/" + UUID.randomUUID().toString().replace("-", "") + extension;

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );

        return objectName;
    }

    /**
     * 获取文件的访问URL（公开bucket直接拼接URL，无需签名）
     * 默认使用endpoint（localhost），Android端通过外部接口获取10.0.2.2地址
     */
    public String getPresignedUrl(String objectName) {
        return getPresignedUrl(objectName, false);
    }

    /**
     * 获取文件的访问URL
     * @param objectName 对象路径
     * @param forMobile true=返回Android模拟器地址(10.0.2.2)，false=返回localhost地址
     */
    public String getPresignedUrl(String objectName, boolean forMobile) {
        try {
            String base;
            if (forMobile) {
                String externalEndpoint = minioConfig.getExternalEndpoint();
                base = (externalEndpoint != null && !externalEndpoint.isBlank())
                        ? externalEndpoint : minioConfig.getEndpoint();
            } else {
                base = minioConfig.getEndpoint();
            }
            if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
            return base + "/" + minioConfig.getBucket() + "/" + objectName;
        } catch (Exception e) {
            log.error("获取文件URL失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 设置bucket为公开读取策略
     */
    public void setBucketPublicRead() {
        try {
            String bucket = minioConfig.getBucket();
            String policy = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:GetObject\",\"s3:ListBucket\"],\"Resource\":[\"arn:aws:s3:::" + bucket + "/*\",\"arn:aws:s3:::" + bucket + "\"]}]}";
            minioClient.setBucketPolicy(
                    io.minio.SetBucketPolicyArgs.builder()
                            .bucket(bucket)
                            .config(policy)
                            .build()
            );
            log.info("MinIO bucket已设置为公开读取: {}", bucket);
        } catch (Exception e) {
            log.warn("设置bucket策略失败: {}", e.getMessage());
        }
    }

    /**
     * 删除文件
     */
    public void deleteFile(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucket())
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            log.error("删除文件失败: {}", e.getMessage());
        }
    }

    /**
     * 下载文件内容为字节数组
     */
    public byte[] downloadFile(String objectName) {
        try {
            try (InputStream is = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioConfig.getBucket())
                            .object(objectName)
                            .build());
                 ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) > 0) {
                    bos.write(buf, 0, n);
                }
                return bos.toByteArray();
            }
        } catch (Exception e) {
            log.error("下载文件失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 确保Bucket存在
     */
    private void ensureBucket(String bucket) throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucket).build()
        );
        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket(bucket).build()
            );
        }
    }
}
