package com.Minterest.ImageHosting.controller;


import com.Minterest.ImageHosting.model.AppFeatures.Feed;
import com.Minterest.ImageHosting.model.Pin;
import com.Minterest.ImageHosting.service.ImageService.PinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/pins")
@RequiredArgsConstructor
public class PinController {

    private final PinService pinService;
    private final com.Minterest.ImageHosting.service.RedisFeedService redisFeedService;

    // Pin endpoints
    @PostMapping
    public ResponseEntity<Pin> createPin(
            @RequestParam("image") MultipartFile image,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("userId") UUID userId,
            @RequestParam(value = "tags", required = false) List<String> tags) {

        Pin createdPin = pinService.createPin(image, title, description, userId, tags);
        return new ResponseEntity<>(createdPin, HttpStatus.CREATED);
    }

    @GetMapping("/{pinId}")
    public ResponseEntity<Pin> getPin(@PathVariable UUID pinId) {
        Pin pin = pinService.getPinWithDetails(pinId);
        return ResponseEntity.ok(pin);
    }

    @PutMapping("/{pinId}/image")
    public ResponseEntity<Pin> updatePinImage(
            @PathVariable UUID pinId,
            @RequestParam("image") MultipartFile newImage) {

        Pin updatedPin = pinService.updatePinImage(pinId, newImage);
        return ResponseEntity.ok(updatedPin);
    }

    @DeleteMapping("/{pinId}")
    public ResponseEntity<Void> deletePin(@PathVariable UUID pinId) {
        pinService.deletePin(pinId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{pinId}/image-url")
    public ResponseEntity<String> getPinImageUrl(
            @PathVariable UUID pinId,
            @RequestParam(defaultValue = "60") int expiryMinutes) {

        String imageUrl = pinService.getPinImageUrl(pinId, expiryMinutes);
        return ResponseEntity.ok(imageUrl);
    }


    @GetMapping("/trending")
    public ResponseEntity<Feed> getTrendingFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Set<UUID> trendingPinIds = redisFeedService.getTrendingPinIds(page, size);
        
        List<Pin> trendingPins = trendingPinIds.stream()
                .map(pinService::getPinWithDetails)
                .collect(Collectors.toList());
        
        Feed feed = new Feed(trendingPins, page, size, trendingPins.size()); // totalPins could be improved
                
        return ResponseEntity.ok(feed);
    }

    @PostMapping("/{pinId}/like")
    public ResponseEntity<Void> likePin(@PathVariable UUID pinId, @RequestParam UUID userId) {
        pinService.likePin(pinId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{pinId}/unlike")
    public ResponseEntity<Void> unlikePin(@PathVariable UUID pinId, @RequestParam UUID userId) {
        pinService.unlikePin(pinId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{pinId}/likes")
    public ResponseEntity<Long> getPinLikesCount(@PathVariable UUID pinId) {
        long count = pinService.getPinLikesCount(pinId);
        return ResponseEntity.ok(count);
    }

    @PostMapping("/{pinId}/save")
    public ResponseEntity<Void> savePin(@PathVariable UUID pinId, @RequestParam UUID userId) {
        pinService.savePin(pinId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{pinId}/unsave")
    public ResponseEntity<Void> unsavePin(@PathVariable UUID pinId, @RequestParam UUID userId) {
        pinService.unsavePin(pinId, userId);
        return ResponseEntity.ok().build();
    }
}