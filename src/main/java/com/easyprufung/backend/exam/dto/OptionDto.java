package com.easyprufung.backend.exam.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.validation.constraints.NotBlank;

@Data @NoArgsConstructor @AllArgsConstructor
public class OptionDto {
    @NotBlank private String key;
    @NotBlank private String text;
}
