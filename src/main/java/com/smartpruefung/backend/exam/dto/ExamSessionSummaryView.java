package com.smartpruefung.backend.exam.dto;

import com.smartpruefung.backend.exam.domain.CefrLevel;
import com.smartpruefung.backend.exam.domain.ExamProvider;
import com.smartpruefung.backend.exam.domain.SessionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamSessionSummaryView {
    private UUID sessionId;
    private String examCode;
    private String title;
    private ExamProvider provider;
    private CefrLevel level;
    private int definitionVersion;
    private SessionStatus status;
    private Instant createdAt;
    private Instant startedAt;
    private Instant expiresAt;
    private Instant submittedAt;
}