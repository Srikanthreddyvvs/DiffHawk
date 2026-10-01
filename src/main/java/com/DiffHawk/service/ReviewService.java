package com.DiffHawk.service;

import com.DiffHawk.client.GitHubClient;
import com.DiffHawk.domain.*;
import com.DiffHawk.dto.AiReviewResponseDto;
import com.DiffHawk.dto.FindingDto;
import com.DiffHawk.exception.GitHubApiException;
import com.DiffHawk.repository.ReviewFindingRepository;
import com.DiffHawk.repository.ReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReviewService {
    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);
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
        List<String> fallbackFindings = new ArrayList<>();
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
                } catch (GitHubApiException e) {
                    if(e.getStatusCode()==422){
                        fallbackFindings.add(String.format(
                                "- **%s:%d** — %s%n  💡 %s",
                                finding.file(),
                                finding.line(),
                                finding.message(),
                                finding.suggestion()
                        ));
                        log.warn(
                                "GitHub rejected inline comment for {}:{}; " +
                                        "adding finding to summary.",
                                finding.file(),
                                finding.line());
                    }else{
                        log.error(
                                "GitHub API failed while posting inline comment. " +
                                        "status={}, file={}, line={}",
                                e.getStatusCode(),
                                finding.file(),
                                finding.line(),
                                e
                        );
                        throw e;
                    }
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

            StringBuilder summaryComment = new StringBuilder(
                    String.format("""
            ## 🦅 DiffHawk AI Review

            **Overall Score:** %d/100

            ### Findings

            🔴 **Critical:** %d
            🟠 **High:** %d
            🟡 **Medium:** %d
            🔵 **Low:** %d
            ⚪ **Info:** %d

            **Total Findings:** %d
            """,
                    aiResponse.overallScore(),
                    criticalCount,
                    highCount,
                    mediumCount,
                    lowCount,
                    infoCount,
                    aiResponse.findings().size()
            )
            );
            if (!fallbackFindings.isEmpty()) {

                summaryComment.append("""

                        ### ⚠️ Findings Not Posted Inline

                        Some findings could not be posted inline because
                        their line numbers did not match the GitHub PR diff.

                        Please review these findings:

                        """);

                for (String fallbackFinding : fallbackFindings) {
                    summaryComment
                            .append(fallbackFinding)
                            .append("\n\n");
                }
            }

            summaryComment.append("""

                    ---
                    _Review generated automatically by DiffHawk._
                    """);

            gitHubClient.postPrComment(
                    owner,
                    repo,
                    review.getPrNumber(),
                    summaryComment.toString(),
                    accessToken
            );

            review.setOverallScore(aiResponse.overallScore());
            review.setTotalFindings(aiResponse.findings().size());
            review.setStatus(ReviewStatus.COMPLETED);
            review.setCompletedAt(Instant.now());

            reviewRepository.save(review);
            log.info(
                    "Review completed successfully for PR #{} with {} findings. " +
                            "Fallback findings={}",
                    review.getPrNumber(),
                    aiResponse.findings().size(),
                    fallbackFindings.size()
            );

        } catch (Exception e) {

            review.setOverallScore(aiResponse.overallScore());
            review.setTotalFindings(aiResponse.findings().size());
            review.setStatus(ReviewStatus.COMMENT_FAILED);

            reviewRepository.save(review);
            log.error(
                    "Failed to post review summary for PR #{}",
                    review.getPrNumber(),
                    e
            );

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
