package com.easyprufung.backend.exam.api;

import com.easyprufung.backend.exam.dto.*;
import com.easyprufung.backend.exam.service.*;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/exams/sessions")
public class ExamSessionController {
    private final ExamGenerationService generation;
    private final ExamSubmissionService submission;
    private final ExamQueryService queries;

    public ExamSessionController(ExamGenerationService generation, ExamSubmissionService submission,
                                 ExamQueryService queries) {
        this.generation = generation; this.submission = submission; this.queries = queries;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExamSessionView start(@Valid @RequestBody StartExamRequest request) {
        return generation.start(request);
    }

    @GetMapping("/{sessionId}")
    public ExamSessionView get(@PathVariable UUID sessionId) { return queries.getSession(sessionId); }

    @PostMapping("/{sessionId}/submit")
    public ExamResultView submit(@PathVariable UUID sessionId, @Valid @RequestBody SubmitExamRequest request) {
        return submission.submit(sessionId, request);
    }

    @GetMapping("/{sessionId}/result")
    public ExamResultView result(@PathVariable UUID sessionId) { return queries.getResult(sessionId); }
}
