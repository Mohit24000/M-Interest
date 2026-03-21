package com.Minterest.ImageHosting.service;

import com.Minterest.ImageHosting.model.Pin;
import com.Minterest.ImageHosting.model.PinLike;
import com.Minterest.ImageHosting.model.User;
import com.Minterest.ImageHosting.repo.mysql.PinLikeRepository;
import com.Minterest.ImageHosting.repo.mysql.PinRepository;
import com.Minterest.ImageHosting.repo.mysql.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LikeSyncService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final PinLikeRepository pinLikeRepository;
    private final PinRepository pinRepository;
    private final UserRepository userRepository;

    private static final String LIKE_BUFFER_KEY = "likes:buffer:";
    private static final String SYNC_PENDING_KEY = "likes:sync:pins";

    @Scheduled(fixedRate = 60000) // Sync every 1 minute
    @Transactional
    public void syncLikesToDb() {
        log.info("Starting Like Synchronization Task...");

        // get all pins with pending likes
        Set<Object> pendingPins = redisTemplate.opsForSet().members(SYNC_PENDING_KEY);
        if (pendingPins == null || pendingPins.isEmpty()) {
            return;
        }

        for (Object pinIdObj : pendingPins) {
            String pinIdStr = (String) pinIdObj;
            UUID pinId = UUID.fromString(pinIdStr);
            String bufferKey = LIKE_BUFFER_KEY + pinIdStr;

            // pop all userIds from the set for this pin
            Set<Object> userIds = redisTemplate.opsForSet().members(bufferKey);
            if (userIds == null || userIds.isEmpty()) {
                redisTemplate.opsForSet().remove(SYNC_PENDING_KEY, pinIdStr);
                continue;
            }

            Pin pin = pinRepository.findById(pinId).orElse(null);
            if (pin == null) {
                redisTemplate.delete(bufferKey);
                redisTemplate.opsForSet().remove(SYNC_PENDING_KEY, pinIdStr);
                continue;
            }

            log.info("Syncing {} likes for Pin {}", userIds.size(), pinId);

            for (Object userIdObj : userIds) {
                String userIdStr = (String) userIdObj;
                UUID userId = UUID.fromString(userIdStr);
                
                User user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    // check if already exists in DB to prevent duplicates
                    if (pinLikeRepository.findByUserAndPin(user, pin).isEmpty()) {
                        PinLike pinLike = new PinLike();
                        pinLike.setUser(user);
                        pinLike.setPin(pin);
                        pinLike.setLikedAt(LocalDateTime.now());
                        pinLikeRepository.save(pinLike);
                    }
                }
                
                // remove from Redis buffer after processing
                redisTemplate.opsForSet().remove(bufferKey, userIdStr);
            }

            // remove pin from sync-pending if buffer is empty
            Long remaining = redisTemplate.opsForSet().size(bufferKey);
            if (remaining == null || remaining == 0) {
                redisTemplate.opsForSet().remove(SYNC_PENDING_KEY, pinIdStr);
            }
        }
        
        log.info("Like Synchronization done");
    }
}
