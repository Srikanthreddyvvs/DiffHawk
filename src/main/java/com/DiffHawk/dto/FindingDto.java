package com.DiffHawk.dto;

public record FindingDto(
        String file,
        Integer line,
        String severity,
        String category,
        String message,
        String suggestion
) {}
