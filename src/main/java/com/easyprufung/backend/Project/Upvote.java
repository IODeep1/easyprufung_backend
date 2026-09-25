package com.easyprufung.backend.Project;

import com.easyprufung.backend.User.User;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "upvotes", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "project_id"}))
@Data
public class Upvote {

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

    @ManyToOne
    private User user;

    @ManyToOne
    private Project project;

    @Column(name = "created_date")
    private Timestamp createdDate;
}
