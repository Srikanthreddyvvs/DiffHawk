package com.DiffHawk.consumer;

import com.DiffHawk.client.GitHubClient;
import com.DiffHawk.domain.GithubRepo;
import com.DiffHawk.domain.Review;
import com.DiffHawk.domain.ReviewStatus;
import com.DiffHawk.exception.RepoNotFoundException;
import com.DiffHawk.repository.RepoRepository;
import com.DiffHawk.repository.ReviewRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Service
public class ReviewConsumer {
    private final ObjectMapper objectMapper;
    private final RepoRepository repoRepository;
    private final ReviewRepository reviewRepository;
    private final GitHubClient gitHubClient;

    public ReviewConsumer(ObjectMapper objectMapper, RepoRepository repoRepository, ReviewRepository reviewRepository, GitHubClient gitHubClient) {
        this.objectMapper = objectMapper;
        this.repoRepository = repoRepository;
        this.reviewRepository = reviewRepository;
        this.gitHubClient = gitHubClient;
    }

    @KafkaListener(topics = "${kafka.topics.pr-review-requested}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeReviewRequest(String message) {
        try {
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
            Review review = Review.builder()
                    .repo(repo)
                    .prNumber(prNumber)
                    .headSha(headSha)
                    .status(ReviewStatus.IN_PROGRESS)
                    .build();
            reviewRepository.save(review);
            List<Map<String, Object>> files =
                    gitHubClient.fetchPullRequestFiles(
                            ownerLogin,
                            name,
                            prNumber,
                            accessToken
                    );
            System.out.println("Fetched " + files.size() + " files for PR #" + prNumber);
        } catch (Exception e) {
            System.out.println("Failed to process pull request review request");
        }

    }
    }
