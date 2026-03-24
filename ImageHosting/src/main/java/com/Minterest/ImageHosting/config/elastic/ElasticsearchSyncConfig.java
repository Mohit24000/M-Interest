package com.Minterest.ImageHosting.config.elastic;
import com.Minterest.ImageHosting.model.Pin;
import com.Minterest.ImageHosting.repo.mysql.PinRepository;
import com.Minterest.ImageHosting.service.PinSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchSyncConfig {

    private final PinRepository pinRepository;
    private final PinSearchService pinSearchService;

    @Transactional(readOnly = true)
    @EventListener(ApplicationReadyEvent.class)
    public void syncExistingPins() {
        log.info("Syncing existing pins to Elasticsearch...");
        // findAllWithUser uses JOIN FETCH so the User proxy is initialized
        // before the session closes, preventing LazyInitializationException.
        List<Pin> allPins = pinRepository.findAllWithUser();
        pinSearchService.indexAllPins(allPins);
        log.info("Synced {} pins to Elasticsearch", allPins.size());
    }
}