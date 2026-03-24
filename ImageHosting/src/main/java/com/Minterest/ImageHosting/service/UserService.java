package com.Minterest.ImageHosting.service;


import com.Minterest.ImageHosting.dto.UserDTO;
import com.Minterest.ImageHosting.model.User;
import com.Minterest.ImageHosting.repo.mysql.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserDTO createUser(User user){
        // Encode password before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        // If username not set, assign a default
        if(user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            user.setUsername("user_" + UUID.randomUUID().toString().substring(0,8));
        }

        User saved = userRepository.save(user);
        return convertToDTO(saved);
    }
    public UserDTO getUserById(UUID userId){

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return convertToDTO(user);
    }
    public List<UserDTO> getAllUsers(){

        return userRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public UserDTO getUserByEmail(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        return convertToDTO(user);
    }

    public UserDTO updateBio(UUID userId, String bio){

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setBio(bio);

        userRepository.save(user);

        return convertToDTO(user);
    }
    public UserDTO updateUsername(UUID userId, String name){

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setUsername(name);

        userRepository.save(user);

        return convertToDTO(user);
    }
    public UserDTO updateProfileImage(UUID userId, String imageUrl){

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setProfileImageUrl(imageUrl);

        userRepository.save(user);

        return convertToDTO(user);
    }

    private UserDTO convertToDTO(User user){

        UserDTO dto = new UserDTO();

        dto.setUserId(user.getUserId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setBio(user.getBio());
        dto.setProfileImageUrl(user.getProfileImageUrl());
        dto.setFollowerCount(user.getFollowerCount());

        return dto;
    }
}
