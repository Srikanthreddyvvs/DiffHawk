package com.DiffHawk.config;

import com.DiffHawk.domain.User;
import com.DiffHawk.repository.UserRepository;
import com.DiffHawk.service.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public OAuth2SuccessHandler(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        if(oAuth2User==null){
            throw new RuntimeException("user not found");
        }
        String username = oAuth2User.getAttribute("login");
        Long githubUserId = ((Number) oAuth2User.getAttribute("id")).longValue();
        User user = userRepository.findByGithubUserId(githubUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        String token = jwtService.generateToken(user.getId(),username);

        response.setContentType("application/json");
        response.getWriter().write("{\"token\":\"" + token + "\"}");
    }
}
