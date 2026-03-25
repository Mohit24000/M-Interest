package com.Minterest.ImageHosting.service;

import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentModerationService {

    private final AmazonRekognition rekognitionClient;

    // Only block these high-risk parent categories.
    // "Suggestive" is intentionally excluded to avoid false positives on anime/art/swimwear.
    private static final Set<String> BLOCKED_PARENT_LABELS = Set.of(
            "Explicit Nudity",
            "Violence",
            "Visually Disturbing",
            "Hate Symbols",
            "Drugs"
    );

    /**
     * Checks if an image in S3 is safe (no NSFW content).
     * @param bucket S3 bucket name
     * @param key S3 object key
     * @return true if safe, false if inappropriate content detected
     */
    public boolean isImageSafe(String bucket, String key) {
        try {
            DetectModerationLabelsRequest request = new DetectModerationLabelsRequest()
                    .withImage(new Image().withS3Object(new S3Object().withBucket(bucket).withName(key)))
                    .withMinConfidence(90F);

            DetectModerationLabelsResult result = rekognitionClient.detectModerationLabels(request);
            List<ModerationLabel> labels = result.getModerationLabels();

            List<ModerationLabel> blockedLabels = labels.stream()
                    .filter(label -> {
                        String parent = label.getParentName();
                        String name = label.getName();
                        // Block if parent category is in blocked list, or the label itself is
                        return BLOCKED_PARENT_LABELS.contains(parent) || BLOCKED_PARENT_LABELS.contains(name);
                    })
                    .collect(Collectors.toList());

            if (!blockedLabels.isEmpty()) {
                String detectedLabels = blockedLabels.stream()
                        .map(label -> label.getName() + " [" + label.getParentName() + "] (" + label.getConfidence() + "%)")
                        .collect(Collectors.joining(", "));
                log.warn("Blocked NSFW content for file {} in bucket {}: {}", key, bucket, detectedLabels);
                return false;
            }

            // Log any non-blocked labels just for visibility
            if (!labels.isEmpty()) {
                String allowedLabels = labels.stream()
                        .map(label -> label.getName() + " [" + label.getParentName() + "]")
                        .collect(Collectors.joining(", "));
                log.info("Non-blocked moderation labels (allowed) for file {}: {}", key, allowedLabels);
            }

            return true;
        } catch (Exception e) {
            log.warn("Content moderation check failed for file {}. Error: {}. Allowing upload.", key, e.getMessage());
            return true;
        }
    }
}
