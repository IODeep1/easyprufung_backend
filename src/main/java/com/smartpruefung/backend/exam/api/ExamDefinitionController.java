package com.smartpruefung.backend.exam.api;

import com.smartpruefung.backend.exam.dto.ExamSummaryView;
import com.smartpruefung.backend.exam.service.ExamDefinitionService;
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
