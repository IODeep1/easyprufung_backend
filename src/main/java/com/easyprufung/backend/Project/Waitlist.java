package com.easyprufung.backend.Project;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "waitlists")
@Data
public class Waitlist {

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
    private String email;

    @Column(name = "joined_date")
    private Timestamp joinedDate;
}
