package com.DiffHawk.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "repository_id",nullable = false)
    private GithubRepo repo;

    @Column(name = "pr_number",nullable = false)
    private Long prNumber;

    @Column(name = "head_sha",nullable = false)
    private String headSha;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ReviewStatus status;

    @Column(name = "overall_score")
    private Integer overallScore;

    @Column(name = "total_findings")
    private Integer totalFindings;

    @Column(name = "created_at",updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
