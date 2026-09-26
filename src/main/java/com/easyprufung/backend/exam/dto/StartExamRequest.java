package com.easyprufung.backend.exam.dto;

import com.easyprufung.backend.exam.domain.CefrLevel;
import com.easyprufung.backend.exam.domain.ExamProvider;
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
