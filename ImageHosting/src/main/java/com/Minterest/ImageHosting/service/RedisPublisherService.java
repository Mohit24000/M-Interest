package com.Minterest.ImageHosting.service;

import com.Minterest.ImageHosting.model.AppFeatures.RedisPubSubNotification;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisPublisherService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public void publishPinUploadEvent(RedisPubSubNotification notification) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(notification);
            log.info("Publishing Pin Upload Event (JSON): {}", jsonMessage);
            redisTemplate.convertAndSend("pin_upload", jsonMessage);
        } catch (Exception e) {
            log.error("Failed to serialize notification to JSON", e);
        }
    }
}
