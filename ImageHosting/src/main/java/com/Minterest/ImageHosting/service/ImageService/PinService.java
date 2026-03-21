package com.Minterest.ImageHosting.service.ImageService;


import com.Minterest.ImageHosting.model.AppFeatures.RedisPubSubNotification;
import com.Minterest.ImageHosting.model.Pin;
import com.Minterest.ImageHosting.model.User;
import com.Minterest.ImageHosting.repo.mysql.PinLikeRepository;
import com.Minterest.ImageHosting.repo.mysql.PinRepository;
import com.Minterest.ImageHosting.repo.mysql.UserRepository;
import com.Minterest.ImageHosting.service.ContentModerationService;
import com.Minterest.ImageHosting.service.RedisFeedService;
import com.Minterest.ImageHosting.service.RedisPublisherService;
import com.Minterest.ImageHosting.service.PinSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PinService {

    private final PinRepository pinRepository;
    private final UserRepository userRepository;
    private final PinServiceInterface s3FileService;
    private final PinLikeRepository pinLikeRepository;
    private final RedisFeedService redisFeedService;
    private final RedisPublisherService redisPublisherService;
    private final PinSearchService pinSearchService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ContentModerationService moderationService;

    @Value("${app.s3.bucket}")
    private String bucketName;

    private static final String LIKE_BUFFER_KEY = "likes:buffer:";
    private static final String SYNC_PENDING_KEY = "likes:sync:pins";
    private static final String UPLOAD_LIMIT_KEY = "upload:limit:";

    @Transactional
    public Pin createPin(MultipartFile imageFile,
                         String title,
                         String description,
                         UUID userId,
                         List<String> tags) {

        // 1. Check Daily Upload Limit (Max 3 per day)
        String limitKey = UPLOAD_LIMIT_KEY + userId + ":" + LocalDate.now();
        Long uploadCount = redisTemplate.opsForValue().increment(limitKey);
        
        if (uploadCount != null && uploadCount > 3) {
            log.warn("User {} exceeded daily upload limit", userId);
            throw new RuntimeException("You have reached your daily limit of 3 pins. Try again tomorrow!");
        }
        
        if (uploadCount != null && uploadCount == 1) {
            redisTemplate.expire(limitKey, 24, TimeUnit.HOURS);
        }

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Prepare metadata for S3
        Map<String, String> metadata = new HashMap<>();
        metadata.put("userId", userId.toString());
        metadata.put("title", title);
        metadata.put("uploadSource", "PinCreation");

        // 2. Upload image to S3 (Quarantine/Temp state)
        String folderPath = "pins/" + userId;
        var uploadResponse = s3FileService.uploadFile(imageFile, folderPath, metadata);
        String s3Key = uploadResponse.getKey();

        // 3. NSFW Content Moderation Check
        if (!moderationService.isImageSafe(bucketName, s3Key)) {
            s3FileService.deleteFile(s3Key); // Delete from S3 immediately
            // Decrement limit if upload failed due to moderation
            redisTemplate.opsForValue().decrement(limitKey);
            throw new RuntimeException("Upload failed: Inappropriate content detected.");
        }

        // 4. Create Pin entity (Finalize)
        Pin pin = new Pin();
        pin.setUser(user);
        pin.setTitle(title);
        pin.setDescription(description);
        pin.setPinUrl(uploadResponse.getUrl()); // Presigned URL for viewing
        pin.setDownloadUrl(s3FileService.generatePublicUrl(s3Key)); // Public URL
        pin.setTags(tags);
        pin.setUploadedAt(LocalDateTime.now());
        pin.setUpdatedAt(LocalDateTime.now());

        Pin savedPin = pinRepository.save(pin);
        log.info("Pin created successfully with ID: {} and S3 key: {}", savedPin.getPinId(), s3Key);

        // Sync to Elasticsearch
        pinSearchService.indexPin(savedPin);

        return savedPin;
    }

    @Transactional
    public Pin updatePinImage(UUID pinId, MultipartFile newImage) {
        Pin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new RuntimeException("Pin not found"));

        // Delete old image from S3
        String oldKey = extractKeyFromUrl(pin.getPinUrl());
        if (oldKey != null) {
            s3FileService.deleteFile(oldKey);
        }

        // Upload new image
        String folderPath = "pins/" + pin.getUser().getUserId();
        Map<String, String> metadata = new HashMap<>();
        metadata.put("pinId", pinId.toString());
        metadata.put("updatedAt", LocalDateTime.now().toString());

        var uploadResponse = s3FileService.uploadFile(newImage, folderPath, metadata);

        // Update pin URLs
        pin.setPinUrl(uploadResponse.getUrl());
        pin.setDownloadUrl(s3FileService.generatePublicUrl(uploadResponse.getKey()));
        pin.setUpdatedAt(LocalDateTime.now());

        Pin updated = pinRepository.save(pin);
        // Re-sync to Elasticsearch
        pinSearchService.indexPin(updated);
        return updated;
    }

    @Transactional
    public void deletePin(UUID pinId) {
        Pin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new RuntimeException("Pin not found"));

        // Delete image from S3
        String key = extractKeyFromUrl(pin.getPinUrl());
        if (key != null) {
            s3FileService.deleteFile(key);
        }

        // Remove from Elasticsearch index
        pinSearchService.removePinFromIndex(pinId);

        // Delete pin from database (comments will be cascade deleted)
        pinRepository.delete(pin);
        log.info("Pin deleted successfully: {}", pinId);
    }

    @Transactional(readOnly = true)
    public String getPinImageUrl(UUID pinId, int expiryMinutes) {
        Pin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new RuntimeException("Pin not found"));

        String key = extractKeyFromUrl(pin.getPinUrl());
        return s3FileService.getPresignedUrl(key, expiryMinutes);
    }

    @Transactional(readOnly = true)
    public Pin getPinWithDetails(UUID pinId) {
        return pinRepository.findById(pinId)
                .orElseThrow(() -> new RuntimeException("Pin not found"));
    }

    // Helper method to extract S3 key from URL
    private String extractKeyFromUrl(String url) {
        if (url == null) return null;
        try {
            // For presigned URLs
            if (url.contains("?")) {
                url = url.substring(0, url.indexOf("?"));
            }
            // Extract key after bucket name
            String bucketPattern = "https://.*?\\.s3\\.amazonaws\\.com/";
            return url.replaceFirst(bucketPattern, "");
        } catch (Exception e) {
            log.error("Failed to extract key from URL: {}", url, e);
            return null;
        }
    }

    @Transactional
    public void likePin(UUID pinId, UUID userId) {
        // 1. Check if already liked in DB
        Pin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new RuntimeException("Pin not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (pinLikeRepository.findByUserAndPin(user, pin).isPresent()) {
            return; // Already in DB
        }

        // 2. Check and Add to Redis Buffer
        String bufferKey = LIKE_BUFFER_KEY + pinId;
        Boolean isNewLike = redisTemplate.opsForSet().add(bufferKey, userId.toString()) > 0;

        if (Boolean.TRUE.equals(isNewLike)) {
            // Add pin to sync-pending list
            redisTemplate.opsForSet().add(SYNC_PENDING_KEY, pinId.toString());

            // Increase trending score by 2
            redisFeedService.updatePinScore(pinId, 2.0);
            
            // Dispatch to Redis Pub/Sub
            RedisPubSubNotification notification = new RedisPubSubNotification(
                "like", "LIKE_PIN", userId, pinId, java.time.LocalDateTime.now()
            );
            redisPublisherService.publishLikeEvent(notification);
            
            log.info("Buffered Like Event in Redis: {}", notification);
        }
    }

    @Transactional
    public void unlikePin(UUID pinId, UUID userId) {
        // 1. Remove from Redis Buffer if exists
        String bufferKey = LIKE_BUFFER_KEY + pinId;
        Long removedCount = redisTemplate.opsForSet().remove(bufferKey, userId.toString());

        if (removedCount != null && removedCount > 0) {
            redisFeedService.updatePinScore(pinId, -2.0);
            log.info("Removed buffered like for User {} on Pin {}", userId, pinId);
            return;
        }

        // 2. Otherwise remove from DB immediately (Unlikes are usually less frequent)
        Pin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new RuntimeException("Pin not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        pinLikeRepository.findByUserAndPin(user, pin).ifPresent(pinLike -> {
            pinLikeRepository.delete(pinLike);
            redisFeedService.updatePinScore(pinId, -2.0);
            log.info("User {} unliked Pin {} in DB", userId, pinId);
        });
    }

    @Transactional(readOnly = true)
    public long getPinLikesCount(UUID pinId) {
        Pin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new RuntimeException("Pin not found"));
        
        // Sum DB count + Redis buffer count
        long dbCount = pinLikeRepository.countByPin(pin);
        
        String bufferKey = LIKE_BUFFER_KEY + pinId;
        Long bufferCount = redisTemplate.opsForSet().size(bufferKey);
        
        return dbCount + (bufferCount != null ? bufferCount : 0);
    }
}