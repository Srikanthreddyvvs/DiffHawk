package com.DiffHawk.service;

import com.DiffHawk.domain.User;
import com.DiffHawk.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        String accessToken = request.getAccessToken().getTokenValue();
        OAuth2User oAuth2User = super.loadUser(request);

        Long githubUserId = ((Number) oAuth2User.getAttribute("id")).longValue();
        String username = oAuth2User.getAttribute("login");
        String email = oAuth2User.getAttribute("email");
        String avatarUrl = oAuth2User.getAttribute("avatar_url");

        Optional<User> existingUser = userRepository.findByGithubUserId(githubUserId);
        User user;
        if(existingUser.isPresent()){
            user = existingUser.get();
            user.setAccessToken(accessToken);
            userRepository.save(user);
        }else{
            user = User.builder()
                    .githubUserId(githubUserId)
                    .username(username)
                    .email(email)
                    .avatarUrl(avatarUrl)
                    .accessToken(accessToken)
                    .build();
            userRepository.save(user);
        }
        return oAuth2User;
    }
}
