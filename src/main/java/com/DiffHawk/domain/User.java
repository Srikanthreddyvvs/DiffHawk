package com.DiffHawk.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "github_user_id",nullable = false,unique = true)
    private Long githubUserId;

    @Column(nullable = false,unique = true)
    private String username;

    @Column(unique = true)
    private String email;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "access_token",nullable = false)
    private String accessToken;

    @Column(name = "created_at")
    @CreationTimestamp
    private Instant createdAt;
}
