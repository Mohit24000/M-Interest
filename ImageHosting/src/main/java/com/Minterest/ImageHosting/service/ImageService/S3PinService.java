package com.Minterest.ImageHosting.service.ImageService;

import com.Minterest.ImageHosting.dto.UploadResponse;
import com.Minterest.ImageHosting.exception.Image.FileUploadException;
import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3PinService implements PinServiceInterface {

    private final AmazonS3 s3Client;

    @Value("${app.s3.bucket}")
    private String bucketName;

    @Override
    public UploadResponse uploadFile(MultipartFile file, String folderPath, Map<String, String> metadata) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString() + extension;
        String key = folderPath + "/" + fileName;

        try {
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentType(file.getContentType());
            objectMetadata.setContentLength(file.getSize());
            if (metadata != null) {
                metadata.forEach(objectMetadata::addUserMetadata);
            }

            s3Client.putObject(new PutObjectRequest(bucketName, key, file.getInputStream(), objectMetadata));

            return UploadResponse.builder()
                    .filename(fileName)
                    .key(key)
                    .originalFilename(originalFilename)
                    .size(file.getSize())
                    .contentType(file.getContentType())
                    .url(generatePublicUrl(key))
                    .metadata(metadata)
                    .uploadedAt(LocalDateTime.now())
                    .build();

        } catch (IOException e) {
            log.error("Failed to upload file to S3: {}", key, e);
            throw new FileUploadException("Could not upload file: " + e.getMessage());
        }
    }

    @Override
    public String getPresignedUrl(String key, int expiryMinutes) {
        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += (long) expiryMinutes * 60 * 1000;
        expiration.setTime(expTimeMillis);

        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(bucketName, key)
                        .withMethod(HttpMethod.GET)
                        .withExpiration(expiration);

        return s3Client.generatePresignedUrl(generatePresignedUrlRequest).toString();
    }

    @Override
    public void deleteFile(String key) {
        s3Client.deleteObject(bucketName, key);
    }

    @Override
    public String generatePublicUrl(String key) {
        return String.format("https://%s.s3.amazonaws.com/%s", bucketName, key);
    }

    @Override
    public byte[] downloadFile(String key) {
        // Basic placeholder for now as per interface comment "add later on"
        return new byte[0];
    }
}
