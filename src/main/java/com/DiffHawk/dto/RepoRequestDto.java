package com.DiffHawk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RepoRequestDto (
        @NotBlank String owner,
        @NotBlank String name,
        @NotNull Long githubRepoId
){}
