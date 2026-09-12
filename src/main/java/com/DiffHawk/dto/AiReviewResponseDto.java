package com.DiffHawk.dto;

import java.util.List;

public record AiReviewResponseDto(
        Long reviewId,
        List<FindingDto> findings,
        Integer overallScore,
        Integer totalFindings
) {
}
