package com.Minterest.ImageHosting.controller;

import com.Minterest.ImageHosting.dto.UserDTO;
import com.Minterest.ImageHosting.model.User;
import com.Minterest.ImageHosting.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService){
        this.userService = userService;
    }

    @PostMapping("/resister")
    public UserDTO registerUser(@RequestBody User user){

        return userService.createUser(user);
    }

    @GetMapping("/{id}")
    public UserDTO getUser(@PathVariable UUID id){

        return userService.getUserById(id);
    }

    @GetMapping("/all")
    public List<UserDTO> getAllUsers(){

        return userService.getAllUsers();
    }

    @PatchMapping("/{id}/bio")
    public UserDTO updateBio(@PathVariable UUID id,
                             @RequestParam String bio){

        return userService.updateBio(id, bio);
    }
    @PatchMapping("/{id}/username")
    public UserDTO updateUsername(@PathVariable UUID id,
                             @RequestParam String username){

        return userService.updateUsername(id, username);
    }
    @PatchMapping("/{id}/profileImage")
    public UserDTO updateProfileImage(@PathVariable UUID id,
                             @RequestParam String profileImageUrl){

        return userService.updateProfileImage(id, profileImageUrl);
    }


    @PatchMapping("/{id}/profile")
    public UserDTO updateProfile(@PathVariable UUID id,
                                 @RequestParam(required = false) String username,
                                 @RequestParam(required = false) String bio) {
        if (username != null) userService.updateUsername(id, username);
        if (bio != null) userService.updateBio(id, bio);
        return userService.getUserById(id);
    }
    @GetMapping("/logout")
    public void logout(HttpSession session) {
        session.invalidate();
    }

    @GetMapping("/me")
    public UserDTO getCurrentUser(org.springframework.security.core.Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
             throw new RuntimeException("Not authenticated");
        }
        
        Object principal = authentication.getPrincipal();
        if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User) {
             org.springframework.security.oauth2.core.user.OAuth2User oauth2User = (org.springframework.security.oauth2.core.user.OAuth2User) principal;
             String email = (String) oauth2User.getAttributes().get("email");
             if (email == null) {
                 String login = (String) oauth2User.getAttributes().get("login");
                 email = login + "@github.com";
             }
             return userService.getUserByEmail(email);
        }
        throw new RuntimeException("Unknown authentication type");
    }
}
