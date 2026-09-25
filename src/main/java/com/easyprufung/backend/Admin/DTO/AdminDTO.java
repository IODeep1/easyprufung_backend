package com.easyprufung.backend.Admin.DTO;

import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

@Data
public class AdminDTO {
    private long id;
    private String firstname;
    private String lastname;
    private String email;
    private String password;
    private String token;
    private Timestamp createdDate;
    private Timestamp updatedDate;
    private List<RoleDTO> roles;
}
