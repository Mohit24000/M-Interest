package com.Minterest.ImageHosting.service.ImageService;

import com.Minterest.ImageHosting.dto.UploadResponse;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

public interface PinServiceInterface {
    UploadResponse uploadFile(MultipartFile file, String folderPath, Map<String, String> metadata);
    String getPresignedUrl(String filename, int expiryMinutes);
    void deleteFile(String filename);
    String generatePublicUrl(String filename);
    byte[] downloadFile(String filename); // add later on
}

