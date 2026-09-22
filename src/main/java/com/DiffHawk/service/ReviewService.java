package com.DiffHawk.service;

import com.DiffHawk.client.GitHubClient;
import com.DiffHawk.domain.*;
import com.DiffHawk.dto.AiReviewResponseDto;
import com.DiffHawk.dto.FindingDto;
import com.DiffHawk.repository.ReviewFindingRepository;
import com.DiffHawk.repository.ReviewRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final ReviewFindingRepository reviewFindingRepository;
    private final GitHubClient gitHubClient;

    public ReviewService(
            ReviewRepository reviewRepository,
            ReviewFindingRepository reviewFindingRepository,
            GitHubClient gitHubClient
    ) {
        this.reviewRepository = reviewRepository;
        this.reviewFindingRepository = reviewFindingRepository;
        this.gitHubClient = gitHubClient;
    }
    public void processReviewResult(
            Review review,
            AiReviewResponseDto aiResponse,
            String owner,
            String repo,
            String headSha,
            String accessToken
    ) {
        for (FindingDto finding : aiResponse.findings()) {

            ReviewFinding reviewFinding = ReviewFinding.builder()
                    .review(review)
                    .filePath(finding.file())
                    .lineNumber(finding.line())
                    .severity(ReviewSeverity.valueOf(finding.severity()))
                    .category(ReviewCategory.valueOf(finding.category()))
                    .message(finding.message())
                    .suggestion(finding.suggestion())
                    .build();
            reviewFindingRepository.save(reviewFinding);
            if (finding.line() != null) {

                String commentBody = String.format(
                        "%s %s — %s%n%s%n💡 %s",
                        severityEmoji(finding.severity()),
                        finding.severity(),
                        finding.category(),
                        finding.message(),
                        finding.suggestion()
                );
                try {
                    gitHubClient.postInlineComment(
                            owner,
                            repo,
                            review.getPrNumber(),
                            headSha,
                            finding.file(),
                            finding.line(),
                            commentBody,
                            accessToken
                    );
                } catch (Exception e) {
                    System.out.println("Skipping inline comment for " + finding.file()
                            + " line " + finding.line() + ": " + e.getMessage());
                }
            }
        }
        try {
            long criticalCount = aiResponse.findings().stream()
                    .filter(f -> "CRITICAL".equals(f.severity()))
                    .count();

            long highCount = aiResponse.findings().stream()
                    .filter(f -> "HIGH".equals(f.severity()))
                    .count();

            long mediumCount = aiResponse.findings().stream()
                    .filter(f -> "MEDIUM".equals(f.severity()))
                    .count();

            long lowCount = aiResponse.findings().stream()
                    .filter(f -> "LOW".equals(f.severity()))
                    .count();

            long infoCount = aiResponse.findings().stream()
                    .filter(f -> "INFO".equals(f.severity()))
                    .count();

            String summaryComment = String.format("""
            ## 🦅 DiffHawk AI Review

            **Overall Score:** %d/100

            ### Findings

            🔴 **Critical:** %d
            🟠 **High:** %d
            🟡 **Medium:** %d
            🔵 **Low:** %d
            ⚪ **Info:** %d

            **Total Findings:** %d

            ---
            _Review generated automatically by DiffHawk._
            """,
                    aiResponse.overallScore(),
                    criticalCount,
                    highCount,
                    mediumCount,
                    lowCount,
                    infoCount,
                    aiResponse.findings().size()
            );

            gitHubClient.postPrComment(
                    owner,
                    repo,
                    review.getPrNumber(),
                    summaryComment,
                    accessToken
            );

            review.setOverallScore(aiResponse.overallScore());
            review.setTotalFindings(aiResponse.findings().size());
            review.setStatus(ReviewStatus.COMPLETED);
            review.setCompletedAt(Instant.now());

            reviewRepository.save(review);

        } catch (Exception e) {

            review.setOverallScore(aiResponse.overallScore());
            review.setTotalFindings(aiResponse.findings().size());
            review.setStatus(ReviewStatus.COMMENT_FAILED);

            reviewRepository.save(review);

            throw e;
        }
    }
    private String severityEmoji(String severity) {
        return switch (severity) {
            case "CRITICAL" -> "🔴";
            case "HIGH" -> "🟠";
            case "MEDIUM" -> "🟡";
            case "LOW" -> "🔵";
            default -> "⚪";
        };
    }
}
