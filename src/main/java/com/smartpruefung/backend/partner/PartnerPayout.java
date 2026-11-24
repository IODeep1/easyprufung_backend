package com.smartpruefung.backend.partner;
import javax.persistence.*;
import java.time.Instant;
@Entity @Table(name="partner_payouts",indexes={@Index(name="ix_partner_payouts_partnerId",columnList="partnerId"),@Index(name="ix_partner_payouts_status",columnList="status")})
public class PartnerPayout extends PartnerRecord {
public String partnerId;
 @Column(nullable=false,unique=true) public String requestKey;
 public String currency="EUR",method,maskedDestination;
 @com.fasterxml.jackson.annotation.JsonIgnore @Lob @Column(length=6000) public String destinationEncrypted;
 public long amountCents;
 public String status="REQUESTED",externalReference,adminActor;
 @Column(length=1000) public String reason;
 public Instant paidAt;
}
