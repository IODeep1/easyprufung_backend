package com.smartpruefung.backend.exam.api;

import com.smartpruefung.backend.Utils.JwtUtils;
import com.smartpruefung.backend.exam.dto.*;
import com.smartpruefung.backend.exam.service.*;
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
    @org.springframework.beans.factory.annotation.Autowired
    private com.smartpruefung.backend.User.Service.UserService users;
    @org.springframework.beans.factory.annotation.Autowired
    private ExamProgressService progress;
    private String owner(String authorization) {
        String email = JwtUtils.getHttpClientConsumer(authorization).email;
        if (email == null) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var user = users.getUserByEmail(email);
        if (user == null) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return user.getUuid();
    }
    private final ExamGenerationService generation;
    private final ExamCertificateService certificates;
    private final ExamSubmissionService submission;
    private final ExamQueryService queries;

    public ExamSessionController(ExamGenerationService generation, ExamSubmissionService submission,
                                 ExamQueryService queries, ExamCertificateService certificates) {
        this.certificates = certificates;
        this.generation = generation; this.submission = submission; this.queries = queries;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExamSessionView start(@Valid @RequestBody StartExamRequest request, @RequestHeader("Authorization") String authorization) {
        request.setUserId(owner(authorization));
        return generation.start(request);
    }

    @GetMapping("/{sessionId}")
    public ExamSessionView get(@PathVariable UUID sessionId, @RequestHeader("Authorization") String authorization) {
        progress.authorize(sessionId, owner(authorization));
        return queries.getSession(sessionId);
    }
    @GetMapping("/active")
    public org.springframework.http.ResponseEntity<ExamSessionView> active(
            @RequestHeader("Authorization") String authorization) {
        ExamSessionView active = progress.active(owner(authorization));
        return active == null ? org.springframework.http.ResponseEntity.noContent().build()
                : org.springframework.http.ResponseEntity.ok(active);
    }
    @GetMapping("/{sessionId}/progress")
    public ExamProgressView progress(@PathVariable UUID sessionId, @RequestHeader("Authorization") String authorization) {
        return progress.get(sessionId, owner(authorization));
    }
    @PutMapping("/{sessionId}/progress")
    public ExamProgressView save(@PathVariable UUID sessionId, @RequestHeader("Authorization") String authorization,
                                 @Valid @RequestBody ExamProgressView draft) {
        return progress.save(sessionId, owner(authorization), draft);
    }

    @PostMapping("/{sessionId}/certificate")
    public java.util.Map<String, String> publishCertificate(@PathVariable UUID sessionId,
            @RequestHeader("Authorization") String authorization) {
        return java.util.Map.of("token", certificates.publish(sessionId, owner(authorization)));
    }

    @DeleteMapping("/{sessionId}/certificate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeCertificate(@PathVariable UUID sessionId,
            @RequestHeader("Authorization") String authorization) {
        certificates.revoke(sessionId, owner(authorization));
    }

    @PostMapping("/{sessionId}/submit")
    public ExamResultView submit(@PathVariable UUID sessionId, @Valid @RequestBody SubmitExamRequest request, @RequestHeader("Authorization") String authorizationHeader) {
        progress.authorize(sessionId, owner(authorizationHeader));
        var httpClientConsumer = JwtUtils.getHttpClientConsumer(authorizationHeader);
        return submission.submit(httpClientConsumer.email, sessionId, request);
    }

    @GetMapping
    public Page<ExamSessionSummaryView> getUserSessions(
            @RequestParam @NotBlank String userId,
            @RequestHeader("Authorization") String authorization,
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return queries.getUserSessions(owner(authorization), pageable);
    }

    @GetMapping("/{sessionId}/result")
    public ExamResultView result(@PathVariable UUID sessionId, @RequestHeader("Authorization") String authorization) {
        progress.authorize(sessionId, owner(authorization));
        return queries.getResult(sessionId);
    }
}
