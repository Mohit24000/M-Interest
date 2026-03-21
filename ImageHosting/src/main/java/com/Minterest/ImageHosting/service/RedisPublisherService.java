package com.Minterest.ImageHosting.service;

import com.Minterest.ImageHosting.model.AppFeatures.RedisPubSubNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisPublisherService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void publishLikeEvent(RedisPubSubNotification notification) {
        log.info("Publishing Like Event: {}", notification);
        redisTemplate.convertAndSend("like", notification);
    }

    public void publishCommentEvent(RedisPubSubNotification notification) {
        log.info("Publishing Comment Event: {}", notification);
        redisTemplate.convertAndSend("comment", notification);
    }

    public void publishFollowEvent(RedisPubSubNotification notification) {
        log.info("Publishing Follow Event: {}", notification);
        redisTemplate.convertAndSend("follow", notification);
    }
}
