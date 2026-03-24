package com.Minterest.ImageHosting.controller;

import com.Minterest.ImageHosting.service.SseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/stream")
public class SseController {
    private final SseService sseService;

    public SseController(SseService sseService) {
        this.sseService = sseService;
    }

    @GetMapping
    public SseEmitter stream() {
        SseEmitter emitter = new SseEmitter(3600000L); // 1 hour timeout
        sseService.addEmitter(emitter);
        return emitter;
    }
}
