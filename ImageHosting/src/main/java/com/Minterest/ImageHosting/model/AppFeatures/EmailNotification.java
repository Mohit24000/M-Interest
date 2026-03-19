package com.Minterest.ImageHosting.model.AppFeatures;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Event payload for triggering email notifications.
 * Published via EmailService when relevant actions occur (e.g. follow, comment).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailNotification {
    private String toEmail;
    private String subject;
    private String body;
    private UUID triggeringUserId;
}
