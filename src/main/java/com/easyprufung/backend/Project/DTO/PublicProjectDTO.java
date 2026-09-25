package com.easyprufung.backend.Project.DTO;

import com.easyprufung.backend.Project.ContactForm;
import com.easyprufung.backend.Project.Waitlist;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

@Data
public class PublicProjectDTO {
    private long id;
    private String uuid;
    private String name;
    private String type;
    private String category;
    private String country;
    private Double latitude;
    private Double longitude;
    private String description;
    private String logoUrl;
    private String tempUrl;
    private Boolean isWebsiteUp;
    private String url;
    private Boolean isPublic;
    private Integer upvote;
    private Timestamp createdDate;
    private Timestamp updatedDate;
}
