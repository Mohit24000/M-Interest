package com.Minterest.ImageHosting.service;

import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentModerationService {

    private final AmazonRekognition rekognitionClient;

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
                    .withMinConfidence(75F);

            DetectModerationLabelsResult result = rekognitionClient.detectModerationLabels(request);
            List<ModerationLabel> labels = result.getModerationLabels();

            if (!labels.isEmpty()) {
                String detectedLabels = labels.stream()
                        .map(label -> label.getName() + " (" + label.getConfidence() + "%)")
                        .collect(Collectors.joining(", "));
                
                log.warn("NSFW content detected for file {} in bucket {}: {}", key, bucket, detectedLabels);
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("Error during content moderation check for file {}: {}", key, e.getMessage());
            // If moderation fails, we fail-safe and treat it as potentially unsafe 
            // OR we can allow it depending on business policy. Here we allow to avoid blocking users on AWS errors.
            return true; 
        }
    }
}
