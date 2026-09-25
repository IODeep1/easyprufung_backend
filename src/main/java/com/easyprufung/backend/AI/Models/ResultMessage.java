package com.easyprufung.backend.AI.Models;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor

public class ResultMessage {
    private String role;
    private String content;
}
