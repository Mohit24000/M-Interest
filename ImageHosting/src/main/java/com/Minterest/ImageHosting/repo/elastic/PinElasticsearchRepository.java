package com.Minterest.ImageHosting.repo.elastic;


import com.Minterest.ImageHosting.config.elastic.PinDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PinElasticsearchRepository extends ElasticsearchRepository<PinDocument, String> {

    // Simple search by title
    Page<PinDocument> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    // Simple search by tags
    Page<PinDocument> findByTagsIn(List<String> tags, Pageable pageable);

    // Combined search using Elasticsearch query
    @Query("{\"bool\": {\"should\": [" +
            "{\"match\": {\"title\": {\"query\": \"?0\", \"fuzziness\": \"AUTO\"}}}," +
            "{\"match\": {\"description\": {\"query\": \"?0\", \"fuzziness\": \"AUTO\"}}}," +
            "{\"match\": {\"tags\": {\"query\": \"?0\", \"fuzziness\": \"AUTO\"}}}" +
            "]}}")
    Page<PinDocument> searchPins(String searchTerm, Pageable pageable);
    // Find by user
    Page<PinDocument> findByUserId(UUID userId, Pageable pageable);

    // Delete by pinId
    void deleteByPinId(UUID pinId);
}