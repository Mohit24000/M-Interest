package com.Minterest.ImageHosting.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class SseService {
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public void addEmitter(SseEmitter emitter) {
        emitters.add(emitter);
        log.info("New SSE Emitter added. Total emitters: {}", emitters.size());
        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            log.info("SSE Emitter completed. Total emitters: {}", emitters.size());
        });
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            log.info("SSE Emitter timed out. Total emitters: {}", emitters.size());
        });
        emitter.onError(e -> {
            emitters.remove(emitter);
            log.info("SSE Emitter error. Total emitters: {}", emitters.size());
        });
    }

    public void broadcast(String channel, String message) {
        log.info("Broadcasting to {} emitters on channel '{}'", emitters.size(), channel);
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(channel).data(message));
                log.info("Successfully sent message to an emitter");
            } catch (IOException e) {
                log.warn("Failed to send message to an emitter, removing it");
                emitters.remove(emitter);
            }
        }
    }
}
