package com.smartpruefung.backend.exam.api;

import com.smartpruefung.backend.exam.dto.PublicExamCertificate;
import com.smartpruefung.backend.exam.service.ExamCertificateService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/exams/certificates")
public class PublicExamCertificateController {
    private final ExamCertificateService certificates;

    public PublicExamCertificateController(ExamCertificateService certificates) {
        this.certificates = certificates;
    }

    @GetMapping("/{token}")
    public ResponseEntity<PublicExamCertificate> get(@PathVariable("token") UUID token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(certificates.getPublic(token));
    }
}
