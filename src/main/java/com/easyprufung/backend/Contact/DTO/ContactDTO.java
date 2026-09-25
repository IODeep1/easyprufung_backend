package com.easyprufung.backend.Contact.DTO;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class ContactDTO {

    private long id;
    private String firstname;
    private String lastname;
    private String email;
    private String subject;
    private String message;
    private String budget;
    private String type;
    private Timestamp createdDate;
    private Timestamp updatedDate;
}
