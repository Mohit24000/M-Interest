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
public class Follower {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID followerId;

    //user who is following
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "follower_user_id", nullable = false)
    private User follower;

    //user being followed
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "following_user_id", nullable = false)
    private User following;

    private LocalDateTime followedAt = LocalDateTime.now();
}