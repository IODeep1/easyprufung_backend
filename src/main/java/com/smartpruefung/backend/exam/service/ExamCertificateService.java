package com.smartpruefung.backend.exam.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.smartpruefung.backend.User.Service.UserService;
import com.smartpruefung.backend.exam.domain.ExamResult;
import com.smartpruefung.backend.exam.dto.PublicExamCertificate;
import com.smartpruefung.backend.exam.dto.SectionResultView;
import com.smartpruefung.backend.exam.repository.ExamResultRepository;
import com.smartpruefung.backend.exam.repository.ExamSessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ExamCertificateService {
    private final ExamSessionRepository sessions;
    private final ExamResultRepository results;
    private final UserService users;
    private final JsonSupport json;

    public ExamCertificateService(ExamSessionRepository sessions, ExamResultRepository results,
                                  UserService users, JsonSupport json) {
        this.sessions = sessions;
        this.results = results;
        this.users = users;
        this.json = json;
    }

    @Transactional
    public String publish(UUID sessionId, String owner) {
        var session = sessions.lockById(sessionId).filter(s -> owner.equals(s.getUserId()))
                .orElseThrow(ExamCertificateService::unavailable);
        passedResult(sessionId);
        if (session.getCertificateToken() == null) {
            session.setCertificateToken(UUID.randomUUID().toString());
        }
        return session.getCertificateToken();
    }

    @Transactional
    public void revoke(UUID sessionId, String owner) {
        var session = sessions.lockById(sessionId).filter(s -> owner.equals(s.getUserId()))
                .orElseThrow(ExamCertificateService::unavailable);
        session.setCertificateToken(null);
    }

    @Transactional(readOnly = true)
    public PublicExamCertificate getPublic(UUID token) {
        var session = sessions.findByCertificateToken(token.toString())
                .orElseThrow(ExamCertificateService::unavailable);
        var result = passedResult(session.getId());
        var user = users.getUserByUUID(session.getUserId());
        if (user == null) throw unavailable();
        String name = Stream.of(user.getFirstname(), user.getLastname())
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(" "));
        List<SectionResultView> sections = json.read(result.getSectionResultsJson(), new TypeReference<>() {});
        return new PublicExamCertificate(name, session.getExamDefinition().getLevel().name(),
                result.getEvaluatedAt(), result.getScore(), result.getMaximumScore(), result.getPercentage(), sections);
    }

    private ExamResult passedResult(UUID sessionId) {
        return results.findByExamSessionId(sessionId).filter(ExamResult::isPassed)
                .orElseThrow(ExamCertificateService::unavailable);
    }

    private static ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}
