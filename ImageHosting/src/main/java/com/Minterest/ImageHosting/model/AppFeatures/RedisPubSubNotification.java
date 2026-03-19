package com.Minterest.ImageHosting.model.AppFeatures;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event payload published to Redis Pub/Sub channels (like, comment, follow).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RedisPubSubNotification {
    private String channel;
    private String action;
    private UUID actorUserId;
    private UUID targetId;   // pinId or userId depending on action
    private LocalDateTime timestamp;
}
