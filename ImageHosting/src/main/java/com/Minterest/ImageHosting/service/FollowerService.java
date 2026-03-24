package com.Minterest.ImageHosting.service;

import com.Minterest.ImageHosting.model.AppFeatures.RedisPubSubNotification;
import com.Minterest.ImageHosting.model.Follower;
import com.Minterest.ImageHosting.model.User;
import com.Minterest.ImageHosting.repo.mysql.FollowerRepository;
import com.Minterest.ImageHosting.repo.mysql.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowerService {
    private final UserRepository userRepository;
    private final FollowerRepository followerRepository;
    private final RedisPublisherService redisPublisherService;

    @Transactional
    public String followUser(String followerUsername, String followingUsername) {
        User followingUser = userRepository.findByUsername(followingUsername)
                .orElseThrow(() -> new RuntimeException("Target user not found: " + followingUsername));
        User followerUser = userRepository.findByUsername(followerUsername)
                .orElseThrow(() -> new RuntimeException("Follower user not found: " + followerUsername));

        if (followerUsername.equals(followingUsername)) {
            return "You cannot follow yourself";
        }

        if (followerRepository.findByFollowerAndFollowing(followerUser, followingUser).isPresent()) {
            return followerUsername + " is already following " + followingUsername;
        }

        Follower newFollow = new Follower();
        newFollow.setFollower(followerUser);
        newFollow.setFollowing(followingUser);
        followerRepository.save(newFollow);

        followingUser.setFollowerCount(followingUser.getFollowerCount() + 1);
        userRepository.save(followingUser);

        log.info("{} is now following {}", followerUsername, followingUsername);

        // dispatch to Redis Pub/Sub
        RedisPubSubNotification notification = new RedisPubSubNotification(
                "follow",
                "Followed_You",
                followerUser.getUserId(),
                followingUser.getUserId(),
                java.time.LocalDateTime.now()
        );
        redisPublisherService.publishFollowEvent(notification);

        return followerUsername + " is now following " + followingUsername;
    }

    @Transactional
    public String unFollowUser(String followerUsername, String followingUsername) {
        User followingUser = userRepository.findByUsername(followingUsername)
                .orElseThrow(() -> new RuntimeException("Target user not found: " + followingUsername));
        User followerUser = userRepository.findByUsername(followerUsername)
                .orElseThrow(() -> new RuntimeException("Follower user not found: " + followerUsername));

        followerRepository.findByFollowerAndFollowing(followerUser, followingUser).ifPresent(follow -> {
            followerRepository.delete(follow);

            int count = followingUser.getFollowerCount();
            if (count > 0) {
                followingUser.setFollowerCount(count - 1);
                userRepository.save(followingUser);
            }

            log.info("{} stopped following {}", followerUsername, followingUsername);

            // dispatch to Redis Pub/Sub
            RedisPubSubNotification notification = new RedisPubSubNotification(
                    "follow",
                    "UnFollowed_You",
                    followerUser.getUserId(),
                    followingUser.getUserId(),
                    java.time.LocalDateTime.now()
            );
            redisPublisherService.publishFollowEvent(notification);
        });

        return followerUsername + " unfollowed " + followingUsername;
    }
}
