package com.easyprufung.backend.exam.api;

import com.easyprufung.backend.exam.dto.*;
import com.easyprufung.backend.exam.service.*;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import javax.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import java.util.UUID;

@RestController
@RequestMapping("/api/exams/sessions")
@Validated
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

    @GetMapping
    public Page<ExamSessionSummaryView> getUserSessions(
            @RequestParam @NotBlank String userId,
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return queries.getUserSessions(userId, pageable);
    }

    @GetMapping("/{sessionId}/result")
    public ExamResultView result(@PathVariable UUID sessionId) { return queries.getResult(sessionId); }
}
