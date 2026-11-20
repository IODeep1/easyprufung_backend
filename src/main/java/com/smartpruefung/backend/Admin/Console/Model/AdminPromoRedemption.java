package com.smartpruefung.backend.Admin.Console.Model;

import java.time.Instant;
import javax.persistence.*;
import lombok.Data;

/** Durable per-customer claim; retained when a code is disabled, deleted or recreated. */
@Entity
@Table(
    name = "admin_promo_redemptions",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_admin_promo_customer",
            columnNames = {"code", "user_uuid"}))
@Data
public class AdminPromoRedemption {
  @Id
  @Column(length = 36)
  private String id;

  @Column(nullable = false, length = 100)
  private String code;

  @Column(name = "user_uuid", nullable = false, length = 100)
  private String userUuid;

  @Column(nullable = false)
  private long promoId;

  @Column(nullable = false)
  private String plan;

  @Column(nullable = false)
  private Instant redeemedAt;
}
