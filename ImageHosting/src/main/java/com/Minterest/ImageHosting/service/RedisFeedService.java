package com.Minterest.ImageHosting.service;

import com.Minterest.ImageHosting.model.Pin;
import com.Minterest.ImageHosting.model.PinLike;
import com.Minterest.ImageHosting.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
// using sorted set
public class RedisFeedService {
    private User user;
    private Pin pin ;
    private PinLike pinLike;

    private final ObjectMapper objectMapper;
    private final RedisTemplate<String , String> redisTemplate;
    @Setter
    private Supplier<Pair<Boolean, Duration>> expiryHandler;

    public <T> void cacheData(final String key, final String hashkey,
                              final T data, final Duration duration) {

    }

}
