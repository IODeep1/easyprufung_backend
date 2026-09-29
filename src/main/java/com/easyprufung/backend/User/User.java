package com.easyprufung.backend.User;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "users")
@Data
@JsonIgnoreProperties(value={ "password" , "projects"}, allowSetters = true)
public class User {

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
    private String firstname;

    @Column
    private String lastname;

    @Column
    private String email;

    @Column
    private String password;

    @Column(name = "reset_password_token")
    private String resetPasswordToken;

    @Column(name = "reset_password_token_creation_date")
    private Timestamp resetPasswordTokenCreationDate;

    @Column
    private String source;

    @Column(name = "created_date")
    private Timestamp createdDate;

    @Column(name = "updated_date")
    private Timestamp updatedDate;

    /*
     * A user now has exactly one access/subscription record.
     * Keeping the existing join table avoids introducing a new subscription_id
     * column on users. Existing databases must be cleaned so each user has at
     * most one row in user_subscriptions before enabling this mapping.
     */
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinTable(
            name = "user_subscriptions",
            joinColumns = @JoinColumn(name = "user_id", unique = true),
            inverseJoinColumns = @JoinColumn(name = "subscription_id", unique = true)
    )
    private Subscription subscription;
}
