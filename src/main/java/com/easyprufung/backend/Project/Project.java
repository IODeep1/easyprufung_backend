package com.easyprufung.backend.Project;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "projects")
@Data
public class Project {

    @Id
    @GeneratedValue(
            strategy= GenerationType.AUTO,
            generator="native"
    )
    @GenericGenerator(
            name = "native",
            strategy = "native"
    )
    private long id;

    @Column
    private String uuid;

    @Column
    private String name;

    @Column
    private String source;

    @Column
    private String type;

    @Column
    private String category;

    @Column
    private String country;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(length = 5000)
    private String prompt;

    @Column(length = 4000)
    private String description;

    @Column(length = 500, name = "short_description")
    private String shortDescription;

    @Column(length = 1000, name = "icon_onfiguration")
    private String iconConfiguration;


    @Column(length = 2000, name = "landingpage_description")
    private String landingPageDescription;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "temp_url")
    private String tempUrl;

    @Column(name = "build_error")
    private String buildError;

    @Column(name = "is_website_up", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isWebsiteUp;

    @Column(name = "is_building", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean IsBuilding;

    @Column
    private String url;

    @Column(name = "dsn_verification_token")
    private String dnsVerificationToken;

    @Column
    private String path;

    @Column(name = "is_public", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean IsPublic;

    @Column(name = "is_approved", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean IsApproved;

    @Column
    private Integer upvote;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinTable(name = "project_contactforms", joinColumns = @JoinColumn(name = "project_id"),inverseJoinColumns = @JoinColumn(name = "contactform_id"))
    private Set<ContactForm> contactForms = new HashSet<>();

    public void addContactForm(ContactForm contactForm){
        this.contactForms.add(contactForm);
    }

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinTable(name = "project_waitlists", joinColumns = @JoinColumn(name = "project_id"),inverseJoinColumns = @JoinColumn(name = "waitlist_id"))
    private Set<Waitlist> waitLists = new HashSet<>();

    public void addWaitList(Waitlist waitList){
        this.waitLists.add(waitList);
    }

    @Column(name = "created_date")
    private Timestamp createdDate;

    @Column(name = "updated_date")
    private Timestamp updatedDate;
}