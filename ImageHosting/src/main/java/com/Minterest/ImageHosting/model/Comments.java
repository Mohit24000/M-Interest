package com.Minterest.ImageHosting.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.BatchSize;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data

public class Comments {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String content;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "pin_id")
    private Pin pin ;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "parent_comment_id")
    private Comments parentComment;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL)
    @BatchSize(size = 20)
    private List<Comments> replies = new ArrayList<>();
}
