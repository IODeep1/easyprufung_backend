package com.smartpruefung.backend.User.DTO;

import com.smartpruefung.backend.User.Subscription;
import lombok.Data;

import java.sql.Timestamp;

@Data
public class UserDTO {
    private long id;
    private String uuid;
    private String token;
    private String firstname;
    private String lastname;
    private String email;
    private String password;
    private String codingKnowledgeLevel;
    private String resetPasswordToken;
    private Timestamp resetPasswordTokenCreationDate;
    private String source;
    private String preferredLanguage;
    private String acquisitionSource;
    private boolean acquisitionOnboardingCompleted;
    private Subscription subscription;
    private Timestamp createdDate;
    private Timestamp updatedDate;
}
