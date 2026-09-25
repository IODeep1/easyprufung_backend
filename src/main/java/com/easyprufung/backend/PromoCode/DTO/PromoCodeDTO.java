package com.easyprufung.backend.PromoCode.DTO;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class PromoCodeDTO {
    private long id;
    private Boolean isActive;
    private String code;
    private String status;
    private String plan;
    private String type;
    private Timestamp createdDate;
    private Timestamp updatedDate;
}