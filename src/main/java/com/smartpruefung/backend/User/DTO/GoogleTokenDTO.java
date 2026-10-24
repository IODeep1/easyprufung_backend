package com.smartpruefung.backend.User.DTO;

import lombok.Data;

@Data
public class GoogleTokenDTO {
    private String access_token;
    private String preferredLanguage;
}
