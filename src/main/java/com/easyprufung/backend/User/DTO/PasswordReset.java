package com.easyprufung.backend.User.DTO;


import lombok.Data;

@Data
public class PasswordReset {
    private String token;
    private String password;
}
