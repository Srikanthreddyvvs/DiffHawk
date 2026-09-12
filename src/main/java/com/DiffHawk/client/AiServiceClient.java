package com.DiffHawk.client;

import com.DiffHawk.dto.AiReviewRequestDto;
import com.DiffHawk.dto.AiReviewResponseDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class AiServiceClient {
    private final WebClient webClient;

    public AiServiceClient(@Qualifier("aiServiceWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public AiReviewResponseDto requestReview(AiReviewRequestDto request){
        return webClient.post()
                .uri("/api/review")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AiReviewResponseDto.class)
                .block();
    }
}
