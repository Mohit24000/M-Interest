package com.Minterest.ImageHosting.config.redis;

import com.Minterest.ImageHosting.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSubscriber implements MessageListener {
    
    private final SseService sseService;
    private final StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = stringRedisSerializer.deserialize(message.getChannel());
        String body = stringRedisSerializer.deserialize(message.getBody());
        
        log.info("Message Received from channel '{}': {}", channel, body);
        sseService.broadcast(channel, body);
    }
}
