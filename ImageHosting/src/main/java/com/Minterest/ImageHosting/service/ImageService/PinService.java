package com.Minterest.ImageHosting.service.ImageService;


import com.Minterest.ImageHosting.model.Pin;
import com.Minterest.ImageHosting.model.PinLike;
import com.Minterest.ImageHosting.model.User;
import com.Minterest.ImageHosting.repo.mysql.PinLikeRepository;
import com.Minterest.ImageHosting.repo.mysql.PinRepository;
import com.Minterest.ImageHosting.repo.mysql.UserRepository;
import com.Minterest.ImageHosting.service.RedisFeedService;
import com.Minterest.ImageHosting.config.redis.RedisPublisherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    @Transactional
    public Pin createPin(MultipartFile imageFile,
                         String title,
                         String description,
                         UUID userId,
                         List<String> tags) {

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Prepare metadata for S3
        Map<String, String> metadata = new HashMap<>();
        metadata.put("userId", userId.toString());
        metadata.put("title", title);
        metadata.put("uploadSource", "PinCreation");

        // Upload image to S3
        String folderPath = "pins/" + userId;
        var uploadResponse = s3FileService.uploadFile(imageFile, folderPath, metadata);

        // Create Pin entity
        Pin pin = new Pin();
        pin.setUser(user);
        pin.setTitle(title);
        pin.setDescription(description);
        pin.setPinUrl(uploadResponse.getUrl()); // Presigned URL for viewing
        pin.setDownloadUrl(s3FileService.generatePublicUrl(uploadResponse.getKey())); // Public URL
        pin.setTags(tags);
        pin.setUploadedAt(LocalDateTime.now());
        pin.setUpdatedAt(LocalDateTime.now());

        // Get image dimensions if available (you might need to extract from file)
        // pin.setImageWidth(width);
        // pin.setImageHeight(height);

        Pin savedPin = pinRepository.save(pin);
        log.info("Pin created successfully with ID: {} and S3 key: {}", savedPin.getPinId(), uploadResponse.getKey());

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

        return pinRepository.save(pin);
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
        Pin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new RuntimeException("Pin not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (pinLikeRepository.findByUserAndPin(user, pin).isEmpty()) {
            PinLike pinLike = new PinLike();
            pinLike.setUser(user);
            pinLike.setPin(pin);
            pinLikeRepository.save(pinLike);

            // Increase trending score by 2
            redisFeedService.updatePinScore(pinId, 2.0);
            
            // Dispatch to Redis Pub/Sub
            String msg = String.format("User %s liked Pin %s", userId, pinId);
            redisPublisherService.publishLikeEvent(msg);
            
            log.info(msg);
        }
    }

    @Transactional
    public void unlikePin(UUID pinId, UUID userId) {
        Pin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new RuntimeException("Pin not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        pinLikeRepository.findByUserAndPin(user, pin).ifPresent(pinLike -> {
            pinLikeRepository.delete(pinLike);

            // Decrease trending score by 2
            redisFeedService.updatePinScore(pinId, -2.0);
            log.info("User {} unliked Pin {}", userId, pinId);
        });
    }

    @Transactional(readOnly = true)
    public long getPinLikesCount(UUID pinId) {
        Pin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new RuntimeException("Pin not found"));
        return pinLikeRepository.countByPin(pin);
    }
}