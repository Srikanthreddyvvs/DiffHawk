package com.DiffHawk.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "review_findings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewFinding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "review_id",nullable = false)
    private Review review;

    @Column(name = "file_path",nullable = false)
    private String filePath;

    @Column(name = "line_number")
    private Integer lineNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity")
    private ReviewSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private ReviewCategory category;

    @Column(name = "message",nullable = false)
    private String message;

    @Column(name = "suggestion")
    private String suggestion;

    @Column(name = "created_at")
    @CreationTimestamp
    private Instant createdAt;
}
