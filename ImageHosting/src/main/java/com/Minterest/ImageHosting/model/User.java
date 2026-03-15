package com.Minterest.ImageHosting.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID userId;

    @Column(unique = true, nullable = false)
    private String username;

    private String password;

    private Integer age;

    @Column(unique = true, nullable = false)
    @Email
    private String email;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private String bio;
    private String profileImageUrl;
    private int followerCount;

    @ToString.Exclude
    @OneToMany(mappedBy = "following")
    private List<Follower> followerList = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Pin> pinList = new ArrayList<>();

    @ToString.Exclude
    @OneToMany(mappedBy = "user")
    private List<PinLike> likedPins = new ArrayList<>();

}
