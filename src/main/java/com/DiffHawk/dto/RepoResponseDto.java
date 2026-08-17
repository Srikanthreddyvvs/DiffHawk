package com.DiffHawk.dto;

import java.time.Instant;

public record RepoResponseDto (
        Long id,
        Long githubRepoId,
        String owner,
        String name,
        String webhookUrl,
        String webhookSecret,
        Instant createdAt
){}
