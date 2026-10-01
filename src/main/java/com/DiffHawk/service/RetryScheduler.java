package com.DiffHawk.service;

import com.DiffHawk.client.GitHubClient;
import com.DiffHawk.consumer.ReviewConsumer;
import com.DiffHawk.domain.GithubRepo;
import com.DiffHawk.domain.Review;
import com.DiffHawk.domain.ReviewStatus;
import com.DiffHawk.repository.ReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class RetryScheduler {
    private static final Logger log = LoggerFactory.getLogger(RetryScheduler.class);
    private final ReviewRepository reviewRepository;
    private final GitHubClient gitHubClient;

    public RetryScheduler(ReviewRepository reviewRepository, GitHubClient gitHubClient) {
        this.reviewRepository = reviewRepository;
        this.gitHubClient = gitHubClient;
    }

    @Scheduled(fixedRate = 300000)
    public void retryFailedComments() {

        List<Review> failedReviews =
                reviewRepository.findByStatus(
                        ReviewStatus.COMMENT_FAILED
                );

        for (Review review : failedReviews) {

            try {
                GithubRepo repo = review.getRepo();

                String owner = repo.getOwner();
                String name = repo.getName();

                String accessToken =
                        repo.getUser().getAccessToken();

                String summaryComment = String.format("""
                    ## 🦅 DiffHawk AI Review

                    **Overall Score:** %d/100

                    **Total Findings:** %d

                    ---
                    _This review summary was posted by DiffHawk after a retry._
                    """,
                        review.getOverallScore(),
                        review.getTotalFindings()
                );

                gitHubClient.postPrComment(
                        owner,
                        name,
                        review.getPrNumber(),
                        summaryComment,
                        accessToken
                );

                log.info("Successfully retried review for PR #{}", review.getPrNumber());
                review.setStatus(ReviewStatus.COMPLETED);
                reviewRepository.save(review);

            } catch (Exception e) {

                log.warn("Retry failed for PR #{}: {}", review.getPrNumber(), e.getMessage());
            }
        }
    }

    @Scheduled(fixedRate = 600000)
    public void cleanupStuckReviews() {

        Instant cutoff =
                Instant.now().minus(10, ChronoUnit.MINUTES);

        List<Review> stuckReviews =
                reviewRepository.findByStatusAndCreatedAtBefore(
                        ReviewStatus.IN_PROGRESS,
                        cutoff
                );

        for (Review review : stuckReviews) {

            review.setStatus(
                    ReviewStatus.FAILED
            );

            reviewRepository.save(review);

            log.warn(
                    "Marked stuck review as FAILED. reviewId={}, prNumber={}",
                    review.getId(),
                    review.getPrNumber()
            );
        }
    }
}
