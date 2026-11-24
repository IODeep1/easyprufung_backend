package com.smartpruefung.backend.partner;
import javax.persistence.*;
import java.time.Instant;
@Entity @Table(name="partner_stripe_codes",indexes={@Index(name="ix_partner_stripe_codes_partnerId",columnList="partnerId")})
public class PartnerCode extends PartnerRecord {
@Column(nullable=false,length=36) public String partnerId;
 @Column(nullable=false,unique=true) public String promotionCodeId;
 public String couponId,displayCode,retirementSource;
 public int discountBps=1000;
 public boolean active=true,liveMode;
 public Instant validFrom=Instant.now(),validTo;
}
