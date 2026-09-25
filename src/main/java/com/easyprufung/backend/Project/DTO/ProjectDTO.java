package com.easyprufung.backend.Project.DTO;

import com.easyprufung.backend.Project.ContactForm;
import com.easyprufung.backend.Project.Models.ChatMessage;
import com.easyprufung.backend.Project.Project;
import com.easyprufung.backend.Project.Waitlist;
import lombok.Data;


import java.sql.Timestamp;
import java.util.List;

@Data
public class ProjectDTO {
    private long id;
    private String uuid;
    private String name;
    private String source;
    private String type;
    private String category;
    private String country;
    private Double latitude;
    private Double longitude;
    private String prompt;
    private String description;
    private Object validationData;
    private Object nameData;
    private String iconData;
    private String svgIcon;
    private String shortDescription;
    private String landingPageDescription;
    private String iconConfiguration;
    private String indexHtmlContent;
    private String logoUrl;
    private String tempUrl;
    private String buildError;
    private Boolean isBuilding;
    private Boolean isWebsiteUp;
    private String url;
    private String dnsVerificationToken;
    private String path;
    private Boolean isApproved;
    private Boolean isPublic;
    private Integer upvote;
    private List<Waitlist> waitlists;
    private List<ContactForm> contactForms;
    private Timestamp createdDate;
    private Timestamp updatedDate;
}
