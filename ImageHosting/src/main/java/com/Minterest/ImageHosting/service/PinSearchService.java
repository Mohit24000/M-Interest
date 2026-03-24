package com.Minterest.ImageHosting.service;

import com.Minterest.ImageHosting.config.elastic.PinDocument;
import com.Minterest.ImageHosting.model.Pin;
import com.Minterest.ImageHosting.repo.elastic.PinElasticsearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PinSearchService {

    private final PinElasticsearchRepository pinElasticsearchRepository;

    /**
     * Convert Pin to PinDocument for indexing
     */
    public PinDocument convertToDocument(Pin pin) {
        return PinDocument.builder()
                .id(pin.getPinId() != null ? pin.getPinId().toString() : null)
                .pinId(pin.getPinId() != null ? pin.getPinId().toString() : null)
                .userId(pin.getUser() != null && pin.getUser().getUserId() != null ? pin.getUser().getUserId().toString() : null)
                .username(pin.getUser() != null ? pin.getUser().getUsername() : null)
                .title(pin.getTitle())
                .description(pin.getDescription())
                .pinUrl(pin.getPinUrl())
                .downloadUrl(pin.getDownloadUrl())
                .uploadedAt(pin.getUploadedAt() != null ? pin.getUploadedAt().toString() : null)
                .saves(pin.getSaves())
                .tags(pin.getTags())
                .commentCount(pin.getCommentsList() != null ? pin.getCommentsList().size() : 0)
                .likeCount(pin.getLikesList() != null ? pin.getLikesList().size() : 0)
                .build();
    }

    /**
     * Index a single pin
     */
    public void indexPin(Pin pin) {
        try {
            PinDocument document = convertToDocument(pin);
            pinElasticsearchRepository.save(document);
            log.info("Pin indexed successfully: {}", pin.getPinId());
        } catch (Exception e) {
            log.error("Failed to index pin: {}", pin.getPinId(), e);
        }
    }

    /**
     * Index multiple pins
     */
    public void indexAllPins(List<Pin> pins) {
        try {
            List<PinDocument> documents = pins.stream()
                    .map(this::convertToDocument)
                    .toList();
            pinElasticsearchRepository.saveAll(documents);
            log.info("Indexed {} pins successfully", pins.size());
        } catch (Exception e) {
            log.error("Failed to index pins", e);
        }
    }

    /**
     * Remove pin from index
     */
    public void removePinFromIndex(UUID pinId) {
        try {
            pinElasticsearchRepository.deleteById(pinId.toString());
            log.info("Pin removed from index: {}", pinId);
        } catch (Exception e) {
            log.error("Failed to remove pin from index: {}", pinId, e);
        }
    }

    /**
     * Search pins by title
     */
    public Page<PinDocument> searchByTitle(String title, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("uploadedAt").descending());
        return pinElasticsearchRepository.findByTitleContainingIgnoreCase(title, pageable);
    }

    /**
     * Search pins by tags
     */
    public Page<PinDocument> searchByTags(List<String> tags, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("uploadedAt").descending());
        return pinElasticsearchRepository.findByTagsIn(tags, pageable);
    }

    /**
     * Search pins by title, description, and tags
     */
    public Page<PinDocument> searchPins(String query, List<String> tags, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("uploadedAt").descending());

        if (query == null || query.trim().isEmpty()) {
            // If no query, just search by tags
            if (tags != null && !tags.isEmpty()) {
                return pinElasticsearchRepository.findByTagsIn(tags, pageable);
            }
            // If no query and no tags, return all pins
            return pinElasticsearchRepository.findAll(pageable);
        }

        // If no query, just search by tags
        if (tags != null && !tags.isEmpty()) {
            return pinElasticsearchRepository.findByTagsIn(tags, pageable);
        }
        // Fall through: full text search (tags ignored if both provided - ES @Query handles title+desc+tags)
        return pinElasticsearchRepository.searchPins(query.trim(), pageable);
    }

    /**
     * Get pins by user
     */
    public Page<PinDocument> getUserPins(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("uploadedAt").descending());
        return pinElasticsearchRepository.findByUserId(userId.toString(), pageable);
    }

    /**
     * Get trending pins (most liked/recent)
     */
    @Cacheable(value = "trending_pins")
    public Page<PinDocument> getTrendingPins(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("likeCount").descending());
        return pinElasticsearchRepository.findAll(pageable);
    }

    /**
     * Get pin by ID from index
     */
    @Cacheable(value = "pin")
    public PinDocument getPinById(UUID pinId) {
        return pinElasticsearchRepository.findById(pinId.toString()).orElse(null);
    }

    public List<String> getSuggestions(String query) {
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }
        Pageable pageable = PageRequest.of(0, 10);
        Page<PinDocument> results = pinElasticsearchRepository.findByTitleContainingIgnoreCase(query.trim(), pageable);
        return results.getContent().stream()
                .map(PinDocument::getTitle)
                .distinct()
                .limit(8)
                .toList();
    
    }
}

