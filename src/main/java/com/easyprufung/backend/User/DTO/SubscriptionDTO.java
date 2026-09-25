package com.easyprufung.backend.User.DTO;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class SubscriptionDTO {
    private long id;
    private String uuid;
    private String customerId;
    private String customerEmail;
    private Boolean isActive;
    private String status;
    private String priceId;
    private String plan;
    private String type;
    private int  iteration;
    private int  quota;
    private Timestamp startDate;
    private Timestamp endDate;
    private Timestamp createdDate;
    private Timestamp updatedDate;
}
