package com.Minterest.ImageHosting.service;


import com.Minterest.ImageHosting.dto.UserDTO;
import com.Minterest.ImageHosting.model.User;
import com.Minterest.ImageHosting.repo.mysql.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    public UserDTO createUser(User user){

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

    public String login(String email, String password, HttpSession session) {
        return userRepository.findByEmailIgnoreCase(email).map(user -> {
            if (!user.getPassword().equals(password)) {
                return "Invalid Credentials";
            }
            session.setAttribute("email", user.getEmail());
            session.setAttribute("userId", user.getUserId());
            return "Login SuccessFull : ".concat(session.getId());
        }).orElse("Invalid Credentials");
    }

    public String getProfile(HttpSession session) {
        if (session == null) {
            return "Not LoggedIn";
        }
        Object email = session.getAttribute("email");

        if (email == null) {
            return "Not LoggedIn";
        }
        return "Welcome : " + email ;
    }


}
