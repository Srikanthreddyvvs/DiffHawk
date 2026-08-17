package com.DiffHawk.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "repositories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GithubRepo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id",nullable = false)
    private User user;

    @Column(name = "github_repo_id",unique = true,nullable = false)
    private Long githubRepoId;

    @Column(name = "owner",nullable = false)
    private String owner;

    @Column(name = "name",nullable = false)
    private String name;

    @Column(name = "webhook_secret",unique = true,nullable = false)
    private String webhookSecret;

    @Column(name = "created_at")
    @CreationTimestamp
    private Instant createdAt;
}
