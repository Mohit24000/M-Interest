package com.Minterest.ImageHosting.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PinLike {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID likeId;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "pin_id", nullable = false)
    private Pin pin;

    private LocalDateTime likedAt = LocalDateTime.now();
}