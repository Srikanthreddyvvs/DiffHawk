package com.DiffHawk.controller;

import com.DiffHawk.domain.User;
import com.DiffHawk.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private  final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String,Object>> getMe(){
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(userId == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Optional<User> userOptional = userRepository.findById(userId);
        if(userOptional.isPresent()){
            Map<String, Object> response = new HashMap<>();
            User user = userOptional.get();
            response.put("id",user.getId());
            response.put("username",user.getUsername());
            response.put("email",user.getEmail());
            response.put("avatarUrl",user.getAvatarUrl());

            return ResponseEntity.status(HttpStatus.OK)
                    .body(response);
        }
        return ResponseEntity.notFound().build();
    }
}
