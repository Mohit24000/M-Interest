package com.Minterest.ImageHosting.controller;


import com.Minterest.ImageHosting.model.Comments;
import com.Minterest.ImageHosting.model.Pin;
import com.Minterest.ImageHosting.service.CommentService;
import com.Minterest.ImageHosting.service.ImageService.PinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/pins")
@RequiredArgsConstructor
public class PinController {

    private final PinService pinService;

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


}