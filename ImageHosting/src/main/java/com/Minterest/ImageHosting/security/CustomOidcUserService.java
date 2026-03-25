package com.Minterest.ImageHosting.security;

import com.Minterest.ImageHosting.model.User;
import com.Minterest.ImageHosting.repo.mysql.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final UserRepository userRepository;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        
        try {
            return processOidcUser(userRequest, oidcUser);
        } catch (Exception ex) {
            throw new OAuth2AuthenticationException(ex.getMessage());
        }
    }

    private OidcUser processOidcUser(OidcUserRequest userRequest, OidcUser oidcUser) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oidcUser.getAttributes();

        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String profileImageUrl = (String) attributes.get("picture");

        if (email == null || email.isEmpty()) {
            throw new RuntimeException("Email not found from OIDC provider");
        }

        if (name == null || name.trim().isEmpty()) {
            name = email.split("@")[0];
        }

        Optional<User> userOptional = userRepository.findByEmailIgnoreCase(email);
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
            if (profileImageUrl != null && (user.getProfileImageUrl() == null || user.getProfileImageUrl().isEmpty())) {
                user.setProfileImageUrl(profileImageUrl);
                user = userRepository.save(user);
            }
        } else {
            user = new User();
            user.setEmail(email);
            String baseUsername = name.replaceAll("\\s+", "").toLowerCase();
            user.setUsername(baseUsername + "_" + UUID.randomUUID().toString().substring(0,5));
            user.setProfileImageUrl(profileImageUrl);
            user.setFollowerCount(0);
            user = userRepository.save(user);
            log.info("New user registered via OIDC {}: {}", registrationId, user.getEmail());
        }

        return new DefaultOidcUser(
                Collections.emptyList(),
                oidcUser.getIdToken(),
                oidcUser.getUserInfo(),
                "email"
        );
    }
}
