package com.easyprufung.backend.User.DTO;

import com.easyprufung.backend.Project.Project;
import com.easyprufung.backend.User.Subscription;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

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
    private List<Subscription> subscriptions;
    private List<Project> projects;
    private Timestamp createdDate;
    private Timestamp updatedDate;
}
