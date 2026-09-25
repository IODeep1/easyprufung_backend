package com.easyprufung.backend.Project;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "contactforms")
@Data
public class ContactForm {

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
    private String name;

    @Column
    private String email;

    @Column(length = 2000)
    private String message;

    @Column(name = "submitted_date")
    private Timestamp submittedDate;
}