package com.Minterest.ImageHosting.security;

import com.Minterest.ImageHosting.model.User;
import com.Minterest.ImageHosting.repo.mysql.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // Fetch the user information from the OAuth2 provider (Google, GitHub....)
        OAuth2User oAuth2User = super.loadUser(userRequest);

        try {
            return processOAuth2User(userRequest, oAuth2User);
        } catch (Exception ex) {
            // Throwing an exception will trigger the authentication failure handler
            throw new OAuth2AuthenticationException(ex.getMessage());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = "";
        String name = "";
        String profileImageUrl = null;

        // Extract normalized data depending on the provider
        if ("google".equalsIgnoreCase(registrationId)) {
            email = (String) attributes.get("email");
            name = (String) attributes.get("name");
            profileImageUrl = (String) attributes.get("picture");
        } else if ("github".equalsIgnoreCase(registrationId)) {
            // Note: GitHub might require an extra API call if email is private, but let's assume it's public for now.
            // Some github accounts have null emails in the public profile. In this case, login/name is used.
            email = (String) attributes.get("email");
            if (email == null) {
                 String login = (String) attributes.get("login");
                 email = login + "@github.com"; // Fallback email
            }
            name = (String) attributes.get("name");
            if (name == null) {
                name = (String) attributes.get("login");
            }
            profileImageUrl = (String) attributes.get("avatar_url");
        } else {
            throw new RuntimeException("Sorry! Login with " + registrationId + " is not supported yet.");
        }

        if (email == null || email.isEmpty()) {
            throw new RuntimeException("Email not found from OAuth2 provider");
        }

        Optional<User> userOptional = userRepository.findByEmailIgnoreCase(email);
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
            // Optional: Update user's name or profile picture here if they changed on provider side
            if(profileImageUrl != null && (user.getProfileImageUrl() == null || user.getProfileImageUrl().isEmpty())) {
                user.setProfileImageUrl(profileImageUrl);
                user = userRepository.save(user);
            }
        } else {
            // Register a new user
            user = new User();
            user.setEmail(email);
            // Default username generation
            String baseUsername = name.replaceAll("\\s+", "").toLowerCase();
            user.setUsername(baseUsername + "_" + UUID.randomUUID().toString().substring(0,5));
            user.setProfileImageUrl(profileImageUrl);
            user.setFollowerCount(0);
            user = userRepository.save(user);
            log.info("New user registered via OAuth2 {}: {}", registrationId, user.getEmail());
        }

        // Return a custom OAuth2 user object containing authorities & attributes
        // GitHub uses 'login' (String) as the name attribute key; Google uses 'email'.
        String nameAttributeKey;
        if ("google".equalsIgnoreCase(registrationId)) {
            nameAttributeKey = "email";
        } else {
            nameAttributeKey = "login"; // GitHub: 'id' is an Integer, must use 'login'
        }
        return new DefaultOAuth2User(
                Collections.emptyList(),
                attributes,
                nameAttributeKey
        );
    }
}
