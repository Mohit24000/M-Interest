package com.Minterest.ImageHosting.controller;

import com.Minterest.ImageHosting.config.elastic.PinDocument;
import com.Minterest.ImageHosting.service.PinSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class PinSearchController {

    private final PinSearchService pinSearchService;

    /**
     * Search pins by query and tags
     */
    @GetMapping("/pins")
    public ResponseEntity<Page<PinDocument>> searchPins(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<PinDocument> results = pinSearchService.searchPins(q, tags, page, size);
        return ResponseEntity.ok(results);
    }

    /**
     * Search pins by title only
     */
    @GetMapping("/pins/title")
    public ResponseEntity<Page<PinDocument>> searchByTitle(
            @RequestParam String title,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<PinDocument> results = pinSearchService.searchByTitle(title, page, size);
        return ResponseEntity.ok(results);
    }

    /**
     * Search pins by tags only
     */
    @GetMapping("/pins/tags")
    public ResponseEntity<Page<PinDocument>> searchByTags(
            @RequestParam List<String> tags,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<PinDocument> results = pinSearchService.searchByTags(tags, page, size);
        return ResponseEntity.ok(results);
    }

    /**
     * Get pins by user
     */
    @GetMapping("/users/{userId}/pins")
    public ResponseEntity<Page<PinDocument>> getUserPins(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<PinDocument> results = pinSearchService.getUserPins(userId, page, size);
        return ResponseEntity.ok(results);
    }

    /**
     * Get trending pins
     */
    @GetMapping("/pins/trending")
    public ResponseEntity<Page<PinDocument>> getTrendingPins(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<PinDocument> results = pinSearchService.getTrendingPins(page, size);
        return ResponseEntity.ok(results);
    }

    /**
     * Get single pin by ID from search index
     */
    @GetMapping("/pins/{pinId}")
    public ResponseEntity<PinDocument> getPinFromIndex(@PathVariable UUID pinId) {
        PinDocument pin = pinSearchService.getPinById(pinId);
        if (pin != null) {
            return ResponseEntity.ok(pin);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<String>> getSuggestions(@RequestParam String q) {
        return ResponseEntity.ok(pinSearchService.getSuggestions(q));
    
    }
}

