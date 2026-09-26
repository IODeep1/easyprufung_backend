package com.easyprufung.backend.User;


import com.easyprufung.backend.Project.Project;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

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

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinTable(name = "user_subscriptions", joinColumns = @JoinColumn(name = "user_id"),inverseJoinColumns = @JoinColumn(name = "subscription_id"))
    private Set<Subscription> subscriptions = new HashSet<>();

    public void addSubscription(Subscription subscription){
        this.subscriptions.add(subscription);
    }


    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinTable(name = "user_projects", joinColumns = @JoinColumn(name = "user_id"),inverseJoinColumns = @JoinColumn(name = "project_id"))
    private Set<Project> projects = new HashSet<>();

    public void addProject(Project project){
        this.projects.add(project);
    }
}
