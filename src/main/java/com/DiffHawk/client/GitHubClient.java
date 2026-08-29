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
}
