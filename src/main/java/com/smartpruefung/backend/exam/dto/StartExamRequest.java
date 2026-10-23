package com.smartpruefung.backend.exam.dto;

import com.smartpruefung.backend.exam.domain.CefrLevel;
import com.smartpruefung.backend.exam.domain.ExamProvider;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data @NoArgsConstructor @AllArgsConstructor
public class StartExamRequest {
    @NotBlank private String userId;
    @NotNull private ExamProvider provider;
    @NotNull private CefrLevel level;
    private String examCode;
}
