package com.Minterest.ImageHosting.controller;

import com.Minterest.ImageHosting.dto.UserDTO;
import com.Minterest.ImageHosting.model.User;
import com.Minterest.ImageHosting.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/user")
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
    @PatchMapping("/{id}/bio")
    public UserDTO updateProfileImage(@PathVariable UUID id,
                             @RequestParam String profileImageUrl){

        return userService.updateProfileImage(id, profileImageUrl);
    }
}
