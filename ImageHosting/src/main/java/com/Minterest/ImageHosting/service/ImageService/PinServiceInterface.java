package com.Minterest.ImageHosting.service.ImageService;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface PinServiceInterface {
    public void uploadFile(MultipartFile file, String folderPath, Map<String, String> metadata);
    public interface FileServiceInterface {
       String getPresignedUrl(String filename, int expiryMinutes);
        void deleteFile(String filename);
        String generatePublicUrl(String filename);
        Map<String, Object> getStorageMetrics();
        List<String> listFolders(String prefix);
        String uploadMultipart(MultipartFile file, String folderPath);
        byte[] downloadFile(String filename);
    }
}
