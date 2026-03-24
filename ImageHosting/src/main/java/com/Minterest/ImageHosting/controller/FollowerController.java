package com.Minterest.ImageHosting.controller;

import com.Minterest.ImageHosting.service.FollowerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/follow")
@RequiredArgsConstructor
public class FollowerController {

    private final FollowerService followerService;

    @PostMapping("/{followingUsername}")
    public ResponseEntity<String> followUser(
            @RequestParam String followerUsername,
            @PathVariable String followingUsername) {
        String result = followerService.followUser(followerUsername, followingUsername);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{followingUsername}")
    public ResponseEntity<String> unfollowUser(
            @RequestParam String followerUsername,
            @PathVariable String followingUsername) {
        String result = followerService.unFollowUser(followerUsername, followingUsername);
        return ResponseEntity.ok(result);
    }
}
