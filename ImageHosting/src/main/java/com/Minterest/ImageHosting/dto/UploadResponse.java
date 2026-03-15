package com.Minterest.ImageHosting.dto;


import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class UploadResponse {
    private String filename;
    private String key;
    private String originalFilename;
    private long size;
    private String contentType;
    private String url;
    private Map<String, String> metadata;
    private LocalDateTime uploadedAt;
}