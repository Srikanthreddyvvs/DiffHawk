package com.DiffHawk.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Component
public class GitHubClient {
    private final WebClient webClient;

    public GitHubClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public List<Map<String, Object>> fetchPullRequestFiles(String owner, String name, Long prNumber, String accessToken){
        return webClient.get()
                .uri("/repos/{owner}/{repo}/pulls/{prNumber}/files",owner,name,prNumber)
                .header("Authorization", "Bearer "+accessToken)
                .header("Accept","application/vnd.github.v3+json")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String,Object>>>() {
                })
                .block();
    }
    public void postInlineComment(
            String owner,
            String repo,
            Long prNumber,
            String commitSha,
            String path,
            Integer line,
            String body,
            String accessToken
    ) {
        Map<String, Object> requestBody = Map.of(
                "body", body,
                "commit_id", commitSha,
                "path", path,
                "line", line,
                "side", "RIGHT"
        );

        webClient.post()
                .uri("/repos/{owner}/{repo}/pulls/{prNumber}/comments",
                        owner, repo, prNumber)
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/vnd.github.v3+json")
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .toBodilessEntity()
                .block();
    }
    public void postPrComment(
            String owner,
            String repo,
            Long prNumber,
            String body,
            String accessToken
    ) {
        Map<String, Object> requestBody = Map.of(
                "body", body
        );

        webClient.post()
                .uri("/repos/{owner}/{repo}/issues/{prNumber}/comments",
                        owner, repo, prNumber)
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/vnd.github.v3+json")
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}
