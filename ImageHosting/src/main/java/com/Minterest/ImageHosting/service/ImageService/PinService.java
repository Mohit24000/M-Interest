package com.Minterest.ImageHosting.service.ImageService;


import com.Minterest.ImageHosting.model.AppFeatures.RedisPubSubNotification;
import com.Minterest.ImageHosting.model.Pin;
import com.Minterest.ImageHosting.model.SavedPin;
import com.Minterest.ImageHosting.model.User;
import com.Minterest.ImageHosting.repo.mysql.CommentRepository;
import com.Minterest.ImageHosting.repo.mysql.PinLikeRepository;
import com.Minterest.ImageHosting.repo.mysql.PinRepository;
import com.Minterest.ImageHosting.repo.mysql.SavedPinRepository;
import com.Minterest.ImageHosting.repo.mysql.UserRepository;
import com.Minterest.ImageHosting.service.ContentModerationService;
import com.Minterest.ImageHosting.service.RedisFeedService;
import com.Minterest.ImageHosting.service.RedisPublisherService;
import com.Minterest.ImageHosting.service.PinSearchService;
import com.Minterest.ImageHosting.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
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
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PinServiceInterface s3FileService;
    private final PinLikeRepository pinLikeRepository;
    private final RedisFeedService redisFeedService;
    private final RedisPublisherService redisPublisherService;
    private final PinSearchService pinSearchService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ContentModerationService moderationService;
    private final SavedPinRepository savedPinRepository;

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

        // 1. Check Daily Upload Limit (Max 50 per day)
        String limitKey = UPLOAD_LIMIT_KEY + userId + ":" + LocalDate.now();
        Long uploadCount = redisTemplate.opsForValue().increment(limitKey);

        if (uploadCount != null && uploadCount > 3) {
            log.warn("User {} exceeded daily upload limit", userId);
            redisTemplate.opsForValue().decrement(limitKey);
            throw new RuntimeException("Daily upload limit exceeded. You can only upload 3 pins per day.");
        }

        if (uploadCount != null && uploadCount == 1) {
            redisTemplate.expire(limitKey, 24, TimeUnit.HOURS);
        }

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

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

        // Dispatch to Redis Pub/Sub for new pin upload
        RedisPubSubNotification notification = new RedisPubSubNotification(
            "pin_upload",
            "UPLOAD_PIN",
            userId,
            savedPin.getPinId(),
            LocalDateTime.now()
        );
        redisPublisherService.publishPinUploadEvent(notification);

        // Add to trending feed with base score 0.5
        redisFeedService.updatePinScore(savedPin.getPinId(), 0.5);

        return savedPin;
    }

    @Transactional
    public Pin updatePinImage(UUID pinId, MultipartFile newImage) {
        Pin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new ResourceNotFoundException("Pin not found"));

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
                .orElseThrow(() -> new ResourceNotFoundException("Pin not found"));

        // 1. Delete image from S3
        String key = extractKeyFromUrl(pin.getPinUrl());
        if (key != null) {
            s3FileService.deleteFile(key);
        }

        // 2. Remove from Elasticsearch index
        pinSearchService.removePinFromIndex(pinId);

        // 3. Clear from Redis feeds
        redisFeedService.removePinFromFeed(pinId);

        // 4. Delete related records that might block DB deletion
        pinLikeRepository.deleteByPin(pin);
        savedPinRepository.deleteByPin(pin);

        // 5. Delete pin from database (comments will be cascade deleted via JPA)
        pinRepository.delete(pin);
        log.info("Pin deleted successfully: {}", pinId);
    }

    @Transactional(readOnly = true)
    public String getPinImageUrl(UUID pinId, int expiryMinutes) {
        Pin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new ResourceNotFoundException("Pin not found"));

        String key = extractKeyFromUrl(pin.getPinUrl());
        return s3FileService.getPresignedUrl(key, expiryMinutes);
    }

    @Transactional(readOnly = true)
    public Pin getPinWithDetails(UUID pinId) {
        Pin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new ResourceNotFoundException("Pin not found"));

        // Filter to only top-level comments to prevent duplicate rendering in frontend
        pin.setCommentsList(commentRepository.findByPinAndParentCommentIsNull(pin));

        refreshPinUrl(pin); // Ensure URL is fresh
        return pin;
    }

    private void refreshPinUrl(Pin pin) {
        if (pin == null || pin.getPinUrl() == null) {
            System.out.println("DEBUG: Pin or PinURL is NULL for id: " + (pin != null ? pin.getPinId() : "unknown"));
            return;
        }
        String currentUrl = pin.getPinUrl();
        String key = extractKeyFromUrl(currentUrl);

        System.out.println("DEBUG: Refreshing URL for Pin: " + pin.getPinId() + " [" + pin.getTitle() + "] Key: " + key);

        if (key != null && !key.isEmpty()) {
            try {
                String freshUrl = s3FileService.getPresignedUrl(key, 60);
                pin.setPinUrl(freshUrl);
                log.info("Successfully refreshed S3 URL for Pin: {} [{}]", pin.getPinId(), pin.getTitle());
            } catch (Exception e) {
                System.out.println("DEBUG: Failed refresh for Key: " + key + ". Error: " + e.getMessage());
                log.error("Failed to refresh S3 URL for Pin: {}. Key: {}. Error: {}", pin.getPinId(), key, e.getMessage());
            }
        } else {
            System.out.println("DEBUG: Key extraction failed for URL: " + currentUrl);
        }
    }

    @Transactional(readOnly = true)
    public com.Minterest.ImageHosting.model.AppFeatures.Feed getPinsByUser(UUID userId, int page, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        org.springframework.data.domain.PageRequest pageRequest = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("uploadedAt").descending());
        org.springframework.data.domain.Page<Pin> pinPage = pinRepository.findByUser(user, pageRequest);
        log.info("### FEED_TRACE_V2 ### Fetching user pins for {}. Page {} size {}. Found in DB: {}", userId, page, size, pinPage.getTotalElements());
        List<Pin> pins = pinPage.getContent();
        pins.forEach(this::refreshPinUrl);
        return new com.Minterest.ImageHosting.model.AppFeatures.Feed(pins, page, size, (int) pinPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public com.Minterest.ImageHosting.model.AppFeatures.Feed getTrendingFeed(int page, int size) {
        java.util.List<java.util.UUID> trendingPinIds = redisFeedService.getTrendingPinIds(page, size);
        List<Pin> trendingPins;
        long totalCount = pinRepository.count();
        log.info("### FEED_TRACE_V2 ### Fetching trending feed page {} size {}. Total pins in DB: {}", page, size, totalCount);

        if (trendingPinIds.isEmpty() && totalCount > 0) {
            org.springframework.data.domain.PageRequest pageRequest =
                org.springframework.data.domain.PageRequest.of(page, size,
                    org.springframework.data.domain.Sort.by("uploadedAt").descending());
            trendingPins = pinRepository.findAll(pageRequest).getContent();
            // Populate Redis with base score for these pins so they stay in trending
            trendingPins.forEach(p -> redisFeedService.updatePinScore(p.getPinId(), 0.5));
        } else {
            trendingPins = trendingPinIds.stream()
                .map(id -> {
                    try { return pinRepository.findById(id).orElse(null); }
                    catch (Exception e) { return null; }
                })
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());
        }
        trendingPins.forEach(this::refreshPinUrl);
        return new com.Minterest.ImageHosting.model.AppFeatures.Feed(trendingPins, page, size, totalCount);
    }

    @Transactional(readOnly = true)
    public com.Minterest.ImageHosting.model.AppFeatures.Feed searchPins(String query, int page, int size) {
        org.springframework.data.domain.PageRequest pageRequest =
            org.springframework.data.domain.PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by("uploadedAt").descending());

        org.springframework.data.domain.Page<Pin> pinPage;
        if (query == null || query.trim().isEmpty()) {
            pinPage = pinRepository.findAll(pageRequest);
        } else {
            String q = query.trim();
            pinPage = pinRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(q, q, pageRequest);
        }

        List<Pin> results = pinPage.getContent();
        results.forEach(this::refreshPinUrl);
        return new com.Minterest.ImageHosting.model.AppFeatures.Feed(results, page, size, pinPage.getTotalElements());
    }

    private String extractKeyFromUrl(String url) {
        if (url == null || url.trim().isEmpty()) return null;
        try {
            // 1. Remove query parameters if present
            String cleanUrl = url.contains("?") ? url.substring(0, url.indexOf("?")) : url;

            // 2. Use URI for path extraction
            java.net.URI uri = new java.net.URI(cleanUrl);
            String path = uri.getPath();
            if (path == null) return null;

            if (path.startsWith("/")) path = path.substring(1);

            // Trim bucketName just in case there's whitespace from properties
            String trimmedBucket = bucketName != null ? bucketName.trim() : null;

            // 3. Handle both virtual-hosted and path-style URLs
            if (trimmedBucket != null && path.startsWith(trimmedBucket + "/")) {
                path = path.substring(trimmedBucket.length() + 1);
            }

            return path;
        } catch (Exception e) {
            log.error("Failed to extract key from URL: {}", url);
            return null;
        }
    }

    @Transactional
    public void likePin(UUID pinId, UUID userId) {
        // 1. Check if already liked in DB
        Pin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new ResourceNotFoundException("Pin not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

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

            log.info("Buffered Like Event in Redis for User: {} on Pin: {}", userId, pinId);


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

    @Transactional
    public void savePin(UUID pinId, UUID userId) {
        Pin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new RuntimeException("Pin not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (savedPinRepository.findByUserAndPin(user, pin).isPresent()) {
            return;
        }

        SavedPin savedPin = new SavedPin();
        savedPin.setUser(user);
        savedPin.setPin(pin);
        savedPinRepository.save(savedPin);
        pin.setSaves(pin.getSaves() + 1);
        pinRepository.save(pin);

        //  score (+4)
        redisFeedService.updatePinScore(pinId, 4.0);

        log.info("User {} saved Pin {}", userId, pinId);
    }

    @Transactional
    public void unsavePin(UUID pinId, UUID userId) {
        Pin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new RuntimeException("Pin not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        savedPinRepository.findByUserAndPin(user, pin).ifPresent(savedPin -> {
            savedPinRepository.delete(savedPin);
            pin.setSaves(Math.max(0, pin.getSaves() - 1));
            pinRepository.save(pin);

            //  score (-4)
            redisFeedService.updatePinScore(pinId, -4.0);

            log.info("User {} unsaved Pin {}", userId, pinId);
        });
    }

    @Transactional(readOnly = true)
    public boolean isPinSavedByUser(UUID pinId, UUID userId) {
        Pin pin = pinRepository.findById(pinId).orElse(null);
        User user = userRepository.findById(userId).orElse(null);
        if (pin == null || user == null) return false;
        return savedPinRepository.findByUserAndPin(user, pin).isPresent();
    }

    @Transactional(readOnly = true)
    public com.Minterest.ImageHosting.model.AppFeatures.Feed getSavedPinsByUser(UUID userId, int page, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        org.springframework.data.domain.PageRequest pageRequest = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<SavedPin> savedPage = savedPinRepository.findByUser(user, pageRequest);

        List<Pin> pins = savedPage.getContent().stream()
                .map(SavedPin::getPin)
                .collect(java.util.stream.Collectors.toList());

        pins.forEach(this::refreshPinUrl);
        return new com.Minterest.ImageHosting.model.AppFeatures.Feed(pins, page, size, (int) savedPage.getTotalElements());
    }
}