package com.easyprufung.backend.exam.api;

import com.easyprufung.backend.exam.dto.ExamSummaryView;
import com.easyprufung.backend.exam.service.ExamDefinitionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exams/definitions")
public class ExamDefinitionController {
    private final ExamDefinitionService service;
    public ExamDefinitionController(ExamDefinitionService service) { this.service = service; }

    @GetMapping
    public List<ExamSummaryView> list() { return service.listActive(); }
}
