package com.smartpruefung.backend.partner;
import javax.persistence.*;
import java.time.Instant;
@Entity @Table(name="partner_commission_entries",indexes={@Index(name="ix_partner_commission_entries_partnerId",columnList="partnerId"),@Index(name="ix_partner_commission_entries_saleId",columnList="saleId"),@Index(name="ix_partner_commission_entries_payoutId",columnList="payoutId"),@Index(name="ix_partner_commission_entries_eligibleAt",columnList="eligibleAt")})
public class PartnerEntry extends PartnerRecord {
@Column(nullable=false,length=36) public String partnerId;
 @Column(nullable=false,length=36) public String saleId;
 @Column(nullable=false,unique=true) public String sourceKey;
 public String kind;
 public long amountCents;
 public Instant eligibleAt;
 public String payoutId;
 public String status="OPEN";
}
