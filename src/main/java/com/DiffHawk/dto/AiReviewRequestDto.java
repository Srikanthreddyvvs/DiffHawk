package com.DiffHawk.dto;

import java.util.List;
import java.util.Map;

public record AiReviewRequestDto(
        Long reviewId,
        String owner,
        String repo,
        Long prNumber,
        List<Map<String, Object>> files
) {
}
