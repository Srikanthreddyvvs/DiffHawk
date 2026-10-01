package com.DiffHawk.consumer;

import com.DiffHawk.client.AiServiceClient;
import com.DiffHawk.client.GitHubClient;
import com.DiffHawk.domain.GithubRepo;
import com.DiffHawk.domain.Review;
import com.DiffHawk.domain.ReviewStatus;
import com.DiffHawk.dto.AiReviewRequestDto;
import com.DiffHawk.dto.AiReviewResponseDto;
import com.DiffHawk.exception.RepoNotFoundException;
import com.DiffHawk.repository.RepoRepository;
import com.DiffHawk.repository.ReviewRepository;
import com.DiffHawk.service.ReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ReviewConsumer {
    private static final Logger log = LoggerFactory.getLogger(ReviewConsumer.class);
    private final ObjectMapper objectMapper;
    private final RepoRepository repoRepository;
    private final ReviewRepository reviewRepository;
    private final GitHubClient gitHubClient;
    private final AiServiceClient aiServiceClient;
    private final ReviewService reviewService;

    public ReviewConsumer(ObjectMapper objectMapper, RepoRepository repoRepository, ReviewRepository reviewRepository, GitHubClient gitHubClient, AiServiceClient aiServiceClient, ReviewService reviewService) {
        this.objectMapper = objectMapper;
        this.repoRepository = repoRepository;
        this.reviewRepository = reviewRepository;
        this.gitHubClient = gitHubClient;
        this.aiServiceClient = aiServiceClient;
        this.reviewService = reviewService;
    }
    @RetryableTopic(
            attempts = "3",
            backOff = @BackOff(delay = 60000, multiplier = 2),
            dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(topics = "${kafka.topics.pr-review-requested}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeReviewRequest(String message) throws Exception{

            Map<String, Object> payload = objectMapper.readValue(message, Map.class);
            Map<String, Object> repository = (Map<String, Object>) payload.get("repository");
            Map<String, Object> owner = (Map<String, Object>) repository.get("owner");
            String ownerLogin = (String) owner.get("login");
            String name = (String) repository.get("name");
            Map<String, Object> pullRequest = (Map<String, Object>) payload.get("pull_request");
            Long prNumber = ((Number) pullRequest.get("number")).longValue();
            Map<String, Object> head = (Map<String, Object>) pullRequest.get("head");
            String headSha = (String) head.get("sha");

            GithubRepo repo = repoRepository.findByOwnerAndName(ownerLogin, name)
                    .orElseThrow(() -> new RepoNotFoundException("Repository not found"));
            String accessToken = repo.getUser().getAccessToken();
            Optional<Review> existingReview = reviewRepository
                    .findByRepoAndPrNumberAndHeadSha(repo, prNumber, headSha);
            Review review;

            if (existingReview.isPresent()) {
                review = existingReview.get();
                if(review.getStatus()==ReviewStatus.COMPLETED){
                    log.info(
                            "Review already completed for PR #{} with SHA {}, skipping.",
                            prNumber,
                            headSha
                    );
                    return;
                }
                if(review.getStatus()==ReviewStatus.COMMENT_FAILED){
                    log.info(
                            "Review has COMMENT_FAILED status for PR #{}. " +
                                    "RetryScheduler will handle it. Skipping Kafka retry.",
                            prNumber
                    );
                    return;
                }
                if (review.getStatus() == ReviewStatus.FAILED) {
                    log.info(
                            "Review has FAILED status for PR #{}. Skipping.",
                            prNumber
                    );
                    return;
                }
                log.info(
                        "Resuming IN_PROGRESS review for PR #{}.",
                        prNumber
                );
            }else{
                review = Review.builder()
                        .repo(repo)
                        .prNumber(prNumber)
                        .headSha(headSha)
                        .status(ReviewStatus.IN_PROGRESS)
                        .build();
                reviewRepository.save(review);
            }
            List<Map<String, Object>> files =
                    gitHubClient.fetchPullRequestFiles(
                            ownerLogin,
                            name,
                            prNumber,
                            accessToken
                    );
            AiReviewRequestDto request = new AiReviewRequestDto(
                    review.getId(),
                    ownerLogin,
                    name,
                    prNumber,
                    files
            );
            AiReviewResponseDto response = aiServiceClient.requestReview(request);
            reviewService.processReviewResult(
                    review,
                    response,
                    ownerLogin,
                    name,
                    headSha,
                    accessToken
            );
    }
}
