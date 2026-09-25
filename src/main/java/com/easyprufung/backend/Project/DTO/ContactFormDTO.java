package com.easyprufung.backend.Project.DTO;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class ContactFormDTO {
    private long id;
    private String projectId;
    private String name;
    private String email;
    private String message;
    private Timestamp submittedDate;
}
