package com.Minterest.ImageHosting.config.elastic;
import com.Minterest.ImageHosting.model.Pin;
import com.Minterest.ImageHosting.repo.mysql.PinRepository;
import com.Minterest.ImageHosting.service.PinSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchSyncConfig {

    private final PinRepository pinRepository;
    private final PinSearchService pinSearchService;

    @EventListener(ApplicationReadyEvent.class)
    public void syncExistingPins() {
        log.info("Syncing existing pins to Elasticsearch...");

        List<Pin> allPins = pinRepository.findAll();
        pinSearchService.indexAllPins(allPins);

        log.info("Synced {} pins to Elasticsearch", allPins.size());
    }
}