package com.etread.utils;

import io.minio.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class MinioUtil {
    @Value("${minio.endpoint}") private String endpoint;
    @Value("${minio.accessKey}") private String accessKey;
    @Value("${minio.secretKey}") private String secretKey;

    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    public String uploadFile(MultipartFile file, String bucket) throws Exception {
        String fileName = UUID.randomUUID().toString().replace("-", "") + "-" + file.getOriginalFilename();
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucket)
                .object(fileName)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build());
        return endpoint + "/" + bucket + "/" + fileName;
    }
    public String uploadBytes(byte[] data, String objectName, String contentType, String bucket) throws Exception {
        try (InputStream is = new ByteArrayInputStream(data)) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket) // 使用传入的 bucket
                    .object(objectName)
                    .stream(is, data.length, -1)
                    .contentType(contentType)
                    .build());
            return endpoint + "/" + bucket + "/" + objectName;
        }
    }
    public void removeObject(String objectName, String bucket) throws Exception {
        minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .build());
    }
    public InputStream getObjectStream(String bucket, String objectName) throws Exception {
        return minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .build());
    }

    /**
     * 2. 辅助下载方法：下载到本地临时文件
     * 适合 TXT：RandomAccessFile 需要本地物理文件
     */
    public File downloadToTemp(String bucket, String objectName) throws Exception {
        // 在系统临时目录创建一个文件
        File tempFile = File.createTempFile("etread-download-", ".tmp");

        try (InputStream is = getObjectStream(bucket, objectName);
             FileOutputStream fos = new FileOutputStream(tempFile)) {

            // 使用 Apache Commons IO 的 IOUtils.copy(is, fos); 或者手动流拷贝
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
        return tempFile;
    }
    public void removeFolder(String bucket, String folderPrefix) throws Exception {
        // 1. 🌟 安全第一：确保前缀以 '/' 结尾！
        if (!folderPrefix.endsWith("/")) {
            folderPrefix += "/";
        }

        log.info("准备清空 MinIO 文件夹: {}/{}", bucket, folderPrefix);

        // 2. 查出这个前缀下的所有文件 (recursive=true 表示包括子文件夹里的文件)
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucket)
                        .prefix(folderPrefix)
                        .recursive(true)
                        .build()
        );

        // 3. 把它们打包成一个个“死亡名单”
        List<DeleteObject> deleteObjects = new LinkedList<>();
        for (Result<Item> result : results) {
            Item item = result.get();
            deleteObjects.add(new DeleteObject(item.objectName()));
        }

        // 如果里面本来就是空的，就直接收工回家啦
        if (deleteObjects.isEmpty()) {
            log.info("文件夹本来就是空的，不需要打扫哦！");
            return;
        }

        // 4. 批量执行“死刑” (这比一次次调 removeObject 快 100 倍！)
        Iterable<Result<DeleteError>> errors = minioClient.removeObjects(
                RemoveObjectsArgs.builder()
                        .bucket(bucket)
                        .objects(deleteObjects)
                        .build()
        );

        // 5. 检查有没有哪些顽固分子没删掉
        for (Result<DeleteError> errorResult : errors) {
            DeleteError error = errorResult.get();
            log.error("哎呀，删除失败了: " + error.objectName() + "，原因: " + error.message());
        }

        log.info("✅ 文件夹打扫完毕！共清理了 {} 个文件。", deleteObjects.size());
    }
}