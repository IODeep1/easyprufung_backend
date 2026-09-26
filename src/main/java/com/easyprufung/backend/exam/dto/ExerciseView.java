package com.easyprufung.backend.exam.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor
public class ExerciseView {
    private UUID id;
    private String sectionKey;
    private String sectionTitle;
    private String partKey;
    private String partTitle;
    private String instructions;
    private String content;
    private String audioUrl;
    private String audioContentType;
    private Integer audioPlayLimit;
    private Integer durationSeconds;
    private List<QuestionView> questions;
}
