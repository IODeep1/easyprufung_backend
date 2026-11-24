package com.smartpruefung.backend.partner;
import javax.persistence.*;
import java.time.Instant;
@Entity @Table(name="partner_profiles",indexes={@Index(name="ix_partner_profiles_userId",columnList="userId")})
public class PartnerProfile extends PartnerRecord {
@Column(nullable=false,unique=true,length=36) public String userId;
 public String displayName,contactEmail,country;
 @Column(length=500) public String website;
 @Lob @Column(length=10000) public String socialsJson;
 @Lob @Column(length=2000) public String approach;
 public String status="PENDING";
 public int commissionBps=2000;
 public int discountBps=1000;
 @Column(length=1000) public String reviewReason="";
 public String reviewedBy,termsVersion;
 public Instant termsAcceptedAt,approvedAt,suspendedAt;
 public String payoutMethod,payoutMasked;
 @com.fasterxml.jackson.annotation.JsonIgnore @Lob @Column(length=6000) public String payoutEncrypted;
 public boolean payoutVerified=false;
}
