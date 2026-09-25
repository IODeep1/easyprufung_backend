package com.easyprufung.backend.Project.DTO;
import java.sql.Timestamp;
import lombok.Data;

@Data
public class WaitlistDTO {
    private long id;
    private String email;
    private String projectId;
    private Timestamp joinedDate;
}
