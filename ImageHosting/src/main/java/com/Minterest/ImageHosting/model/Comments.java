package com.Minterest.ImageHosting.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data

public class Comments {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String content;

    @ManyToOne
    @JoinColumn(name = "pin_id")
    private Pin pin ;

    @ManyToOne
    @JoinColumn(name = "parent_comment_id")
    private Comments parentComment;

    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL)
    @BatchSize(size = 20)
    private List<Comments> replies = new ArrayList<>();
}
