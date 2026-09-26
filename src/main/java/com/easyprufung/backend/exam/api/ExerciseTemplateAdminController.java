package com.easyprufung.backend.exam.api;

import com.easyprufung.backend.exam.dto.CreateTemplateRequest;
import com.easyprufung.backend.exam.service.ExerciseTemplateService;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/exam-definitions/{examCode}/parts/{partKey}/templates")
public class ExerciseTemplateAdminController {
    private final ExerciseTemplateService service;
    public ExerciseTemplateAdminController(ExerciseTemplateService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, UUID> create(@PathVariable String examCode, @PathVariable String partKey,
                                    @Valid @RequestBody CreateTemplateRequest request) {
        return Map.of("templateId", service.create(examCode, partKey, request));
    }
}
