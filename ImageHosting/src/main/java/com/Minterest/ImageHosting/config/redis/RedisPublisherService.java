package com.Minterest.ImageHosting.config.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisPublisherService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void publishLikeEvent(String message) {
        log.info("Publishing Like Event: {}", message);
        redisTemplate.convertAndSend("like", message);
    }

    public void publishCommentEvent(String message) {
        log.info("Publishing Comment Event: {}", message);
        redisTemplate.convertAndSend("comment", message);
    }

    public void publishFollowEvent(String message) {
        log.info("Publishing Follow Event: {}", message);
        redisTemplate.convertAndSend("follow", message);
    }
}
