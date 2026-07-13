package com.jorge.notesai.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public class NoteDtos {

    public record NoteRequest(
            @NotBlank String title,
            @NotBlank String content
    ) {}

    public record NoteResponse(
            Long id,
            String title,
            String content,
            String aiSummary,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}
}
