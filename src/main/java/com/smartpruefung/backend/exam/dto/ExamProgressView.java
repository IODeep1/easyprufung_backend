package com.smartpruefung.backend.exam.dto;

import lombok.Data;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.*;

@Data
public class ExamProgressView {
    @Min(0) private long revision;
    @Min(0) private int index;
    @NotNull @Valid private Map<String, DraftAnswer> answers = new LinkedHashMap<>();
    @Data
    public static class DraftAnswer {
        @NotNull private List<String> selectedOptionKeys;
        @NotNull private String text;
    }
}
