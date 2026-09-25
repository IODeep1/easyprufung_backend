package com.easyprufung.backend.PromoCode;


import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "promo_codes")
@Data
public class PromoCode {
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

    @Column(name = "is_active")
    private Boolean isActive;

    @Column
    private String code;

    @Column
    private String status;

    @Column
    private String plan;

    @Column
    private String type;


    @Column(name = "created_date")
    private Timestamp createdDate;

    @Column(name = "updated_date")
    private Timestamp updatedDate;
}
