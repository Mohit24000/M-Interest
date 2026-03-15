package com.Minterest.ImageHosting.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class UserDTO {

    private UUID userId;

    private String username;

    private String email;

    private String bio;

    private String profileImageUrl;

    private int followerCount;

}