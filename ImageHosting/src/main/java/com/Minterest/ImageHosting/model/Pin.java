package com.Minterest.ImageHosting.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Pin {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID pinId;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    private String description;

    private Integer imageWidth;
    private Integer imageHeight;

    @Column(nullable = false)
    private String pinUrl;

    private  String downloadUrl;

    @CreationTimestamp
    private LocalDateTime uploadedAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @ToString.Exclude
    @OneToMany(mappedBy = "pin", cascade = CascadeType.ALL)
    private List<PinLike> likesList = new ArrayList<>();

    private long saves;

    @ToString.Exclude
    @OneToMany(mappedBy = "pin", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comments> commentsList = new ArrayList<>();

    @ElementCollection
    private List<String> tags = new ArrayList<>();
}
