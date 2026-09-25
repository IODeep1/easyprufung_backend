package com.easyprufung.backend.User;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "subscriptions")
@Data
public class Subscription {

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

    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column
    private String plan;

    @Column(name = "price_id")
    private String priceId;

    @Column
    private String status;

    @Column
    private String type;

    @Column
    private int quota;

    @Column
    private int iteration;

    @Column(name = "start_date")
    private Timestamp startDate;

    @Column(name = "end_date")
    private Timestamp endDate;

    @Column(name = "created_date")
    private Timestamp createdDate;

    @Column(name = "updated_date")
    private Timestamp updatedDate;
}