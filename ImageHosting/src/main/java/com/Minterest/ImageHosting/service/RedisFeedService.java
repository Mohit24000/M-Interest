package com.Minterest.ImageHosting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisFeedService {

    private final RedisTemplate<String, String> redisTemplate;
    
    private static final String TRENDING_PINS_KEY = "trending_pins";

    /**
     * Updates the score of a pin in the trending set.
     * Score can be calculated as e.g. (Likes * 2 + Comments * 3 + Saves * 4)
     */
    public void updatePinScore(UUID pinId, double delta) {
        redisTemplate.opsForZSet().incrementScore(TRENDING_PINS_KEY, pinId.toString(), delta);
        log.info("Updated trending score for pin {} by {}", pinId, delta);
    }

    /**
     * Gets the top trending pin IDs.
     */
    public Set<UUID> getTrendingPinIds(int page, int size) {
        long start = (long) page * size;
        long end = start + size - 1;
        
        Set<String> pinIds = redisTemplate.opsForZSet().reverseRange(TRENDING_PINS_KEY, start, end);
        
        if (pinIds == null) {
            return Set.of();
        }
        
        return pinIds.stream()
                .map(UUID::fromString)
                .collect(Collectors.toSet());
    }

    /**
     * Clears old pins from the trending feed (e.g. older than a week, or keeps top N)
     * Can be run via a @Scheduled job.
     */
    public void trimTrendingFeed(int keepTopN) {
        // Remove elements outside the top N ranking
        redisTemplate.opsForZSet().removeRange(TRENDING_PINS_KEY, 0, -(keepTopN + 1));
    }
}
